/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package dev.ucdm.gcdm.server;

import com.google.common.base.Stopwatch;
import dev.ucdm.gcdm.GcdmGridConverter;
import dev.ucdm.gcdm.protogen.GcdmGridProto;
import dev.ucdm.gcdm.protogen.GcdmServerProto;
import dev.ucdm.grid.api.GridReferencedArray;
import dev.ucdm.grid.api.GridSubset;
import io.grpc.Grpc;
import io.grpc.InsecureChannelCredentials;
import io.grpc.ManagedChannel;
import io.grpc.StatusRuntimeException;

import java.io.Closeable;
import java.util.Formatter;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import dev.ucdm.gcdm.protogen.GcdmGrpc;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** A simple client that makes a GridDataset request from GcdmServer.*/
public class TestServerGridRequest {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TestServerGridRequest.class);
  private static final int MAX_MESSAGE = 50 * 1000 * 1000;
  private static final String target = "localhost:16111";
  private static final String location = "oldTestDir/ft/grid/echoTops_runtime.nc";
  private static final String gridName = "ECHO_TOP";

  @Test
  public void testBadTargetAndLocation() {
    try (GcdmClient client = new GcdmClient("localhost:16112")) {
      GcdmServerProto.GridDatasetRequest request = GcdmServerProto.GridDatasetRequest.newBuilder().setLocation("badLocation").build();
      Throwable cause = assertThrows(io.grpc.StatusRuntimeException.class, () -> client.blockingStub.getGridDataset(request));
      assertThat(cause.getMessage()).contains("UNAVAILABLE");
    }
  }

  @Test
  public void testBadLocation() {
    try (GcdmClient client = new GcdmClient("localhost:16111")) {
      GcdmServerProto.GridDatasetRequest request = GcdmServerProto.GridDatasetRequest.newBuilder().setLocation("badLocation").build();
      GcdmServerProto.GridDatasetResponse response = client.blockingStub.getGridDataset(request);
      assertThat(response.hasError()).isTrue();
      assertThat(response.getError().getMessage()).isEqualTo("No data root for 'badLocation'");
      assertThat(response.getLocation()).isEqualTo("badLocation");
    }
  }

  @Test
  public void testGoodRootBadLocation() {
    try (GcdmClient client = new GcdmClient("localhost:16111")) {
      GcdmServerProto.GridDatasetRequest request = GcdmServerProto.GridDatasetRequest.newBuilder().setLocation("oldTestDir/badLocation").build();
      GcdmServerProto.GridDatasetResponse response = client.blockingStub.getGridDataset(request);
      assertThat(response).isNotNull();
      assertThat(response.hasError()).isTrue();
      assertThat(response.getError().getMessage()).doesNotContain("thredds-test-data/cdmUnitTest"); // dont leak data roots
      assertThat(response.getError().getMessage()).contains("(No such file or directory)");
      assertThat(response.getLocation()).isEqualTo("oldTestDir/badLocation");
      assertThat(response.getGridDataset()).isNotNull(); // surprise
      assertThat(response.getGridDataset().getLocation()).isNotNull(); // surprise
    }
  }

  @Test
  public void testNotAGridDataset() {
    try (GcdmClient client = new GcdmClient("localhost:16111")) {
      GcdmServerProto.GridDatasetRequest request = GcdmServerProto.GridDatasetRequest.newBuilder().setLocation("coreLocalNetcdf3Dir/testWriteFill.nc").build();
      GcdmServerProto.GridDatasetResponse response = client.blockingStub.getGridDataset(request);
      assertThat(response).isNotNull();
      assertThat(response.hasError()).isTrue();
      assertThat(response.getError().getMessage()).contains("Dataset 'coreLocalNetcdf3Dir/testWriteFill.nc' not found or not a GridDataset");
      assertThat(response.getGridDataset()).isNotNull(); // surprise
      assertThat(response.getGridDataset().getLocation()).isEmpty();
      assertThat(response.getLocation()).isEqualTo("coreLocalNetcdf3Dir/testWriteFill.nc");
    }
  }

  @Test
  public void testGood() {
    try (GcdmClient client = new GcdmClient("localhost:16111")) {
      GcdmServerProto.GridDatasetRequest request = GcdmServerProto.GridDatasetRequest.newBuilder().setLocation(location).build();
      GcdmServerProto.GridDatasetResponse response = client.blockingStub.getGridDataset(request);
      assertThat(response).isNotNull();
      assertThat(response.hasError()).isFalse();
      assertThat(response.getGridDataset()).isNotNull();
      assertThat(response.getGridDataset().getLocation()).isEqualTo(location);
      assertThat(response.getLocation()).isEqualTo(location);
    }
  }

  @Test
  public void testBadTargetAndLocationData() {
    try (GcdmClient client = new GcdmClient("localhost:16112")) {
      GcdmServerProto.GridDataRequest request = GcdmServerProto.GridDataRequest.newBuilder().setLocation("badLocation").build();
      var responses = client.blockingStub.getGridData(request);
      assertThat(responses).isNotNull();  // surprise

      Throwable cause = assertThrows(io.grpc.StatusRuntimeException.class, () -> responses.hasNext());
      assertThat(cause.getMessage()).contains("UNAVAILABLE");
    }
  }

  @Test
  public void testNoRootData() {
    try (GcdmClient client = new GcdmClient("localhost:16111")) {
      GcdmServerProto.GridDataRequest request = GcdmServerProto.GridDataRequest.newBuilder().setLocation("badLocation").build();
      var responses = client.blockingStub.getGridData(request);
      assertThat(responses).isNotNull();
      while (responses.hasNext()) {
        var response = responses.next();
        assertThat(response.hasError()).isTrue();
        assertThat(response.getError().getMessage()).isEqualTo("No data root for 'badLocation'");
        assertThat(response.getLocation()).isEqualTo("badLocation");
      }
    }
  }

  @Test
  public void testGoodRootBadLocationData() {
    try (GcdmClient client = new GcdmClient("localhost:16111")) {
      GcdmServerProto.GridDataRequest request = GcdmServerProto.GridDataRequest.newBuilder().setLocation("oldTestDir/badLocation").build();
      var responses = client.blockingStub.getGridData(request);
      assertThat(responses).isNotNull();
      while (responses.hasNext()) {
        var response = responses.next();
        assertThat(response.hasError()).isTrue();
        assertThat(response.getError().getMessage()).doesNotContain("thredds-test-data/cdmUnitTest"); // dont leak data roots
        assertThat(response.getError().getMessage()).contains("oldTestDir/badLocation (No such file or directory)"); // surprise
        assertThat(response.getLocation()).isEqualTo("oldTestDir/badLocation");
        assertThat(response.getData()).isNotNull(); // surprise
      }
    }
  }

  @Test
  public void testNotAGridDatasetData() {
    try (GcdmClient client = new GcdmClient("localhost:16111")) {
      GcdmServerProto.GridDataRequest request = GcdmServerProto.GridDataRequest.newBuilder().setLocation("coreLocalNetcdf3Dir/testWriteFill.nc").build();
      var responses = client.blockingStub.getGridData(request);
      assertThat(responses).isNotNull();
      while (responses.hasNext()) {
        var response = responses.next();
        assertThat(response).isNotNull();
        assertThat(response.hasError()).isTrue();
        assertThat(response.getError().getMessage()).contains("'coreLocalNetcdf3Dir/testWriteFill.nc' not a Grid dataset"); // surprise
        assertThat(response.getData()).isNotNull(); // surprise
        assertThat(response.getLocation()).isEqualTo("coreLocalNetcdf3Dir/testWriteFill.nc");
      }
    }
  }

  @Test
  public void testNoGridSetData() {
    try (GcdmClient client = new GcdmClient("localhost:16111")) {
      GcdmServerProto.GridDataRequest request = GcdmServerProto.GridDataRequest.newBuilder().setLocation(location).build();
      var responses = client.blockingStub.getGridData(request);
      assertThat(responses).isNotNull();
      while (responses.hasNext()) {
        var response = responses.next();
        assertThat(response).isNotNull();
        assertThat(response.hasError()).isTrue();
        assertThat(response.getError().getMessage()).contains("GridName is not set"); // surprise
        assertThat(response.getData()).isNotNull(); // surprise
        assertThat(response.getLocation()).isEqualTo(location);
      }
    }
  }

  @Test
  public void testBadGridNameData() {
    try (GcdmClient client = new GcdmClient("localhost:16111")) {
      GridSubset subset = GridSubset.create().setGridName("babby");
      GcdmServerProto.GridDataRequest request = GcdmServerProto.GridDataRequest.newBuilder()
              .setLocation(location)
              .putAllSubset(subset.getMap())
              .build();
      var responses = client.blockingStub.getGridData(request);
      assertThat(responses).isNotNull();
      while (responses.hasNext()) {
        var response = responses.next();
        assertThat(response).isNotNull();
        assertThat(response.hasError()).isTrue();
        assertThat(response.getError().getMessage()).contains("GridDataset 'oldTestDir/ft/grid/echoTops_runtime.nc' does not have Grid 'babby"); // surprise
        assertThat(response.getData()).isNotNull(); // surprise
        assertThat(response.getLocation()).isEqualTo(location);
        assertThat(response.getSubsetMap()).isEqualTo(subset.getMap());
      }
    }
  }

  @Test
  public void testGoodData() {
    try (GcdmClient client = new GcdmClient(target)) {
      GridSubset subset = GridSubset.create().setGridName(gridName);
      GcdmServerProto.GridDataRequest request = GcdmServerProto.GridDataRequest.newBuilder()
              .setLocation(location)
              .putAllSubset(subset.getMap())
              .build();
      var responses = client.blockingStub.getGridData(request);
      assertThat(responses).isNotNull();
      while (responses.hasNext()) {
        var response = responses.next();
        assertThat(response).isNotNull();
        assertThat(response.hasError()).isFalse();
        assertThat(response.getLocation()).isEqualTo(location);
        assertThat(response.getSubsetMap()).isEqualTo(subset.getMap());
        assertThat(response.getData()).isNotNull();
        assertThat(response.getData().getData()).isNotNull();
        assertThat(response.getData().getData().getArrayType()).isNotNull();
        assertThat(response.getData().getMaterializedCoordinateSystem()).isNotNull();
        assertThat(response.getData().getMaterializedCoordinateSystem().getAxesCount()).isEqualTo(4);
      }
    }
  }

  private class GcdmClient implements Closeable {
    ManagedChannel channel;
    GcdmGrpc.GcdmBlockingStub blockingStub;

    public GcdmClient(String endpoint) {
      this.channel = Grpc.newChannelBuilder(
                      endpoint, InsecureChannelCredentials.create())
              .maxInboundMessageSize(MAX_MESSAGE) //
              .build();
      blockingStub = GcdmGrpc.newBlockingStub(channel);
    }

    public void close() {
      try {
        channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }
  }

}
