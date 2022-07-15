/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package dev.ucdm.gcdm.server;

import com.google.common.base.Stopwatch;
import dev.ucdm.core.write.ChunkingIndex;
import dev.ucdm.dataset.api.CdmDatasets;
import dev.ucdm.dataset.transform.vertical.VerticalTransform;
import dev.ucdm.gcdm.GcdmConverter;
import dev.ucdm.gcdm.GcdmGridConverter;
import dev.ucdm.gcdm.protogen.GcdmServerProto;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Formatter;
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
import dev.ucdm.grid.api.*;

import static dev.ucdm.gcdm.GcdmConverter.encodeErrorMessage;

/**
 * Server that manages startup/shutdown of a gCDM Server.
 * Note that CdmDataset / GridDataset is opened/closed on each request.
 * TODO test performance w/wo caching
 */
public class GcdmServer {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(GcdmServer.class);
  private static final int MAX_MESSAGE = 50 * 1000 * 1000; // 50 Mb LOOK where is this set server or client ??

  private Server server;
  /* The port on which the server should run */
  int port = 16111;

  private void start() throws IOException {
    // no security
    server = ServerBuilder.forPort(port) //
            .addService(new GcdmImpl()) //
            // .intercept(new MyServerInterceptor())
            .build().start();
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      // Use stderr here since the logger may have been reset by its JVM shutdown hook.
      System.err.println("*** shutting down gRPC server since JVM is shutting down");
      try {
        GcdmServer.this.stop();
      } catch (InterruptedException e) {
        e.printStackTrace(System.err);
      }
      System.err.println("*** server shut down");
    }));
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

  ///////////////////////////////////////////////////////////////////////////////////////
  static class GcdmImpl extends GcdmImplBase {
    DataRoots roots = new DataRoots();

    @Override
    public void getCdmFile(CdmRequest req, StreamObserver<CdmResponse> responseObserver) {
      var response = CdmResponse.newBuilder().setLocation(req.getLocation());

      String dataPath = roots.convertRootToPath(req.getLocation());
      if (dataPath == null) {
        response.setError(encodeErrorMessage(String.format("No data root for '%s'", req.getLocation())));
      } else {
        System.out.printf("GcdmServer getCdmFile '%s' -> '%s'%n", req.getLocation(), dataPath);

        try (CdmFile ncfile = CdmDatasets.openFile(dataPath, null)) {
          GcdmProto.CdmFile.Builder cdmFile = GcdmProto.CdmFile.newBuilder().setLocation(req.getLocation())
                  .setRoot(GcdmConverter.encodeGroup(ncfile.getRootGroup(), 100).build());
          response.setCdmFile(cdmFile);
          logger.debug("GcdmServer getCdmFile " + req.getLocation());

        } catch (FileNotFoundException t) {
          response.setError(encodeErrorMessage(req.getLocation() + " (No such file or directory)"));

        } catch (Throwable t) {
          logger.warn("GcdmServer getCdmFile failed, returning an error", t);
          t.printStackTrace();
          response.setError(encodeErrorMessage(req.getLocation() + " Server error"));
        }
      }

      responseObserver.onNext(response.build());
      responseObserver.onCompleted();
    }

    @Override
    public void getCdmData(CdmDataRequest req, StreamObserver<CdmDataResponse> responseObserver) {
      System.out.printf("GcdmServer getData %s %s%n", req.getLocation(), req.getVariableSpec());

      var response = CdmDataResponse.newBuilder()
              .setLocation(req.getLocation()).setVariableSpec(req.getVariableSpec());

      String dataPath = roots.convertRootToPath(req.getLocation());
      if (dataPath == null) {
        response.setError(encodeErrorMessage(String.format("No data root for '%s'", req.getLocation())));
        responseObserver.onNext(response.build());
        responseObserver.onCompleted();
        return;

      }
      System.out.printf("GcdmServer getHeader open '%s' -> '%s'%n", req.getLocation(), dataPath);
      final Stopwatch stopwatch = Stopwatch.createStarted();
      long size;

      try (CdmFile ncfile = CdmDatasets.openFile(dataPath, null)) { // TODO cache ncfile?
        ParsedArraySectionSpec varSection = ParsedArraySectionSpec.parseVariableSection(ncfile, req.getVariableSpec());
        Variable var = varSection.getVariable();
        Section wantSection = varSection.getSection();
        size = var.getElementSize() * wantSection.computeSize();
        readCdmData(req, varSection, responseObserver);
        logger.debug("GcdmServer getData " + req.getLocation());
        System.out.printf(" ** size=%d took=%s%n", size, stopwatch.stop());

      } catch (FileNotFoundException t) {
        response.setError(encodeErrorMessage(req.getLocation() + " (No such file or directory)"));
        responseObserver.onNext(response.build());

      } catch (Throwable t) {
        logger.warn("GcdmServer getCdmData failed, returning an error", t);
        t.printStackTrace();
        response.setError(encodeErrorMessage(req.getLocation() + " Server error"));
        responseObserver.onNext(response.build());
      }
      responseObserver.onCompleted();
    }

    private void readCdmData(CdmDataRequest req, ParsedArraySectionSpec varSection,
                             StreamObserver<CdmDataResponse> responseObserver) throws IOException, InvalidRangeException {

      Variable var = varSection.getVariable();
      Section wantSection = varSection.getSection();
      long size = var.getElementSize() * wantSection.computeSize();
      if (size > MAX_MESSAGE) {
        readDataInChunks(req, varSection, responseObserver);
      } else {
        readOneChunk(req, varSection, responseObserver);
      }
    }

    private void readDataInChunks(CdmDataRequest req, ParsedArraySectionSpec varSection,
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
        readOneChunk(req, spec, responseObserver);
        index.setCurrentCounter(index.currentElement() + (int) Arrays.computeSize(chunkShape));
      }
    }

    private void readOneChunk(CdmDataRequest req, ParsedArraySectionSpec varSection,
                              StreamObserver<CdmDataResponse> responseObserver) throws IOException, InvalidRangeException {

      String spec = varSection.makeSectionSpecString();
      Variable var = varSection.getVariable();
      Section wantSection = varSection.getSection();

      CdmDataResponse.Builder response = CdmDataResponse.newBuilder().
              setLocation(req.getLocation())
              .setVariableSpec(spec)
              .setVarFullName(var.getFullName())
              .setSection(GcdmConverter.encodeSection(wantSection));

      Array<?> data = var.readArray(wantSection);
      response.setData(GcdmConverter.encodeData(data.getArrayType(), data));

      responseObserver.onNext(response.build());
      System.out.printf(" Send one chunk %s size=%d bytes%n", spec,
              data.length() * varSection.getVariable().getElementSize());
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////
    // GridDataset

    @Override
    public void getGridDataset(GcdmServerProto.GridDatasetRequest req,
                               io.grpc.stub.StreamObserver<GcdmServerProto.GridDatasetResponse> responseObserver) {
      GcdmServerProto.GridDatasetResponse.Builder response = GcdmServerProto.GridDatasetResponse.newBuilder().setLocation(req.getLocation());

      String dataPath = roots.convertRootToPath(req.getLocation());
      if (dataPath == null) {
        response.setError(encodeErrorMessage(String.format("No data root for '%s'", req.getLocation())));

      } else {
        System.out.printf("GcdmServer getGridDataset open %s%n", dataPath);
        Formatter errlog = new Formatter();
        try (GridDataset gridDataset = GridDatasetFactory.openGridDataset(dataPath, errlog)) {
          if (gridDataset == null) {
            response.setError(encodeErrorMessage(String.format("Dataset '%s' not found or not a GridDataset", req.getLocation())));
          } else {
            response.setGridDataset(GcdmGridConverter.encodeGridDataset(gridDataset, req.getLocation()));
          }
          logger.debug("GcdmServer getGridDataset " + req.getLocation());

        } catch (FileNotFoundException t) {
          response.setError(encodeErrorMessage(req.getLocation() + " (No such file or directory)"));

        } catch (Throwable t) {
          logger.warn("GcdmServer getGridDataset failed ", t);
          t.printStackTrace();
          response.setError(encodeErrorMessage(req.getLocation() + " Server error"));
        }
      }

      responseObserver.onNext(response.build());
      responseObserver.onCompleted();
    }

    @Override
    public void getGridData(GcdmServerProto.GridDataRequest req,
                            io.grpc.stub.StreamObserver<dev.ucdm.gcdm.protogen.GcdmServerProto.GridDataResponse> responseObserver) {
      var response = GcdmServerProto.GridDataResponse.newBuilder().setLocation(req.getLocation())
              .putAllSubset(req.getSubsetMap());

      String dataPath = roots.convertRootToPath(req.getLocation());
      if (dataPath == null) {
        response.setError(encodeErrorMessage(String.format("No data root for '%s'", req.getLocation())));
        responseObserver.onNext(response.build());
        responseObserver.onCompleted();
        return;
      }

      System.out.printf("GcdmServer getData %s %s == %s%n", req.getLocation(), req.getSubsetMap(), dataPath);
      final Stopwatch stopwatch = Stopwatch.createStarted();

      Formatter errlog = new Formatter();
      try (GridDataset gridDataset = GridDatasetFactory.openGridDataset(dataPath, errlog)) {
        if (gridDataset == null) {
          response.setError(encodeErrorMessage(String.format("'%s' not a Grid dataset", req.getLocation())));
        } else {
          GridSubset gridSubset = GridSubset.fromStringMap(req.getSubsetMap());
          String wantGridName = gridSubset.getGridName();
          Grid wantGrid = gridDataset.findGrid(wantGridName).orElse(null);
          if (gridSubset.getGridName() == null) {
            response.setError(encodeErrorMessage("GridName is not set"));

          } else if (wantGrid == null) {
            response.setError(encodeErrorMessage(
                    String.format("GridDataset '%s' does not have Grid '%s'", req.getLocation(), wantGridName)));

          } else {
            GridReferencedArray geoReferencedArray = wantGrid.readData(gridSubset);
            response.setData(GcdmGridConverter.encodeGridReferencedArray(geoReferencedArray));
            System.out.printf(" ** size=%d shape=%s%n", geoReferencedArray.data().length(),
                    java.util.Arrays.toString(geoReferencedArray.data().getShape()));
          }
        }

      } catch (FileNotFoundException t) {
        response.setError(encodeErrorMessage(req.getLocation() + " (No such file or directory)"));

      } catch (Throwable t) {
        logger.warn("GcdmServer getGridDataset failed ", t);
        t.printStackTrace();
        response.setError(encodeErrorMessage(req.getLocation() + " Server error"));
      }

      responseObserver.onNext(response.build());
      responseObserver.onCompleted();
      System.out.printf(" ** took=%s%n", stopwatch.stop());
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////
    // VerticalTransform

    @Override
    public void getVerticalTransform(GcdmServerProto.VerticalTransformRequest req,
                                     io.grpc.stub.StreamObserver<GcdmServerProto.VerticalTransformResponse> responseObserver) {
      GcdmServerProto.VerticalTransformResponse.Builder response = GcdmServerProto.VerticalTransformResponse.newBuilder()
              .setLocation(req.getLocation()).setVerticalTransform(req.getVerticalTransform())
              .setTimeIndex(req.getTimeIndex());

      String dataPath = roots.convertRootToPath(req.getLocation());
      if (dataPath == null) {
        response.setError(encodeErrorMessage(String.format("No data root for '%s'", req.getLocation())));

      } else {
        System.out.printf("GcdmServer getVerticalTransform open %s%n", req.getLocation());
        Formatter errlog = new Formatter();
        try (GridDataset gridDataset = GridDatasetFactory.openGridDataset(dataPath, errlog)) {
          if (gridDataset == null) {
            response.setError(encodeErrorMessage("Dataset not found or not a GridDataset"));
          } else if (req.getVerticalTransform().isEmpty()) {
            response.setError(encodeErrorMessage("The VerticalTransform name must be supplied"));
          } else {
            Optional<VerticalTransform> cto = gridDataset.findVerticalTransformByName(req.getVerticalTransform());
            if (cto.isPresent()) {
              Array<?> data = cto.get().getCoordinateArray3D(req.getTimeIndex());
              response.setData3D(GcdmConverter.encodeData(data.getArrayType(), data));
              logger.debug("GcdmServer getVerticalTransform " + req.getVerticalTransform());
            } else {
              response.setError(encodeErrorMessage("VerticalTransform '" + req.getVerticalTransform() + "' not found"));
            }
          }

        } catch (FileNotFoundException t) {
          response.setError(encodeErrorMessage(req.getLocation() + " (No such file or directory)"));

        } catch (Throwable t) {
          System.out.printf("GcdmServer getVerticalTransform failed %s %n%s%n", t.getMessage(), errlog);
          logger.warn("GcdmServer getVerticalTransform failed ", t);
          t.printStackTrace();
          response.setError(encodeErrorMessage(req.getLocation() + " Server error"));
        }
      }

      responseObserver.onNext(response.build());
      responseObserver.onCompleted();
    }

  } // GcdmImpl
}
