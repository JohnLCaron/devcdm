/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package dev.ucdm.gcdm.server;

import com.google.common.base.Stopwatch;
import dev.ucdm.core.write.ChunkingIndex;
import dev.ucdm.dataset.api.CdmDatasets;
import dev.ucdm.gcdm.GcdmConverter;
import dev.ucdm.gcdm.GcdmGridConverter;
import dev.ucdm.gcdm.protogen.GcdmServerProto;
import io.grpc.Grpc;
import io.grpc.Metadata;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerCall;
import io.grpc.ServerCall.Listener;
import io.grpc.ServerCallHandler;
import io.grpc.ServerCredentials;
import io.grpc.ServerInterceptor;
import io.grpc.TlsServerCredentials;
import io.grpc.stub.StreamObserver;

import java.io.File;
import java.io.IOException;
import java.util.Formatter;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import dev.ucdm.array.*;
import dev.ucdm.gcdm.protogen.GcdmGrpc.GcdmImplBase;
import dev.ucdm.gcdm.protogen.GcdmProto;
import dev.ucdm.gcdm.protogen.GcdmServerProto.CdmDataRequest;
import dev.ucdm.gcdm.protogen.GcdmServerProto.CdmDataResponse;
import dev.ucdm.gcdm.protogen.GcdmServerProto.CdmRequest;
import dev.ucdm.gcdm.protogen.GcdmServerProto.CdmResponse;
import dev.ucdm.array.InvalidRangeException;
import dev.ucdm.array.Section;
import dev.ucdm.core.api.*;
import dev.ucdm.dataset.transform.vertical.VerticalTransform;
import dev.ucdm.grid.api.*;

/**
 * Server that manages startup/shutdown of a gCDM Server.
 * Note that CdmDataset / GridDataset is opened/closed on each request.
 * TODO test performance w/wo caching
 */
public class GcdmServer {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(GcdmServer.class);
  private static final int MAX_MESSAGE = 50 * 1000 * 1000; // 50 Mb LOOK where is this set server or client ??
  private static final int SEQUENCE_CHUNK = 1000;

  private Server server;
  /* The port on which the server should run */
  int port = 16111;

