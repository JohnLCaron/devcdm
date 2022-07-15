/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package dev.ucdm.gcdm.server;

import dev.ucdm.array.Array;
import dev.ucdm.array.Arrays;
import dev.ucdm.array.InvalidRangeException;
import dev.ucdm.gcdm.GcdmConverter;
import dev.ucdm.gcdm.protogen.GcdmGrpc;
import dev.ucdm.gcdm.protogen.GcdmServerProto;
import io.grpc.Grpc;
import io.grpc.InsecureChannelCredentials;
import io.grpc.ManagedChannel;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.Test;

import java.io.Closeable;
import java.util.concurrent.TimeUnit;

import static com.google.common.truth.Truth.assertThat;
import static dev.ucdm.array.PrintArray.printArray;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** A simple client that makes a VerticalTransform request from GcdmServer.*/
public class TestServerVerticalTransform {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TestServerVerticalTransform.class);
  private static final int MAX_MESSAGE = 50 * 1000 * 1000;
  private static final String target = "localhost:16111";
  private static final String location = "extraTestDir/transforms/ocean_his_g2.nc";
  private static final String gridName = "u";

  @Test
  public void testBadTargetAndLocation() {
    try (GcdmClient client = new GcdmClient("localhost:16112")) {
      GcdmServerProto.VerticalTransformRequest request = GcdmServerProto.VerticalTransformRequest.newBuilder().setLocation("badLocation").build();
      Throwable cause = assertThrows(StatusRuntimeException.class, () -> client.blockingStub.getVerticalTransform(request));
      assertThat(cause.getMessage()).contains("UNAVAILABLE");
    }
  }

  @Test
  public void testBadLocation() {
    try (GcdmClient client = new GcdmClient("localhost:16111")) {
      GcdmServerProto.VerticalTransformRequest request = GcdmServerProto.VerticalTransformRequest.newBuilder().setLocation("badLocation").build();
      GcdmServerProto.VerticalTransformResponse response = client.blockingStub.getVerticalTransform(request);
      assertThat(response.hasError()).isTrue();
      assertThat(response.getError().getMessage()).isEqualTo("No data root for 'badLocation'");
      assertThat(response.getLocation()).isEqualTo("badLocation");
    }
  }

  @Test
  public void testGoodRootBadLocation() {
    try (GcdmClient client = new GcdmClient("localhost:16111")) {
      GcdmServerProto.VerticalTransformRequest request = GcdmServerProto.VerticalTransformRequest.newBuilder().setLocation("oldTestDir/badLocation").build();
      GcdmServerProto.VerticalTransformResponse response = client.blockingStub.getVerticalTransform(request);
      assertThat(response).isNotNull();
      assertThat(response.hasError()).isTrue();
      assertThat(response.getError().getMessage()).doesNotContain("thredds-test-data/cdmUnitTest"); // dont leak data roots
      assertThat(response.getError().getMessage()).contains("(No such file or directory)");
      assertThat(response.getLocation()).isEqualTo("oldTestDir/badLocation");
      assertThat(response.getData3D()).isNotNull(); // surprise
    }
  }

  @Test
  public void testNotAGridDataset() {
    try (GcdmClient client = new GcdmClient("localhost:16111")) {
      GcdmServerProto.VerticalTransformRequest request = GcdmServerProto.VerticalTransformRequest.newBuilder().setLocation("coreLocalNetcdf3Dir/testWriteFill.nc").build();
      GcdmServerProto.VerticalTransformResponse response = client.blockingStub.getVerticalTransform(request);
      assertThat(response).isNotNull();
      assertThat(response.hasError()).isTrue();
      assertThat(response.getError().getMessage()).contains("Dataset not found or not a GridDataset");
      assertThat(response.getData3D()).isNotNull(); // surprise
      assertThat(response.getLocation()).isEqualTo("coreLocalNetcdf3Dir/testWriteFill.nc");
    }
  }

  @Test
  public void testNoTransformName() {
    try (GcdmClient client = new GcdmClient("localhost:16111")) {
      GcdmServerProto.VerticalTransformRequest request = GcdmServerProto.VerticalTransformRequest.newBuilder().setLocation(location).build();
      GcdmServerProto.VerticalTransformResponse response = client.blockingStub.getVerticalTransform(request);
      assertThat(response).isNotNull();
      assertThat(response.hasError()).isTrue();
      assertThat(response.getError().getMessage()).contains("The VerticalTransform name must be supplied");
      assertThat(response.getData3D()).isNotNull();
      assertThat(response.getLocation()).isEqualTo(location);
    }
  }

  @Test
  public void testBadTransformName() {
    try (GcdmClient client = new GcdmClient("localhost:16111")) {
      GcdmServerProto.VerticalTransformRequest request = GcdmServerProto.VerticalTransformRequest.newBuilder()
              .setLocation(location)
              .setVerticalTransform("bad")
              .build();
      GcdmServerProto.VerticalTransformResponse response = client.blockingStub.getVerticalTransform(request);
      assertThat(response).isNotNull();
      assertThat(response.hasError()).isTrue();
      assertThat(response.getError().getMessage()).contains("VerticalTransform 'bad' not found");
      assertThat(response.getData3D()).isNotNull();
      assertThat(response.getLocation()).isEqualTo(location);
      assertThat(response.getVerticalTransform()).isEqualTo("bad");
    }
  }

  @Test
  public void testGood() throws InvalidRangeException {
    try (GcdmClient client = new GcdmClient("localhost:16111")) {
      GcdmServerProto.VerticalTransformRequest request = GcdmServerProto.VerticalTransformRequest.newBuilder()
              .setLocation(location)
              .setVerticalTransform("ocean_s_coordinate_g2")
              .build();
      GcdmServerProto.VerticalTransformResponse response = client.blockingStub.getVerticalTransform(request);
      assertThat(response).isNotNull();
      assertThat(response.getLocation()).isEqualTo(location);
      assertThat(response.getVerticalTransform()).isEqualTo("ocean_s_coordinate_g2");
      assertThat(response.hasError()).isFalse();
      assertThat(response.getData3D()).isNotNull();
      Array<?> data = GcdmConverter.decodeData(response.getData3D());
      assertThat(data.getRank()).isEqualTo(3);
      assertThat(data.getShape()).isEqualTo(new int[] {6, 120, 156});
      System.out.printf("data = %s%n", printArray(Arrays.slice(data, 0, 0)));
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
