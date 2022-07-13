/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package dev.ucdm.gcdm;

import com.google.common.base.Stopwatch;
import dev.ucdm.array.Array;
import dev.ucdm.array.Arrays;
import dev.ucdm.core.api.Group;
import dev.ucdm.core.api.Variable;
import dev.ucdm.gcdm.protogen.GcdmGrpc;
import dev.ucdm.gcdm.protogen.GcdmServerProto;
import dev.ucdm.gcdm.protogen.GcdmServerProto.CdmDataRequest;
import dev.ucdm.gcdm.protogen.GcdmServerProto.CdmDataResponse;
import io.grpc.Channel;
import io.grpc.Grpc;
import io.grpc.InsecureChannelCredentials;
import io.grpc.ManagedChannel;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.google.common.truth.Truth.assertThat;

/** A simple client that makes a CdmFile request from GcdmServer. */
public class TestCdmRequest {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TestCdmRequest.class);
  private static final int MAX_MESSAGE = 50 * 1000 * 1000;
  private static final String oldTestDir = "/media/snake/0B681ADF0B681ADF/thredds-test-data/local/thredds-test-data/cdmUnitTest/";
  private static final String localFilename = "oldTestDir/formats/netcdf4/e562p1_fp.inst3_3d_asm_Nv.20100907_00z+20100909_1200z.nc4";

  @Test
  public void testClient() throws InterruptedException {
    String location = localFilename;
    String target = "localhost:16111";

    // Base case - no encryption or authentication
    ManagedChannel channel = Grpc.newChannelBuilder(
                    target, InsecureChannelCredentials.create())
            .maxInboundMessageSize(MAX_MESSAGE) //
            .build();

    /* ManagedChannel channel = ManagedChannelBuilder.forTarget(target)
            // Channels are secure by default (via SSL/TLS). For now, we disable TLS to avoid needing certificates.
            .usePlaintext() //
            .enableFullStreamDecompression() //
            .maxInboundMessageSize(MAX_MESSAGE) //
            .build(); */

    // On the client side, server authentication with SSL/TLS
    // see https://grpc.io/docs/guides/auth/#base-case---no-encryption-or-authentication-5

    try {
      Stopwatch stopwatchAll = Stopwatch.createStarted();
      GcdmClient client = new GcdmClient(channel);
      Group root = client.getHeader(location);
      System.out.printf("header = %s%n", root);
      assertThat(root).isNotNull();

      long total = 0;
      for (Variable v : root.getVariables()) {
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

  private class GcdmClient {
    private final GcdmGrpc.GcdmBlockingStub blockingStub;

    public GcdmClient(Channel channel) {
      blockingStub = GcdmGrpc.newBlockingStub(channel);
    }

    public Group getHeader(String location) {
      System.out.printf("CdmFile request %s%n", location);
      GcdmServerProto.CdmRequest request = GcdmServerProto.CdmRequest.newBuilder().setLocation(location).build();
      GcdmServerProto.CdmResponse response;
      try {
        response = blockingStub.getCdmFile(request);
        if (response.hasError()) {
          throw new RuntimeException(response.getError().getMessage());
        } else {
          var cdmFile = response.getCdmFile();
          Group.Builder root = Group.builder().setName("");
          GcdmConverter.decodeGroup(cdmFile.getRoot(), root);
          return root.build();
        }
      } catch (StatusRuntimeException e) {
        logger.warn("getHeader failed: " + location, e);
        e.printStackTrace();
      }
      return null;
    }

    public Array<?> getData(String location, Variable v) {
      if (!v.getArrayType().isFloatingPoint()) { //  || v.getSize() > MAX_MESSAGE) {
        System.out.printf("***skip %s (%s)%n", v.getShortName(), v.getSize());
        return null;
      }
      System.out.printf("Data request %s %s (%d)%n", v.getArrayType(), v.getShortName(), v.getSize());
      CdmDataRequest request = CdmDataRequest.newBuilder().setLocation(location).setVariableSpec(v.getShortName()).build();
      Iterator<CdmDataResponse> responses;
      try {
        responses = blockingStub.withDeadlineAfter(30, TimeUnit.SECONDS).getCdmData(request);
        List<Array<?>> results = new ArrayList<>();
        while (responses.hasNext()) {
          CdmDataResponse response = responses.next();
          results.add(GcdmConverter.decodeData(response.getData()));
        }
        return Arrays.combine(v.getArrayType(), v.getShape(), results);
      } catch (Throwable e) {
        logger.warn("getData failed: " + location, e);
        e.printStackTrace();
        return null;
      }
    }
  }

}
