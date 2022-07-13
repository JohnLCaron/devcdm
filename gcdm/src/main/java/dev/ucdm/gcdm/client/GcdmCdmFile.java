/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.ucdm.gcdm.client;

import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import dev.ucdm.gcdm.protogen.GcdmProto;
import dev.ucdm.gcdm.protogen.GcdmServerProto.CdmDataRequest;
import dev.ucdm.gcdm.protogen.GcdmServerProto.CdmDataResponse;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import dev.ucdm.array.Arrays;
import dev.ucdm.array.StructureDataArray;
import dev.ucdm.gcdm.protogen.GcdmGrpc;
import dev.ucdm.gcdm.protogen.GcdmServerProto.CdmRequest;
import dev.ucdm.gcdm.protogen.GcdmServerProto.CdmResponse;
import dev.ucdm.gcdm.GcdmConverter;
import dev.ucdm.core.api.*;
import org.jetbrains.annotations.Nullable;

/** A remote CdmFile, using gprc protocol to communicate. */
public class GcdmCdmFile extends CdmFile {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GcdmCdmFile.class);
  private static final int MAX_DATA_WAIT_SECONDS = 30;
  private static final int MAX_MESSAGE = 101 * 1000 * 1000; // 101 Mb LOOK where is this set ??
  private static boolean showRequest = true;

  public static final String PROTOCOL = "gcdm";
  public static final String SCHEME = PROTOCOL + ":";

  @Override
  protected Iterator<dev.ucdm.array.StructureData> getSequenceIterator(Sequence s, int bufferSize) throws IOException {
    dev.ucdm.array.Array<?> data = readArrayData(s, s.getSection());
    Preconditions.checkNotNull(data);
    Preconditions.checkArgument(data instanceof StructureDataArray);
    StructureDataArray sdata = (StructureDataArray) data;
    return sdata.iterator();
  }

  @Nullable
  protected dev.ucdm.array.Array<?> readArrayData(Variable v, dev.ucdm.array.Section sectionWanted) throws IOException {
    String spec = ParsedArraySectionSpec.makeSectionSpecString(v, sectionWanted);
    if (showRequest) {
      long expected = sectionWanted.computeSize() * v.getElementSize();
      System.out.printf("GcdmCdmFile data request forspec=(%s)%n url='%s'%n path='%s' request bytes = %d%n", spec,
          this.remoteURI, this.path, expected);
    }
    final Stopwatch stopwatch = Stopwatch.createStarted();

    List<dev.ucdm.array.Array<?>> results = new ArrayList<>();
    long size = 0;
    CdmDataRequest request = CdmDataRequest.newBuilder().setLocation(this.path).setVariableSpec(spec).build();
    try {
      Iterator<CdmDataResponse> responses =
          blockingStub.withDeadlineAfter(MAX_DATA_WAIT_SECONDS, TimeUnit.SECONDS).getCdmData(request);
      while (responses.hasNext()) {
        CdmDataResponse response = responses.next();
        if (response.hasError()) {
          throw new IOException(response.getError().getMessage());
        }
        // Section sectionReturned = GcdmConverter.decodeSection(response.getSection());
        dev.ucdm.array.Array<?> result = GcdmConverter.decodeData(response.getData());
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
    this.path = builder.dataPath;
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
    private String dataPath;
    private boolean built;

    protected abstract T self();

    // serverUrl/dataPath
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
      this.dataPath = uri.getPath();
      if (this.dataPath.startsWith("/")) {
        this.dataPath = this.dataPath.substring(1);
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
        readCdmFile(dataPath);

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

    private void readCdmFile(String location) {
      log.info("GcdmCdmFile request header for " + location);
      CdmRequest request = CdmRequest.newBuilder().setLocation(location).build();
      CdmResponse response = blockingStub.getCdmFile(request);
      if (response.hasError()) {
        throw new RuntimeException(response.getError().getMessage());
      } else {
        GcdmProto.CdmFile header = response.getCdmFile();
        setId(header.getId());
        setTitle(header.getTitle());
        setLocation(remoteURI);

        this.rootGroup = Group.builder().setName("");
        GcdmConverter.decodeGroup(header.getRoot(), this.rootGroup);
      }
    }

  }

}
