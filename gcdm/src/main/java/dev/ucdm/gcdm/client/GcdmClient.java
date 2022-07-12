/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package dev.ucdm.gcdm.client;

import com.google.common.base.Stopwatch;
import dev.ucdm.gcdm.protogen.GcdmProto;
import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import dev.ucdm.array.Array;
import dev.ucdm.array.Arrays;
import dev.ucdm.array.ArrayType;
import dev.ucdm.array.Section;
import dev.ucdm.gcdm.protogen.GcdmGrpc;
import dev.ucdm.gcdm.protogen.GcdmServerProto.DataRequest;
import dev.ucdm.gcdm.protogen.GcdmServerProto.DataResponse;
import dev.ucdm.gcdm.protogen.GcdmProto.Header;
import dev.ucdm.gcdm.protogen.GcdmServerProto.HeaderRequest;
import dev.ucdm.gcdm.protogen.GcdmServerProto.HeaderResponse;
import dev.ucdm.gcdm.GcdmConverter;

/** A simple client that makes a Netcdf request from GcdmServer. Used for testing. */
public class GcdmClient {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(GcdmClient.class);
  private static final int MAX_MESSAGE = 99 * 1000 * 1000;

  private final GcdmGrpc.GcdmBlockingStub blockingStub;
  private static final String cdmUnitTestDir = "D:/testData/thredds-test-data/local/thredds-test-data/cdmUnitTest/";
  private static final String localFilename =
      cdmUnitTestDir + "formats/netcdf4/e562p1_fp.inst3_3d_asm_Nv.20100907_00z+20100909_1200z.nc4";

  /** Construct client for accessing HelloWorld server using the existing channel. */
  public GcdmClient(Channel channel) {
    blockingStub = GcdmGrpc.newBlockingStub(channel);
  }

  private Header getHeader(String location) {
    System.out.printf("Header request %s%n", location);
    HeaderRequest request = HeaderRequest.newBuilder().setLocation(location).build();
    HeaderResponse response;
    try {
      response = blockingStub.getNetcdfHeader(request);
      // System.out.printf("Header response %s%n", response);
      return response.getHeader();
    } catch (StatusRuntimeException e) {
      logger.warn("getHeader failed: " + location, e);
      e.printStackTrace();
    }
    return null;
  }

  private Array<?> getData(String location, GcdmProto.Variable v) {
    ArrayType dataType = GcdmConverter.convertArrayType(v.getArrayType());
    Section section = GcdmConverter.decodeSection(v);
    System.out.printf("Data request %s %s (%s)%n", v.getArrayType(), v.getShortName(), section);
    if (dataType != ArrayType.DOUBLE && dataType != ArrayType.FLOAT) {
      System.out.printf("***skip%n");
      return null;
    }
    DataRequest request = DataRequest.newBuilder().setLocation(location).setVariableSpec(v.getShortName()).build();
    Iterator<DataResponse> responses;
    try {
      responses = blockingStub.withDeadlineAfter(30, TimeUnit.SECONDS).getCdmData(request);
      List<Array<?>> results = new ArrayList<>();
      while (responses.hasNext()) {
        DataResponse response = responses.next();
        results.add(GcdmConverter.decodeData(response.getData()));
      }
      return Arrays.combine(dataType, section.getShape(), results);
    } catch (Throwable e) {
      logger.warn("getData failed: " + location, e);
      e.printStackTrace();
      return null;
    }
  }

  /**
   * Greet server. If provided, the first element of {@code args} is the name to use in the
   * greeting. The second argument is the target server.
   */
  public static void main(String[] args) throws Exception {
    String location = localFilename;
    String target = "localhost:16111";
    if (args.length > 0) {
      if ("--help".equals(args[0])) {
        System.err.printf("Usage: [name [target]]%n%n");
        System.err.printf("  location Get Header for this location. Defaults to %s%n", location);
        System.err.printf("  target  The server to connect to. Defaults to %s%n", target);
        System.exit(1);
      }
      location = args[0];
    }
    if (args.length > 1) {
      target = args[1];
    }

    ManagedChannel channel = ManagedChannelBuilder.forTarget(target).usePlaintext().enableFullStreamDecompression()
        .maxInboundMessageSize(MAX_MESSAGE).usePlaintext().build();
    try {
      Stopwatch stopwatchAll = Stopwatch.createStarted();
      GcdmClient client = new GcdmClient(channel);
      Header header = client.getHeader(location);
      long total = 0;
      for (GcdmProto.Variable v : header.getRoot().getVarsList()) {
        Stopwatch s2 = Stopwatch.createStarted();
        Array<?> array = client.getData(location, v);
        s2.stop();
        if (array != null) {
          long size = array.length();
          double rate = ((double) size) / s2.elapsed(TimeUnit.MICROSECONDS);
          System.out.printf("    size = %d, time = %s rate = %10.4f MB/sec%n", size, s2, rate);
          total += size;
        }
      }
      stopwatchAll.stop();
      double rate = ((double) total) / stopwatchAll.elapsed(TimeUnit.MICROSECONDS);
      System.out.printf("*** %d bytes took %s = %10.4f MB/sec%n", total, stopwatchAll, rate);
    } finally {
      channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}
