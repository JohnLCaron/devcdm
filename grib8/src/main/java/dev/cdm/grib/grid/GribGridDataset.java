/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package dev.cdm.grib.grid;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import dev.cdm.grib.collection.GribCdmIndex;
import dev.cdm.grib.collection.GribCollectionImmutable;
import dev.cdm.grib.collection.GribIosp;
import dev.cdm.grib.coord.Coordinate;
import dev.cdm.grib.coord.CoordinateTime2D;
import dev.cdm.grib.grib2.Grib2Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thredds.featurecollection.GribConfig;
import thredds.inventory.CollectionUpdateType;
import dev.cdm.array.InvalidRangeException;
import dev.cdm.core.api.AttributeContainer;
import dev.cdm.core.constants.AxisType;
import dev.cdm.core.constants.FeatureType;
import dev.cdm.grib.util.GdsHorizCoordSys;
import dev.cdm.grid.api.Grid;
import dev.cdm.grid.api.GridAxis;
import dev.cdm.grid.api.GridCoordinateSystem;
import dev.cdm.grid.api.GridDataset;
import dev.cdm.grid.api.GridHorizCoordinateSystem;
import dev.cdm.grid.api.GridTimeCoordinateSystem;
import dev.cdm.core.io.RandomAccessFile;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/** Grib implementation of {@link GridDataset} */
@Immutable
public class GribGridDataset implements GridDataset {
  private static final Logger logger = LoggerFactory.getLogger(GribGridDataset.class);

  /**
   * Open GribCollection as a GridDataset.
   *
   * @param errLog if is grib but error, add error message to this log.
   * @return empty if not a GribCollection or on error.
   */
  public static Optional<GribGridDataset> open(String endpoint, Formatter errLog) throws IOException {
    GribCollectionImmutable gc;

    if (endpoint.startsWith("file:")) {
      endpoint = endpoint.substring("file:".length());
    }

    // try to fail fast
    RandomAccessFile raf;
    try {
      raf = new RandomAccessFile(endpoint, "r");
      // TODO how do you pass in a non-standard GribConfig ? Or is that only when you are creating?
      gc = GribCdmIndex.openGribCollectionFromRaf(raf, new GribConfig(), CollectionUpdateType.nocheck,
          logger);

      if (gc == null) {
        raf.close();
        return Optional.empty();
      }

      /*
       * TODO here is the issue of multiple groups. How to handle? FeatureDatasetCoverage had baked in multiple
       * List<GribGridDataset> datasets = new ArrayList<>();
       * for (GribCollectionImmutable.Dataset ds : gc.getDatasets()) {
       * for (GribCollectionImmutable.GroupGC group : ds.getGroups()) {
       * GribGridDataset gribCov = new GribGridDataset(gc, ds, group);
       * datasets.add(gribCov);
       * }
       * }
       */
      GribGridDataset result = new GribGridDataset(gc, null, null);
      return Optional.of(result);

    } catch (IOException ioe) {
      throw ioe; // propagate up
    } catch (Throwable t) {
      logger.error("GribGridDataset.open failed", t);
      errLog.format("%s", t.getMessage());
      return Optional.empty();
    }
  }

  /////////////////////////////////////////////////////////////////////////////////////////

  private final GribCollectionImmutable gribCollection;
  private final GribCollectionImmutable.Dataset dataset;
  private final GribCollectionImmutable.GroupGC group;
  private final GridGribHorizHelper hhelper;
  private final Map<Integer, GridAxis<?>> gridAxes; // <index, GridAxis>
  private final ImmutableList<GridCoordinateSystem> gridCoordinateSystems;
  private final ImmutableList<GribGrid> grids;

  private final boolean isCurvilinearOrthogonal;

  GribGridDataset(GribCollectionImmutable gribCollection, @Nullable GribCollectionImmutable.Dataset dataset,
      @Nullable GribCollectionImmutable.GroupGC group) throws IOException, InvalidRangeException {
    Preconditions.checkNotNull(gribCollection);
    this.gribCollection = gribCollection;
    this.dataset = (dataset != null) ? dataset : gribCollection.getDataset(0);
    Preconditions.checkNotNull(this.dataset);
    this.group = (group != null) ? group : this.dataset.getGroup(0);
    Preconditions.checkNotNull(this.group);

    boolean isGrib1 = gribCollection.isGrib1;
    GribIosp iosp = gribCollection.getIosp();

    // A GribGridDataset has a unique GdsHorizCoordSys. When curvilinear, there may be multiple horizCS
    GdsHorizCoordSys hcs = this.group.getGdsHorizCoordSys();
    this.isCurvilinearOrthogonal =
        !isGrib1 && Grib2Utils.isCurvilinearOrthogonal(hcs.template, gribCollection.getCenter());
    this.hhelper = new GridGribHorizHelper(gribCollection, hcs, isCurvilinearOrthogonal, this.group.getVariables());

    Map<Integer, CoordAndAxis> coordIndexMap = new HashMap<>(); // <index, CoordAndAxis>
    Map<Integer, GribGridTimeCoordinateSystem> timeCsMap = new HashMap<>();
    Map<Integer, GridCoordinateSystem> csMap = new HashMap<>(); // hashCs, cs

    // Each Coordinate becomes a GridAxis
    this.gridAxes = new HashMap<>(); // <index, GridAxis>
    int coordIndex = 0;
    for (Coordinate coord : this.group.getCoordinates()) {
      CoordAndAxis coordAndAxis = GribGridAxis.create(this.dataset.getType(), coord, iosp);
      this.gridAxes.put(coordIndex, coordAndAxis.axis);
      coordIndexMap.put(coordIndex, coordAndAxis);
      coordIndex++;
    }

    // Each VariableIndex becomes a grid, except for curvilinear coordinates
    ArrayList<GribGrid> grids = new ArrayList<>();
    for (GribCollectionImmutable.VariableIndex vi : hhelper.getVariables()) {
      GridHorizCoordinateSystem horizCs = hhelper.getHorizCs(vi);
      GridCoordinateSystem ggcs =
          makeCoordinateSystem(vi.getCoordinateIndex(), coordIndexMap, timeCsMap, csMap, horizCs);
      grids.add(new GribGrid(iosp, this.gribCollection, ggcs, vi));
    }

    grids.sort((g1, g2) -> CharSequence.compare(g1.getName(), g2.getName()));
    this.grids = ImmutableList.copyOf(grids);
    this.gridCoordinateSystems = ImmutableList.copyOf(csMap.values());
  }

