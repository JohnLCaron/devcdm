/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package dev.ucdm.grid.internal;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import dev.ucdm.array.Array;
import dev.ucdm.array.ArrayType;
import dev.ucdm.array.Arrays;
import dev.ucdm.array.InvalidRangeException;
import dev.ucdm.array.Range;
import dev.ucdm.array.RangeComposite;
import dev.ucdm.core.constants.AxisType;
import dev.ucdm.grid.api.*;
import dev.ucdm.dataset.geoloc.LatLonPoints;
import dev.ucdm.dataset.geoloc.ProjectionRect;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import java.util.Optional;

/** longitude axis that crosses the seam */
public class CylindricalCoord {

  // a longitude axis that covers the globe
  public static boolean isFullCylindrical(GridHorizCoordinateSystem hcs) {
    GridAxisPoint xaxis = hcs.getXHorizAxis();
    int n = xaxis.getNominalSize();
    double width = xaxis.getCoordInterval(n - 1).end() - xaxis.getCoordInterval(0).start();
    return (xaxis.getAxisType() == AxisType.Lon && width >= 360);
  }

  private final GridAxisPoint xaxis; // original xaxis
  private final double start;
  private final double end;
  private List<Range> saveIntvs;

  public CylindricalCoord(GridHorizCoordinateSystem hcs) {
    // Preconditions.checkArgument(isCylindrical(hcs));
    this.xaxis = hcs.getXHorizAxis();
    this.start = xaxis.getCoordInterval(0).start();
    this.end = xaxis.getCoordInterval(xaxis.getNominalSize() - 1).end();
  }

  public boolean needsSpecialRead() {
    return (this.saveIntvs != null);
  }

  public Optional<GridAxisPoint> subsetLon(ProjectionRect projbb, int horizStride, Formatter errlog) {
    double wantMin = LatLonPoints.lonNormalFrom(projbb.getMinX(), this.start);
    double wantMax = LatLonPoints.lonNormalFrom(projbb.getMaxX(), this.start);

    // may be 0, 1, or 2 CoordIntervals
    // List<Range> lonIntvs = subsetLonIntervals(wantMin, wantMax, horizStride);
    List<CoordInterval> coordIntvs = subsetLonIntervals(wantMin, wantMax, this.start, this.end);

    if (coordIntvs.isEmpty()) {
      errlog.format("longitude want [%f,%f] does not intersect lon axis [%f,%f]", wantMin, wantMax, start, end);
      return Optional.empty();
    }

    if (coordIntvs.size() == 1) {
      Range lonIntv = intvFromCoords(coordIntvs.get(0));
      GridAxisPoint.Builder<?> builder = xaxis.toBuilder().subsetWithRange(lonIntv);
      return Optional.of(builder.build());
    }

    Range lonIntv0 = intvFromCoords(coordIntvs.get(0));
    Range lonIntv1 = intvFromCoords(coordIntvs.get(1));
    this.saveIntvs = List.of(lonIntv0, lonIntv1);

    GridAxisPoint xaxis0 = xaxis.toBuilder().subsetWithRange(lonIntv0).build();
    GridAxisPoint xaxis1 = xaxis.toBuilder().subsetWithRange(lonIntv1).build();
    int ncoords = xaxis0.getNominalSize() + xaxis1.getNominalSize();
    double[] coords = new double[ncoords];
    int count = 0;
    for (Number c : xaxis0) {
      coords[count++] = c.doubleValue();
    }
    double last = coords[count-1];
    double offset = (last < xaxis1.getCoordDouble(0)) ? 0 : 360;
    for (Number c : xaxis1) {
      coords[count++] = c.doubleValue() + offset;
    }
    RangeComposite union = new RangeComposite(xaxis.getName(), List.of(xaxis0.getSubsetRange(), xaxis1.getSubsetRange()));

    GridAxisPoint.Builder<?> builder = xaxis.toBuilder();
    builder.setValues(coords).setIsSubset(true).setRangeIterator(union).setSpacing(GridAxisSpacing.irregularPoint);

    return Optional.of(builder.build());
  }

