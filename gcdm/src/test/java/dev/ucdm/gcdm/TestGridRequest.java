/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package dev.ucdm.gcdm;

import com.google.common.base.Stopwatch;
import dev.ucdm.gcdm.protogen.GcdmGridProto;
import dev.ucdm.gcdm.protogen.GcdmServerProto;
import dev.ucdm.grid.api.GridReferencedArray;
import dev.ucdm.grid.api.GridSubset;
import io.grpc.Channel;
import io.grpc.Grpc;
import io.grpc.InsecureChannelCredentials;
import io.grpc.ManagedChannel;
import io.grpc.StatusRuntimeException;
import java.util.Formatter;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import dev.ucdm.gcdm.protogen.GcdmGrpc;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;

/** A simple client that makes a GridDataset request from GcdmServer.*/
public class TestGridRequest {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TestGridRequest.class);
  private static final int MAX_MESSAGE = 50 * 1000 * 1000;
  private static final String target = "localhost:16111";
  private static final String location = "oldTestDir/ft/grid/echoTops_runtime.nc";
  private static final String gridName = "ECHO_TOP";

  @Test
  public void testGridRequest() throws InterruptedException {

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
      client.readHeader(location);
      long total = client.readData(location, gridName);
      stopwatchAll.stop();
      double rate = ((double) total) / stopwatchAll.elapsed(TimeUnit.MICROSECONDS);
      System.out.printf("*** %d bytes took %s = %10.4f MB/sec%n", total, stopwatchAll, rate);
    } finally {
      channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
    }
  }

  private class GcdmClient {
    private final GcdmGrpc.GcdmBlockingStub blockingStub;
    GcdmGridProto.GridDataset gridDataset;

    public GcdmClient(Channel channel) {
      blockingStub = GcdmGrpc.newBlockingStub(channel);
    }

    public void readHeader(String location) {
      System.out.printf("CdmFile request %s%n", location);
      GcdmServerProto.GridDatasetRequest request = GcdmServerProto.GridDatasetRequest.newBuilder().setLocation(location).build();
      GcdmServerProto.GridDatasetResponse response;
      try {
        response = blockingStub.getGridDataset(request);
        if (response.hasError()) {
          throw new RuntimeException(response.getError().getMessage());
        } else {
          this.gridDataset = response.getGridDataset();
        }
      } catch (StatusRuntimeException e) {
        logger.warn("getHeader failed: " + location, e);
        throw new RuntimeException(e);
      }
    }

    public long readData(String location, String gridName) {
      var subset = GridSubset.create().setGridName(gridName);
      var requestb = GcdmServerProto.GridDataRequest.newBuilder()
              .setLocation(location);
      for (Map.Entry<String, Object> entry : subset.getEntries()) {
        requestb.putSubset(entry.getKey(), entry.getValue().toString());
      }
      var request = requestb.build();

      long total = 0;
      Iterator<GcdmServerProto.GridDataResponse> responses;
      try {
        responses = blockingStub.withDeadlineAfter(30, TimeUnit.SECONDS).getGridData(request);
        Formatter errlog = new Formatter();
        while (responses.hasNext()) {
          GcdmServerProto.GridDataResponse response = responses.next();
          GridReferencedArray result = GcdmGridConverter.decodeGridReferencedArray(response.getData(), errlog);
          assertThat(result).isNotNull();
          assertThat(result.materializedCoordinateSystem()).isNotNull();
          assertThat(result.materializedCoordinateSystem().getMaterializedShape()).isEqualTo(List.of(24,1,1,1));
          total += result.data().getSize();
        }
      } catch (Throwable e) {
        logger.warn("getData failed: " + location, e);
        throw new RuntimeException(e);
      }
      return total;
    }
  }

}