  private GridCoordinateSystem makeCoordinateSystem(Iterable<Integer> indices, Map<Integer, CoordAndAxis> coordIndexMap,
      Map<Integer, GribGridTimeCoordinateSystem> timeCsMap, Map<Integer, GridCoordinateSystem> csMap,
      GridHorizCoordinateSystem horizCs) {

    int hash = horizCs.hashCode() + makeHash(indices);
    return csMap.computeIfAbsent(hash, h -> {
      GribGridTimeCoordinateSystem tcs = makeTimeCoordinateSystem(indices, coordIndexMap, timeCsMap);
      List<GridAxis<?>> axes =
          new ArrayList<>(Streams.stream(indices).map(this.gridAxes::get).collect(Collectors.toList()));
      axes.add(hhelper.yaxis);
      axes.add(hhelper.xaxis);

      // remove RunTime axis if its an Observation Type
      if (tcs.getType() == GridTimeCoordinateSystem.Type.Observation) {
        axes = axes.stream().filter(a -> a.getAxisType() != AxisType.RunTime).collect(Collectors.toList());
      }

      // TODO need verticalTransform
      return new GridCoordinateSystem(axes, tcs, null, horizCs);
    });
  }

  private GribGridTimeCoordinateSystem makeTimeCoordinateSystem(Iterable<Integer> indices,
      Map<Integer, CoordAndAxis> coordIndexMap, Map<Integer, GribGridTimeCoordinateSystem> timeCsMap) {

    List<Integer> timeIndices = Streams.stream(indices).filter(index -> this.gridAxes.get(index).getAxisType().isTime())
        .collect(Collectors.toList());
    int hash = makeHash(timeIndices);
    List<CoordAndAxis> coordAndAxesList = Streams.stream(indices).map(coordIndexMap::get).collect(Collectors.toList());
    return timeCsMap.computeIfAbsent(hash,
        h -> GribGridTimeCoordinateSystem.create(dataset.getType(), coordAndAxesList));
  }

  private int makeHash(Iterable<Integer> indices) {
    Hasher hasher = Hashing.goodFastHash(32).newHasher();
    indices.forEach(hasher::putInt);
    return hasher.hash().asInt();
  }

  static class CoordAndAxis {
    Coordinate coord;
    GridAxis<?> axis;
    CoordinateTime2D time2d;

    CoordAndAxis(Coordinate coord, GridAxis<?> axis) {
      this.coord = coord;
      this.axis = axis;
    }

    CoordAndAxis withTime2d(CoordinateTime2D time2d) {
      this.time2d = time2d;
      return this;
    }
  }

  ////////////////////////////////////////////////////////////////////////

  @Override
  public void close() throws IOException {
    gribCollection.close();
  }

  @Override
  public String getName() {
    return gribCollection.getName();
  }

  @Override
  public String getLocation() {
    return gribCollection.getLocation() + "#" + group.getId();
  }

  @Override
  public AttributeContainer attributes() {
    return gribCollection.getGlobalAttributes();
  }

  @Override
  public FeatureType getFeatureType() {
    return isCurvilinearOrthogonal ? FeatureType.CURVILINEAR : FeatureType.GRID;
  }

  @Override
  public List<GridCoordinateSystem> getGridCoordinateSystems() {
    return this.gridCoordinateSystems;
  }

  @Override
  public List<GridAxis<?>> getGridAxes() {
    ImmutableList.Builder<GridAxis<?>> builder = ImmutableList.builder();
    builder.addAll(gridAxes.values());
    builder.addAll(hhelper.getHorizAxes()); // always has the same axes, CS may differ when curvilinear
    return builder.build();
  }

  @Override
  public List<Grid> getGrids() {
    return this.grids.stream().collect(ImmutableList.toImmutableList());
  }
}