  private Range intvFromCoords(CoordInterval coordIntv) {
    int min = SubsetHelpers.findCoordElement(xaxis, coordIntv.start(), true);
    int max = SubsetHelpers.findCoordElement(xaxis, coordIntv.end(), true);
    Preconditions.checkArgument(min <= max);
    return Range.make(min, max);
  }

  public Array<Number> readSpecial(MaterializedCoordinateSystem subsetCoordSys, Grid grid)
      throws InvalidRangeException, IOException {
    ArrayList<Range> ranges = new ArrayList<>(subsetCoordSys.getSubsetRanges());
    int last = ranges.size() - 1;
    ranges.set(last, this.saveIntvs.get(0));
    Array<Number> data0 = grid.readDataSection(new dev.ucdm.array.Section(ranges));
    ranges.set(last, this.saveIntvs.get(1));
    Array<Number> data1 = grid.readDataSection(new dev.ucdm.array.Section(ranges));

    int[] shape0 = data0.getShape();
    int[] shape1 = data1.getShape();
    int part0 = (int) data0.length() / shape0[last];
    int part1 = (int) data1.length() / shape1[last];
    int xlen = shape0[last] + shape1[last];

    int[] reshape0 = new int[] {part0, shape0[last]};
    int[] reshape1 = new int[] {part1, shape1[last]};
    Array<Number> redata0 = Arrays.reshape(data0, reshape0);
    Array<Number> redata1 = Arrays.reshape(data1, reshape1);

    double[] values = new double[(int) (data0.length() + data1.length())];
    for (int j = 0; j < part0; j++) {
      for (int i = 0; i < shape0[last]; i++) {
        values[j * xlen + i] = redata0.get(j, i).doubleValue();
      }
    }
    for (int j = 0; j < part1; j++) {
      for (int i = 0; i < shape1[last]; i++) {
        values[j * xlen + shape0[last] + i] = redata1.get(j, i).doubleValue();
      }
    }

    int[] shapeAll = java.util.Arrays.copyOf(shape0, shape0.length);
    shapeAll[last] = xlen;
    return Arrays.factory(ArrayType.DOUBLE, shapeAll, values);
  }

  /*
   * This is the more general case, not needed here because we assume that we have a complete longitude axis.
   * Subset longitude intervals, after normalizing to start.
   * draw a circle, representing longitude values from start to start + 360.
   * all values are on this circle and are > start.
   * put start at bottom of circle, end > start, data has values from start, counterclockwise to end.
   * wantMin, wantMax can be anywhere, want goes from wantMin counterclockwise to wantMax.
   * wantMin may be less than or greater than wantMax.
   *
   * cases:
   * A. wantMin < wantMax
   * 1 wantMin > end : empty
   * 2 wantMin, wantMax <= end : [wantMin, wantMax]
   * 3 wantMin < end, wantMax > end : [wantMin, end]
   *
   * wantMin > wantMax
   * B. wantMax > end : [start, end]
   * C. wantMin > end : [start, wantMax]
   * D. wantMin < end : 2 pieces: [wantMin, end] + [start, wantMax]
   *
   * use CoordInterval to hold a real valued interval, min < max
   */
  private List<CoordInterval> subsetLonIntervals(double wantMin, double wantMax, double start, double end) {
    if (wantMin <= wantMax) {
      if (wantMin > end) { // none A.1
        return ImmutableList.of();
      }

      if (wantMin < end && wantMax <= end) {// A.2
        return ImmutableList.of(new CoordInterval(wantMin, wantMax));
      }

      if (wantMin < end && wantMax > end) { // A.3
        return ImmutableList.of(new CoordInterval(wantMin, end));
      }

    } else {
      if (wantMax > end) { // B
        return ImmutableList.of(new CoordInterval(start, end));
      } else if (wantMin > end) { // C
        return ImmutableList.of(new CoordInterval(start, wantMax));
      } else if (wantMin <= end) { // D
        return ImmutableList.of(new CoordInterval(wantMin, end), new CoordInterval(start, wantMax));
      }
    }

    // otherwise no intersection
    return ImmutableList.of();
  }

}
