/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.ucdm.grid.internal;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import dev.ucdm.array.ArrayType;
import dev.ucdm.array.Immutable;
import dev.ucdm.core.api.Group;
import dev.ucdm.core.constants.AxisType;
import dev.ucdm.dataset.api.CdmDatasetCS;
import dev.ucdm.dataset.api.CoordinateTransform;
import dev.ucdm.grid.api.GridDataset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import dev.ucdm.core.api.AttributeContainer;
import dev.ucdm.core.api.AttributeContainerMutable;
import dev.ucdm.core.api.Dimension;
import dev.ucdm.core.api.Dimensions;
import dev.ucdm.core.api.Variable;
import dev.ucdm.core.constants.FeatureType;
import dev.ucdm.dataset.api.CoordinateAxis;
import dev.ucdm.dataset.api.CoordinateSystem;
import dev.ucdm.dataset.api.CdmDataset;
import dev.ucdm.dataset.api.VariableDS;
import dev.ucdm.dataset.transform.vertical.VerticalTransform;
import dev.ucdm.dataset.transform.vertical.VerticalTransformFactory;
import dev.ucdm.grid.api.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Formatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** GridDataset implementation wrapping a CdmDataset. */
@Immutable
public class GridNetcdfDataset implements GridDataset {
  private static final Logger log = LoggerFactory.getLogger(GridNetcdfDataset.class);

  public static Optional<GridNetcdfDataset> create(CdmDatasetCS ncd, Formatter errInfo) throws IOException {
    DatasetClassifier classifier = new DatasetClassifier(ncd, errInfo);
    if (classifier.getFeatureType() == FeatureType.GRID || classifier.getFeatureType() == FeatureType.CURVILINEAR) {
      return createGridDataset(ncd, classifier, errInfo);
    } else {
      return Optional.empty();
    }
  }

  private static Optional<GridNetcdfDataset> createGridDataset(CdmDatasetCS ncd, DatasetClassifier classifier,
      Formatter errInfo) throws IOException {
    FeatureType featureType = classifier.getFeatureType();

    Map<String, GridAxis<?>> gridAxes = new HashMap<>();
    ArrayList<GridCoordinateSystem> coordsys = new ArrayList<>();
    Multimap<GridCoordinateSystem, Grid> gridsets = ArrayListMultimap.create();

    // Do all the independent axes first
    for (CoordinateAxis axis : classifier.getIndependentAxes()) {
      if (axis.getFullName().startsWith("Best/")) {
        continue;
      }
      if (axis.getRank() < 2) {
        GridAxis<?> gridAxis = CoordAxisToGridAxis
            .create(axis, GridAxisDependenceType.independent, ncd.isIndependentCoordinate(axis)).extractGridAxis();
        gridAxes.put(axis.getFullName(), gridAxis);
      } else {
        log.warn("Independent gridAxis {} rank > 1", axis.getFullName());
        errInfo.format("Independent gridAxis %s rank > 1", axis.getFullName());
      }
    }

    // Now we can do dependent, knowing we have all the independent ones in gridAxes
    for (CoordinateAxis axis : classifier.getDependentAxes()) {
      if (axis.getFullName().startsWith("Best/")) {
        continue;
      }
      if (axis.getRank() < 2) {
        GridAxis<?> gridAxis = CoordAxisToGridAxis
            .create(axis, GridAxisDependenceType.dependent, ncd.isIndependentCoordinate(axis)).extractGridAxis();
        gridAxes.put(axis.getFullName(), gridAxis);
      }
    }

    // vertical transforms
    VerticalTransformFinder finder = new VerticalTransformFinder(ncd, errInfo);
    Set<TrackVerticalTransform> verticalTransforms = finder.findVerticalTransforms();

    // Convert CoordinateSystem to GridCoordinateSystem
    Set<String> alreadyDone = new HashSet<>();
    Map<String, TrackGridCS> trackCsConverted = new HashMap<>();
    for (DatasetClassifier.CoordSysClassifier csc : classifier.getCoordinateSystemsUsed()) {
      if (csc.getName().startsWith("Best/")) {
        continue;
      }
      GridNetcdfCSBuilder.createFromClassifier(csc, gridAxes, verticalTransforms, errInfo).ifPresent(gcs -> {
        coordsys.add(gcs);
        trackCsConverted.put(csc.getName(), new TrackGridCS(csc, gcs));
      });
    }

    // Largest Coordinate Systems come first
    coordsys.sort((o1, o2) -> o2.getGridAxes().size() - o1.getGridAxes().size());

    // Assign coordsys to grids
    for (Variable v : ncd.getVariables()) {
      if (v.getFullName().startsWith("Best/")) { // TODO remove Best from grib generation code
        continue;
      }
      if (alreadyDone.contains(v.getFullName())) {
        continue;
      }
      VariableDS vds = (VariableDS) v;
      List<CoordinateSystem> css = new ArrayList<>(ncd.makeCoordinateSystemsFor(vds));
      if (css.isEmpty()) {
        continue;
      }
      // Use the largest (# axes) coordsys
      css.sort((o1, o2) -> o2.getCoordinateAxes().size() - o1.getCoordinateAxes().size());
      for (CoordinateSystem cs : css) {
        TrackGridCS track = trackCsConverted.get(cs.getName());
        if (track == null) {
          continue; // not used
        }
        GridCoordinateSystem gcs = track.gridCS;
        // Set<Dimension> domain = Dimensions.makeDomain(track.csc.getAxesUsed(), false);
        if (gcs != null && gcs.getFeatureType() == featureType && cs.isCoordinateSystemFor(v)) {
          Grid grid = new GridVariable(gcs, vds);
          gridsets.put(gcs, grid);
          alreadyDone.add(v.getFullName());
          break;
        }
      }
    }

    if (gridsets.isEmpty()) {
      errInfo.format("gridsets is empty%n");
      return Optional.empty();
    }

    HashSet<Grid> ugrids = new HashSet<>(gridsets.values());
    ArrayList<Grid> grids = new ArrayList<>(ugrids);
    grids.sort((g1, g2) -> CharSequence.compare(g1.getName(), g2.getName()));
    return Optional.of(new GridNetcdfDataset(ncd, featureType, coordsys, gridAxes.values(), grids));
  }