  private void start() throws IOException {
    // no security
    server = ServerBuilder.forPort(port) //
            .addService(new GcdmImpl()) //
            // .intercept(new MyServerInterceptor())
            .build().start();
    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        // Use stderr here since the logger may have been reset by its JVM shutdown hook.
        System.err.println("*** shutting down gRPC server since JVM is shutting down");
        try {
          GcdmServer.this.stop();
        } catch (InterruptedException e) {
          e.printStackTrace(System.err);
        }
        System.err.println("*** server shut down");
      }
    });
    logger.info("Server started, listening on " + port);
    System.out.println("---> Server started, listening on " + port);
  }

  private void startWithTLS() throws IOException {
    // Creates an instance using provided certificate chain and private key.
    // Generally they should be PEM-encoded and the key is an unencrypted PKCS#8 key
    // (file headers have "BEGIN CERTIFICATE" and "BEGIN PRIVATE KEY").
    // ServerCredentials creds = TlsServerCredentials.create(certChainFile, privateKeyFile);
    ServerCredentials creds = TlsServerCredentials.create((File) null, (File) null);
    Server server = Grpc.newServerBuilderForPort(port, creds)
            .addService(new GcdmImpl()) //
            .build().start();

    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        // Use stderr here since the logger may have been reset by its JVM shutdown hook.
        System.err.println("*** shutting down gRPC server since JVM is shutting down");
        try {
          GcdmServer.this.stop();
        } catch (InterruptedException e) {
          e.printStackTrace(System.err);
        }
        System.err.println("*** server shut down");
      }
    });

    logger.info("Server started, listening on " + port);
    System.out.println("---> Server started, listening on " + port);
  }

  private void stop() throws InterruptedException {
    if (server != null) {
      server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
    }
  }

  /**
   * Await termination on the main thread since the grpc library uses daemon threads.
   */
  private void blockUntilShutdown() throws InterruptedException {
    if (server != null) {
      server.awaitTermination();
    }
  }

  /**
   * Main launches the server from the command line.
   */
  public static void main(String[] args) throws IOException, InterruptedException {
    final GcdmServer server = new GcdmServer();
    server.start();
    server.blockUntilShutdown();
  }

  static class MyServerInterceptor implements ServerInterceptor {
    @Override
    public <ReqT, RespT> Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata requestHeaders,
                                                      ServerCallHandler<ReqT, RespT> next) {
      System.out.printf("***ServerCall %s%n", call);
      System.out.printf("   Attributes %s%n", call.getAttributes());
      System.out.printf("   MethodDesc %s%n", call.getMethodDescriptor());
      System.out.printf("   Authority %s%n", call.getAuthority());
      System.out.printf("   Metadata %s%n", requestHeaders);
      return next.startCall(call, requestHeaders);
    }
  }

  private static Map<String, String> dataRoots = Map.of(
          "extraTestDir", "/home/snake/tmp/testData/",
          "oldTestDir", "/media/snake/0B681ADF0B681ADF/thredds-test-data/local/thredds-test-data/cdmUnitTest/"
  );

  ///////////////////////////////////////////////////////////////////////////////////////
  static class GcdmImpl extends GcdmImplBase {
    DataRoots roots = new DataRoots();

    @Override
    public void getCdmFile(CdmRequest req, StreamObserver<CdmResponse> responseObserver) {
      CdmResponse.Builder response = CdmResponse.newBuilder();

      String dataPath = roots.convertRootToPath(req.getLocation());
      if (dataPath == null) {
        response.setError(GcdmProto.Error.newBuilder()
                .setMessage(String.format("No data root for '%s'", req.getLocation()))
                .build());
      } else {
        System.out.printf("GcdmServer getHeader open '%s' -> '%s'%n", req.getLocation(), dataPath);

        try (CdmFile ncfile = CdmDatasets.openFile(dataPath, null)) {
          GcdmProto.CdmFile.Builder cdmFile = GcdmProto.CdmFile.newBuilder().setLocation(req.getLocation())
                  .setRoot(GcdmConverter.encodeGroup(ncfile.getRootGroup(), 100).build());
          response.setCdmFile(cdmFile);
          logger.info("GcdmServer getHeader " + req.getLocation());
        } catch (Throwable t) {
          logger.warn("GcdmServer getHeader failed, returning an error", t);
          // t.printStackTrace();
          response.setError(GcdmProto.Error.newBuilder().setMessage(t.getMessage()).build());
        }
      }

      responseObserver.onNext(response.build());
      responseObserver.onCompleted();
    }

    @Override
    public void getCdmData(CdmDataRequest req, StreamObserver<CdmDataResponse> responseObserver) {
      System.out.printf("GcdmServer getData %s %s%n", req.getLocation(), req.getVariableSpec());
      final Stopwatch stopwatch = Stopwatch.createStarted();
      long size = -1;

      String dataPath = roots.convertRootToPath(req.getLocation());
      if (dataPath == null) {
        CdmDataResponse.Builder response =
                CdmDataResponse.newBuilder().setLocation(req.getLocation()).setVariableSpec(req.getVariableSpec());
        response.setError(GcdmProto.Error.newBuilder()
                .setMessage(String.format("No data root for '%s'", req.getLocation()))
                .build());
        responseObserver.onNext(response.build());

      } else {
        System.out.printf("GcdmServer getHeader open '%s' -> '%s'%n", req.getLocation(), dataPath);

        try (CdmFile ncfile = CdmDatasets.openFile(dataPath, null)) { // TODO cache ncfile?
          ParsedArraySectionSpec varSection = ParsedArraySectionSpec.parseVariableSection(ncfile, req.getVariableSpec());
          Variable var = varSection.getVariable();
          if (var instanceof Sequence) {
            size = getSequenceData(ncfile, varSection, responseObserver);
          } else {
            Section wantSection = varSection.getSection();
            size = var.getElementSize() * wantSection.computeSize();
            getNetcdfData(ncfile, varSection, responseObserver);
          }
          logger.info("GcdmServer getData " + req.getLocation());

        } catch (Throwable t) {
          logger.warn("GcdmServer getData failed, returning an error", t);
          // t.printStackTrace();
          CdmDataResponse.Builder response =
                  CdmDataResponse.newBuilder().setLocation(req.getLocation()).setVariableSpec(req.getVariableSpec());
          response.setError(
                  GcdmProto.Error.newBuilder().setMessage(t.getMessage() == null ? "N/A" : t.getMessage()).build());
          responseObserver.onNext(response.build());
        }
      }
      responseObserver.onCompleted();

      System.out.printf(" ** size=%d took=%s%n", size, stopwatch.stop());
    }

    private void getNetcdfData(CdmFile ncfile, ParsedArraySectionSpec varSection,
                               StreamObserver<CdmDataResponse> responseObserver) throws IOException, InvalidRangeException {

      Variable var = varSection.getVariable();
      Section wantSection = varSection.getSection();
      long size = var.getElementSize() * wantSection.computeSize();
      if (size > MAX_MESSAGE) {
        getDataInChunks(ncfile, varSection, responseObserver);
      } else {
        getOneChunk(ncfile, varSection, responseObserver);
      }
    }

    private void getDataInChunks(CdmFile ncfile, ParsedArraySectionSpec varSection,
                                 StreamObserver<CdmDataResponse> responseObserver) throws IOException, InvalidRangeException {

      Variable var = varSection.getVariable();
      long maxChunkElems = MAX_MESSAGE / var.getElementSize();
      // TODO wrong this assume starts at 0, should start at varSection
      ChunkingIndex index = new ChunkingIndex(var.getShape());
      while (index.currentElement() < index.size()) {
        int[] chunkOrigin = index.currentCounter();
        int[] chunkShape = index.computeChunkShape(maxChunkElems);
        Section section = new Section(chunkOrigin, chunkShape);
        ParsedArraySectionSpec spec = new ParsedArraySectionSpec(var, section);
        getOneChunk(ncfile, spec, responseObserver);
        index.setCurrentCounter(index.currentElement() + (int) Arrays.computeSize(chunkShape));
      }
    }

    private void getOneChunk(CdmFile ncfile, ParsedArraySectionSpec varSection,
                             StreamObserver<CdmDataResponse> responseObserver) throws IOException, InvalidRangeException {

      String spec = varSection.makeSectionSpecString();
      Variable var = varSection.getVariable();
      Section wantSection = varSection.getSection();

      CdmDataResponse.Builder response = CdmDataResponse.newBuilder().setLocation(ncfile.getLocation()).setVariableSpec(spec)
              .setVarFullName(var.getFullName()).setSection(GcdmConverter.encodeSection(wantSection));

      Array<?> data = var.readArray(wantSection);
      response.setData(GcdmConverter.encodeData(data.getArrayType(), data));

      responseObserver.onNext(response.build());
      System.out.printf(" Send one chunk %s size=%d bytes%n", spec,
              data.length() * varSection.getVariable().getElementSize());
    }


    private long getSequenceData(CdmFile ncfile, ParsedArraySectionSpec varSection,
                                 StreamObserver<CdmDataResponse> responseObserver) throws InvalidRangeException {

      String spec = varSection.makeSectionSpecString();
      Sequence seq = (Sequence) varSection.getVariable();
      StructureMembers.Builder membersb = seq.makeStructureMembersBuilder().setStandardOffsets();
      StructureMembers members = membersb.build();

      StructureData[] sdata = new StructureData[SEQUENCE_CHUNK];
      int start = 0;
      int count = 0;
      Iterator<StructureData> it = seq.iterator();
      while (it.hasNext()) {
        sdata[count++] = it.next();

        if (count >= SEQUENCE_CHUNK || !it.hasNext()) {
          StructureDataArray sdataArray = new StructureDataArray(members, new int[]{count}, sdata);
          Section section = Section.builder().appendRange(start, start + count).build();
          CdmDataResponse.Builder response = CdmDataResponse.newBuilder().setLocation(ncfile.getLocation())
                  .setVariableSpec(spec).setVarFullName(seq.getFullName()).setSection(GcdmConverter.encodeSection(section));
          response.setData(GcdmConverter.encodeData(ArrayType.SEQUENCE, sdataArray));
          responseObserver.onNext(response.build());
          start = count;
          count = 0;
        }
      }
      return (start + count) * members.getStorageSizeBytes();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////
    // GridDataset

    @Override
    public void getGridDataset(GcdmServerProto.GridDatasetRequest request,
                               io.grpc.stub.StreamObserver<GcdmServerProto.GridDatasetResponse> responseObserver) {
      GcdmServerProto.GridDatasetResponse.Builder response = GcdmServerProto.GridDatasetResponse.newBuilder();

      String dataPath = roots.convertRootToPath(request.getLocation());
      if (dataPath == null) {
        response.setError(GcdmProto.Error.newBuilder()
                .setMessage(String.format("No data root for '%s'", request.getLocation()))
                .build());
      } else {

        System.out.printf("GcdmServer getGridDataset open %s%n", dataPath);
        Formatter errlog = new Formatter();
        try (GridDataset gridDataset = GridDatasetFactory.openGridDataset(dataPath, errlog)) {
          if (gridDataset == null) {
            response.setError(GcdmProto.Error.newBuilder()
                    .setMessage(String.format("Dataset '%s' not found or not a GridDataset", request.getLocation())).build());
          } else {
            response.setGridDataset(GcdmGridConverter.encodeGridDataset(gridDataset, request.getLocation()));
          }
          logger.info("GcdmServer getGridDataset " + request.getLocation());
        } catch (Throwable t) {
          System.out.printf("GcdmServer getGridDataset failed %s %n%s%n", t.getMessage(), errlog);
          logger.warn("GcdmServer getGridDataset failed ", t);
          // t.printStackTrace();
          response.setError(GcdmProto.Error.newBuilder().setMessage(t.getMessage()).build());
        }
      }

      responseObserver.onNext(response.build());
      responseObserver.onCompleted();
    }

    @Override
    public void getGridData(GcdmServerProto.GridDataRequest request,
                            io.grpc.stub.StreamObserver<dev.ucdm.gcdm.protogen.GcdmServerProto.GridDataResponse> responseObserver) {

      String dataPath = roots.convertRootToPath(request.getLocation());
      if (dataPath == null) {
        GcdmServerProto.GridDataResponse.Builder response =
                GcdmServerProto.GridDataResponse.newBuilder().setLocation(request.getLocation());
        response.setError(GcdmProto.Error.newBuilder()
                .setMessage(String.format("No data root for '%s'", request.getLocation()))
                .build());
        responseObserver.onNext(response.build());
        responseObserver.onCompleted();
        return;

      } else {
        System.out.printf("GcdmServer getData %s %s == %s%n", request.getLocation(), request.getSubsetMap(), dataPath);
        GcdmServerProto.GridDataResponse.Builder response = GcdmServerProto.GridDataResponse.newBuilder();
        response.setLocation(request.getLocation()).putAllSubset(request.getSubsetMap());

        final Stopwatch stopwatch = Stopwatch.createStarted();

        GridSubset gridSubset = GridSubset.fromStringMap(request.getSubsetMap());
        if (gridSubset.getGridName() == null) {
          makeError(response, "GridName is not set");
          responseObserver.onNext(response.build());
          responseObserver.onCompleted();
          return;
        }

        Formatter errlog = new Formatter();
        try (GridDataset gridDataset = GridDatasetFactory.openGridDataset(dataPath, errlog)) {
          if (gridDataset == null) {
            makeError(response, String.format("GridDataset '%s' not found", request.getLocation()));
          } else {
            String wantGridName = gridSubset.getGridName();
            Grid wantGrid = gridDataset.findGrid(wantGridName).orElse(null);
            if (wantGrid == null) {
              makeError(response,
                      String.format("GridDataset '%s' does not have Grid '%s", request.getLocation(), wantGridName));
            } else {
              GridReferencedArray geoReferencedArray = wantGrid.readData(gridSubset);
              response.setData(GcdmGridConverter.encodeGridReferencedArray(geoReferencedArray));
              System.out.printf(" ** size=%d shape=%s%n", geoReferencedArray.data().length(),
                      java.util.Arrays.toString(geoReferencedArray.data().getShape()));
            }
          }
        } catch (Throwable t) {
          logger.warn("GcdmServer getGridData failed ", t);
          t.printStackTrace();
          errlog.format("%n%s", t.getMessage() == null ? "" : t.getMessage());
          makeError(response, errlog.toString());
        }

        responseObserver.onNext(response.build());
        responseObserver.onCompleted();
        System.out.printf(" ** took=%s%n", stopwatch.stop());
      }
    }

    void makeError(GcdmServerProto.GridDataResponse.Builder response, String message) {
      response.setError(GcdmProto.Error.newBuilder().setMessage(message).build());
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////
    // VerticalTransform

    @Override
    public void getVerticalTransform(GcdmServerProto.VerticalTransformRequest request,
                                     io.grpc.stub.StreamObserver<GcdmServerProto.VerticalTransformResponse> responseObserver) {
      String dataPath = roots.convertRootToPath(request.getLocation());
      if (dataPath == null) {
        GcdmServerProto.VerticalTransformResponse.Builder response = GcdmServerProto.VerticalTransformResponse.newBuilder();
        response.setError(GcdmProto.Error.newBuilder()
                .setMessage(String.format("No data root for '%s'", request.getLocation()))
                .build());
        responseObserver.onNext(response.build());
        responseObserver.onCompleted();
        return;
      } else {
        System.out.printf("GcdmServer getVerticalTransform open %s%n", request.getLocation());
        GcdmServerProto.VerticalTransformResponse.Builder response = GcdmServerProto.VerticalTransformResponse.newBuilder();
        Formatter errlog = new Formatter();
        try (GridDataset gridDataset = GridDatasetFactory.openGridDataset(request.getLocation(), errlog)) {
          response.setLocation(request.getLocation());
          response.setVerticalTransform(request.getVerticalTransform());
          response.setTimeIndex(request.getTimeIndex());

          if (gridDataset == null) {
            response.setError(GcdmProto.Error.newBuilder().setMessage("Dataset not found or not a GridDataset").build());
          } else {
            Optional<VerticalTransform> cto = gridDataset.findVerticalTransformByHash(request.getId());
            if (cto.isPresent()) {
              Array<?> data = cto.get().getCoordinateArray3D(request.getTimeIndex());
              response.setData3D(GcdmConverter.encodeData(data.getArrayType(), data));
            } else {
              response.setError(GcdmProto.Error.newBuilder().setMessage("VerticalTransform not found").build());
            }
          }
          logger.info("GcdmServer getVerticalTransform " + request.getLocation());
        } catch (Throwable t) {
          System.out.printf("GcdmServer getVerticalTransform failed %s %n%s%n", t.getMessage(), errlog);
          logger.warn("GcdmServer getVerticalTransform failed ", t);
          // t.printStackTrace();
          response.setError(GcdmProto.Error.newBuilder().setMessage(t.getMessage()).build());
        }

        responseObserver.onNext(response.build());
        responseObserver.onCompleted();
      }
    }


  } // GcdmImpl
}
