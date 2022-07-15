/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package dev.ucdm.gcdm.server;

import com.google.common.base.Stopwatch;
import dev.ucdm.array.Array;
import dev.ucdm.array.Arrays;
import dev.ucdm.array.Section;
import dev.ucdm.core.api.Group;
import dev.ucdm.core.api.Variable;
import dev.ucdm.gcdm.GcdmConverter;
import dev.ucdm.gcdm.protogen.GcdmGrpc;
import dev.ucdm.gcdm.protogen.GcdmServerProto;
import dev.ucdm.gcdm.protogen.GcdmServerProto.CdmDataRequest;
import dev.ucdm.gcdm.protogen.GcdmServerProto.CdmDataResponse;
import io.grpc.Grpc;
import io.grpc.InsecureChannelCredentials;
import io.grpc.ManagedChannel;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.Test;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** A simple client that makes a CdmFile request from GcdmServer. */
public class TestServerCdmFileRequest {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TestServerCdmFileRequest.class);
  private static final int MAX_MESSAGE = 50 * 1000 * 1000;
  private static final String oldTestDir = "/media/snake/0B681ADF0B681ADF/thredds-test-data/local/thredds-test-data/cdmUnitTest/";
  private static final String localFilename = "oldTestDir/formats/netcdf4/e562p1_fp.inst3_3d_asm_Nv.20100907_00z+20100909_1200z.nc4";

  @Test
  public void testBadTarget() {
    try (GcdmClient client = new GcdmClient("localhost:16112")) {
      // this doesnt fail,  surprisingly
    }
  }

  @Test
  public void testBadTargetAndLocation() {
    try (GcdmClient client = new GcdmClient("localhost:16112")) {
      GcdmServerProto.CdmRequest request = GcdmServerProto.CdmRequest.newBuilder().setLocation("badLocation").build();
      Throwable cause = assertThrows(io.grpc.StatusRuntimeException.class, () -> client.blockingStub.getCdmFile(request));
      assertThat(cause.getMessage()).contains("UNAVAILABLE");
    }
  }

  @Test
  public void testBadLocation() {
    try (GcdmClient client = new GcdmClient("localhost:16111")) {
      GcdmServerProto.CdmRequest request = GcdmServerProto.CdmRequest.newBuilder().setLocation("badLocation").build();
      GcdmServerProto.CdmResponse response = client.blockingStub.getCdmFile(request);
      assertThat(response.hasError()).isTrue();
      assertThat(response.getError().getMessage()).isEqualTo("No data root for 'badLocation'");
      assertThat(response.getLocation()).isEqualTo("badLocation");
    }
  }

  @Test
  public void testGoodRootBadLocation() {
    try (GcdmClient client = new GcdmClient("localhost:16111")) {
      GcdmServerProto.CdmRequest request = GcdmServerProto.CdmRequest.newBuilder().setLocation("oldTestDir/badLocation").build();
      GcdmServerProto.CdmResponse response = client.blockingStub.getCdmFile(request);
      assertThat(response).isNotNull();
      assertThat(response.hasError()).isTrue();
      assertThat(response.getError().getMessage()).doesNotContain("thredds-test-data/cdmUnitTest"); // dont leak data roots
      assertThat(response.getError().getMessage()).contains("(No such file or directory)");
      assertThat(response.getLocation()).isEqualTo("oldTestDir/badLocation");
    }
  }

  @Test
  public void testGood() {
    try (GcdmClient client = new GcdmClient("localhost:16111")) {
      GcdmServerProto.CdmRequest request = GcdmServerProto.CdmRequest.newBuilder().setLocation("coreLocalNetcdf3Dir/testWriteFill.nc").build();
      GcdmServerProto.CdmResponse response = client.blockingStub.getCdmFile(request);
      assertThat(response).isNotNull();
      assertThat(response.hasError()).isFalse();
      assertThat(response.getCdmFile()).isNotNull();
      assertThat(response.getCdmFile().getLocation()).isEqualTo("coreLocalNetcdf3Dir/testWriteFill.nc");
      assertThat(response.getLocation()).isEqualTo("coreLocalNetcdf3Dir/testWriteFill.nc");
    }
  }

  @Test
  public void testBadTargetAndLocationData() {
    try (GcdmClient client = new GcdmClient("localhost:16112")) {
      var request = GcdmServerProto.CdmDataRequest.newBuilder().setLocation("badLocation").build();
      var responses = client.blockingStub.getCdmData(request);
      assertThat(responses).isNotNull();  // surprise

      Throwable cause = assertThrows(io.grpc.StatusRuntimeException.class, () -> responses.hasNext());
      assertThat(cause.getMessage()).contains("UNAVAILABLE");
    }
  }

  @Test
  public void testBadLocationData() {
    try (GcdmClient client = new GcdmClient("localhost:16111")) {
      var request = GcdmServerProto.CdmDataRequest.newBuilder().setLocation("badLocation").build();
      var responses = client.blockingStub.getCdmData(request);
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
  public void testNoVariableName() {
    try (GcdmClient client = new GcdmClient("localhost:16111")) {
      GcdmServerProto.CdmDataRequest request = GcdmServerProto.CdmDataRequest.newBuilder().setLocation("oldTestDir/badLocation").build();
      var responses = client.blockingStub.getCdmData(request);
      assertThat(responses).isNotNull();
      while( responses.hasNext()) {
        var response = responses.next();
        assertThat(response.hasError()).isTrue();
        assertThat(response.getError().getMessage()).doesNotContain("thredds-test-data/cdmUnitTest"); // dont leak data roots
        assertThat(response.getError().getMessage()).contains("(No such file or directory)");
        assertThat(response.getLocation()).isEqualTo("oldTestDir/badLocation");
        assertThat(response.getVariableSpec()).isEmpty();
      }
    }
  }

  @Test
  public void testBadVariableName() {
    try (GcdmClient client = new GcdmClient("localhost:16111")) {
      GcdmServerProto.CdmDataRequest request = GcdmServerProto.CdmDataRequest.newBuilder()
              .setLocation("oldTestDir/badLocation")
              .setVariableSpec("coreLocalNetcdf3Dir/testWriteFill.nc")
              .build();
      var responses = client.blockingStub.getCdmData(request);
      assertThat(responses).isNotNull();
      while( responses.hasNext()) {
        var response = responses.next();
        assertThat(response.hasError()).isTrue();
        assertThat(response.getError().getMessage()).doesNotContain("thredds-test-data/cdmUnitTest"); // dont leak data roots
        assertThat(response.getError().getMessage()).contains("(No such file or directory)");
        assertThat(response.getLocation()).isEqualTo("oldTestDir/badLocation");
      }
    }
  }

  @Test
  public void testGoodData() {
    try (GcdmClient client = new GcdmClient("localhost:16111")) {
      GcdmServerProto.CdmDataRequest request = GcdmServerProto.CdmDataRequest.newBuilder()
              .setLocation("coreLocalNetcdf3Dir/testWriteFill.nc")
              .setVariableSpec("rtemperature")
              .build();
      var responses = client.blockingStub.getCdmData(request);
      assertThat(responses).isNotNull();
      while( responses.hasNext()) {
        var response = responses.next();
        assertThat(response.hasError()).isFalse();
        assertThat(response.getData()).isNotNull();
        assertThat(response.getLocation()).isEqualTo("coreLocalNetcdf3Dir/testWriteFill.nc");
        assertThat(response.getVariableSpec()).isEqualTo("rtemperature(0:0, 0:5, 0:11)");
        assertThat(response.getVarFullName()).isEqualTo("rtemperature");
        Section cdmSect = GcdmConverter.decodeSection(response.getSection());
        assertThat(cdmSect.toString()).isEqualTo("0:0,0:5,0:11");
      }
    }
  }

  @Test
  public void testDataReading() {
    String location = localFilename;
    String endpoint = "localhost:16111";

    Stopwatch stopwatchAll = Stopwatch.createStarted();
    try (GcdmClient client= new GcdmClient(endpoint)) {
      Group root = client.getHeader(location);
      // System.out.printf("header = %s%n", root);
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
          if (total > 1000 * 1000 * 50) {
            break;
          }
        }
      }
      stopwatchAll.stop();
      double rate = ((double) total) / stopwatchAll.elapsed(TimeUnit.MICROSECONDS);
      System.out.printf("*** %d bytes took %s = %10.4f MB/sec%n", total, stopwatchAll, rate);
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