  private static class VerticalTransformFinder {
    final CdmDatasetCS ncd;
    final Formatter errlog;
    final Set<TrackVerticalTransform> result;

    VerticalTransformFinder(CdmDatasetCS ncd, Formatter errlog) {
      this.ncd = ncd;
      this.errlog = errlog;
      this.result = new HashSet<>();
    }

    Set<TrackVerticalTransform> findVerticalTransforms() {
      for (CoordinateSystem csys : ncd.getCoordinateSystems()) {
        CoordinateAxis vertAxis = csys.findAxis(AxisType.GeoZ); // ??
        CoordinateTransform ctv = csys.getVerticalTransform();
        if (ctv != null && vertAxis != null) {
          VerticalTransform vt = VerticalTransformFactory.makeTransform(ncd, csys, ctv);
          if (vt != null) {
            result.add(new TrackVerticalTransform(vertAxis.getFullName(), vt, csys));
          }
        }
      }
      return result;
    }
  }

  private static class TrackGridCS {
    final DatasetClassifier.CoordSysClassifier csc;
    final GridCoordinateSystem gridCS;

    public TrackGridCS(DatasetClassifier.CoordSysClassifier csc, GridCoordinateSystem gridCS) {
      this.csc = csc;
      this.gridCS = gridCS;
    }
  }

  static class TrackVerticalTransform {
    final String axisName;
    final VerticalTransform vertTransform;
    final CoordinateSystem csys;

    public TrackVerticalTransform(String axisName, VerticalTransform vertTransform, CoordinateSystem csys) {
      this.axisName = axisName;
      this.vertTransform = vertTransform;
      this.csys = csys;
    }

    boolean equals(String name, CoordinateSystem csys) {
      return this.axisName.equals(name) && this.csys.equals(csys);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o)
        return true;
      if (o == null || getClass() != o.getClass())
        return false;
      TrackVerticalTransform that = (TrackVerticalTransform) o;
      return vertTransform.getName().equals(that.vertTransform.getName()) && csys.equals(that.csys);
    }

    @Override
    public int hashCode() {
      return Objects.hash(vertTransform.getName(), csys);
    }
  }

  ///////////////////////////////////////////////////////////////////
  private final CdmDataset ncd;
  private final FeatureType featureType;

  private final ImmutableList<GridCoordinateSystem> coordsys;

  private final ImmutableList<GridAxis<?>> gridAxes;
  private final ImmutableList<Grid> grids;

  public GridNetcdfDataset(CdmDataset ncd, FeatureType featureType, List<GridCoordinateSystem> coordsys,
      Collection<GridAxis<?>> gridAxes, Collection<Grid> grids) {
    this.ncd = ncd;
    this.featureType = featureType;
    this.coordsys = ImmutableList.copyOf(coordsys);
    this.gridAxes = ImmutableList.copyOf(gridAxes);
    this.grids = ImmutableList.copyOf(grids);
  }

  @Override
  public String getName() {
    String loc = ncd.getLocation();
    int pos = loc.lastIndexOf('/');
    if (pos < 0)
      pos = loc.lastIndexOf('\\');
    return (pos < 0) ? loc : loc.substring(pos + 1);
  }

  @Override
  public String getLocation() {
    return ncd.getLocation();
  }

  @Override
  public AttributeContainer attributes() {
    return AttributeContainerMutable.copyFrom(ncd.getRootGroup().attributes()).setName(getName()).toImmutable();
  }

  @Override
  public List<GridCoordinateSystem> getGridCoordinateSystems() {
    return ImmutableList.copyOf(coordsys);
  }

  @Override
  public List<GridAxis<?>> getGridAxes() {
    return gridAxes;
  }

  @Override
  public List<Grid> getGrids() {
    return ImmutableList.copyOf(grids);
  }

  @Override
  public FeatureType getFeatureType() {
    return featureType;
  }

  @Override
  public String toString() {
    Formatter f = new Formatter();
    toString(f);
    return f.toString();
  }

  private boolean wasClosed = false;

  @Override
  public synchronized void close() throws IOException {
    try {
      if (!wasClosed)
        ncd.close();
    } finally {
      wasClosed = true;
    }
  }
}
