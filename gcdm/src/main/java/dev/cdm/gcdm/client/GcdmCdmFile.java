/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.cdm.gcdm.client;

import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import dev.cdm.array.Arrays;
import dev.cdm.array.StructureDataArray;
import dev.cdm.gcdm.protogen.GcdmGrpc;
import dev.cdm.gcdm.protogen.GcdmServerProto.DataRequest;
import dev.cdm.gcdm.protogen.GcdmServerProto.DataResponse;
import dev.cdm.gcdm.protogen.GcdmProto.Header;
import dev.cdm.gcdm.protogen.GcdmServerProto.HeaderRequest;
import dev.cdm.gcdm.protogen.GcdmServerProto.HeaderResponse;
import dev.cdm.gcdm.GcdmConverter;
import dev.cdm.core.api.*;

/** A remote CdmFile, using gprc protocol to communicate. */
public class GcdmCdmFile extends CdmFile {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GcdmCdmFile.class);
  private static final int MAX_DATA_WAIT_SECONDS = 30;
  private static final int MAX_MESSAGE = 101 * 1000 * 1000; // 101 Mb
  private static boolean showRequest = true;

  public static final String PROTOCOL = "gcdm";
  public static final String SCHEME = PROTOCOL + ":";

  @Override
  protected Iterator<dev.cdm.array.StructureData> getSequenceIterator(Sequence s, int bufferSize) throws IOException {
    dev.cdm.array.Array<?> data = readArrayData(s, s.getSection());
    Preconditions.checkNotNull(data);
    Preconditions.checkArgument(data instanceof StructureDataArray);
    StructureDataArray sdata = (StructureDataArray) data;
    return sdata.iterator();
  }

  @Nullable
  protected dev.cdm.array.Array<?> readArrayData(Variable v, dev.cdm.array.Section sectionWanted) throws IOException {
    String spec = ParsedArraySectionSpec.makeSectionSpecString(v, sectionWanted);
    if (showRequest) {
      long expected = sectionWanted.computeSize() * v.getElementSize();
      System.out.printf("GcdmCdmFile data request forspec=(%s)%n url='%s'%n path='%s' request bytes = %d%n", spec,
          this.remoteURI, this.path, expected);
    }
    final Stopwatch stopwatch = Stopwatch.createStarted();

    List<dev.cdm.array.Array<?>> results = new ArrayList<>();
    long size = 0;
    DataRequest request = DataRequest.newBuilder().setLocation(this.path).setVariableSpec(spec).build();
    try {
      Iterator<DataResponse> responses =
          blockingStub.withDeadlineAfter(MAX_DATA_WAIT_SECONDS, TimeUnit.SECONDS).getCdmData(request);
      while (responses.hasNext()) {
        DataResponse response = responses.next();
        if (response.hasError()) {
          throw new IOException(response.getError().getMessage());
        }
        // Section sectionReturned = GcdmConverter.decodeSection(response.getSection());
        dev.cdm.array.Array<?> result = GcdmConverter.decodeData(response.getData());
        results.add(result);
        size += result.length() * v.getElementSize();
        if (showRequest) {
          long recieved = result.length() * v.getElementSize();
          System.out.printf("  readArrayData bytes recieved = %d %n", recieved);
        }
      }

    } catch (StatusRuntimeException e) {
      log.warn("readSection requestData failed failed: ", e);
      throw new IOException(e);

    } catch (Throwable t) {
      System.out.printf(" ** failed after %s%n", stopwatch);
      log.warn("readSection requestData failed failed: ", t);
      throw new IOException(t);
    }
    if (showRequest) {
      double rate = ((double) size) / stopwatch.elapsed(TimeUnit.MICROSECONDS);
      System.out.printf(" ** recieved=%d took=%s rate=%.2f MB/sec%n", size, stopwatch.stop(), rate);
    }

    if (results.size() == 1) {
      return results.get(0);
    } else {
      return Arrays.combine(v.getArrayType(), sectionWanted.getShape(), (List) results); // TODO generics
    }
  }

  @Override
  public String getCdmFileTypeId() {
    return PROTOCOL;
  }

  @Override
  public String getCdmFileTypeDescription() {
    return PROTOCOL;
  }

  @Override
  public synchronized void close() {
    try {
      channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
    } catch (InterruptedException interruptedException) {
      log.warn("GcdmCdmFile shutdown interrupted");
      // fall through
    }
  }

  ////////////////////////////////////////////////////////////////////////////////////////////

  private final String remoteURI;
  private final String path;
  private final ManagedChannel channel;
  private final GcdmGrpc.GcdmBlockingStub blockingStub;

  private GcdmCdmFile(Builder<?> builder) {
    super(builder);
    this.remoteURI = builder.remoteURI;
    this.path = builder.path;
    this.channel = builder.channel;
    this.blockingStub = builder.blockingStub;
  }

  public Builder<?> toBuilder() {
    return addLocalFieldsToBuilder(builder());
  }

  private Builder<?> addLocalFieldsToBuilder(Builder<? extends Builder<?>> b) {
    b.setRemoteURI(this.remoteURI);
    return (Builder<?>) super.addLocalFieldsToBuilder(b);
  }

  public static Builder<?> builder() {
    return new Builder2();
  }

  private static class Builder2 extends Builder<Builder2> {
    @Override
    protected Builder2 self() {
      return this;
    }
  }

  public static abstract class Builder<T extends Builder<T>> extends CdmFile.Builder<T> {
    private String remoteURI;
    private ManagedChannel channel;
    private GcdmGrpc.GcdmBlockingStub blockingStub;
    private String path;
    private boolean built;

    protected abstract T self();

    public T setRemoteURI(String remoteURI) {
      this.remoteURI = remoteURI;
      return self();
    }

    public GcdmCdmFile build() {
      if (built)
        throw new IllegalStateException("already built");
      built = true;
      openChannel();
      return new GcdmCdmFile(this);
    }

    private void openChannel() {
      // parse the URI
      URI uri = java.net.URI.create(this.remoteURI);
      String target = uri.getAuthority();
      this.path = uri.getPath();
      if (this.path.startsWith("/")) {
        this.path = this.path.substring(1);
      }

      // Create a communication channel to the server, known as a Channel. Channels are thread-safe
      // and reusable. It is common to create channels at the beginning of your application and reuse
      // them until the application shuts down.
      this.channel = ManagedChannelBuilder.forTarget(target)
          // Channels are secure by default (via SSL/TLS). For now, we disable TLS to avoid needing certificates.
          .usePlaintext() //
          .enableFullStreamDecompression() //
          .maxInboundMessageSize(MAX_MESSAGE) //
          .build();
      try {
        this.blockingStub = GcdmGrpc.newBlockingStub(channel);
        readHeader(path);

      } catch (Exception e) {
        // ManagedChannels use resources like threads and TCP connections. To prevent leaking these
        // resources the channel should be shut down when it will no longer be used. If it may be used
        // again leave it running.
        try {
          channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException interruptedException) {
          log.warn("Shutdown interrupted ", e);
          // fall through
        }
        e.printStackTrace();
        throw new RuntimeException("Cant open Gcdm url " + this.remoteURI, e);
      }
    }

    private void readHeader(String location) {
      log.info("GcdmCdmFile request header for " + location);
      HeaderRequest request = HeaderRequest.newBuilder().setLocation(location).build();
      HeaderResponse response = blockingStub.getNetcdfHeader(request);
      if (response.hasError()) {
        throw new RuntimeException(response.getError().getMessage());
      } else {
        Header header = response.getHeader();
        setId(header.getId());
        setTitle(header.getTitle());
        setLocation(remoteURI);

        this.rootGroup = Group.builder().setName("");
        GcdmConverter.decodeGroup(header.getRoot(), this.rootGroup);
      }
    }

  }

}
