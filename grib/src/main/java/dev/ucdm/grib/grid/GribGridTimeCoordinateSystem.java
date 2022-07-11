/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package dev.ucdm.grib.grid;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import dev.ucdm.grib.collection.CollectionType;
import dev.ucdm.grib.coord.Coordinate;
import dev.ucdm.grib.coord.CoordinateRuntime;
import dev.ucdm.grib.coord.CoordinateTime2D;
import dev.ucdm.grib.coord.CoordinateTimeAbstract;
import dev.cdm.array.Range;
import dev.cdm.core.calendar.CalendarDate;
import dev.cdm.core.calendar.CalendarDateUnit;
import dev.cdm.core.calendar.CalendarPeriod;
import dev.cdm.grid.api.GridAxisInterval;
import dev.cdm.grid.api.GridSubset;
import dev.cdm.grid.api.GridAxis;
import dev.cdm.grid.api.GridAxisPoint;
import dev.cdm.grid.api.GridTimeCoordinateSystem;
import dev.cdm.array.Indent;
import dev.cdm.grid.internal.SubsetTimeHelper;

import org.jetbrains.annotations.Nullable;
import dev.cdm.array.Immutable;
import java.util.Formatter;
import java.util.List;
import java.util.Optional;

/**
 * Grib implementation of {@link GridTimeCoordinateSystem}
 * Is it possible that much of this is general?
 */
@Immutable
public abstract class GribGridTimeCoordinateSystem extends GridTimeCoordinateSystem {

  public static GribGridTimeCoordinateSystem create(CollectionType type,
                                                    List<GribGridDataset.CoordAndAxis> coordAndAxes) {

    GribGridDataset.CoordAndAxis runtime =
        coordAndAxes.stream().filter(ca -> ca.coord.getType() == Coordinate.Type.runtime).findFirst().orElse(null);
    GribGridDataset.CoordAndAxis time =
        coordAndAxes.stream().filter(ca -> ca.coord.getType() == Coordinate.Type.time).findFirst().orElse(null);
    if (time == null) {
      time =
          coordAndAxes.stream().filter(ca -> ca.coord.getType() == Coordinate.Type.timeIntv).findFirst().orElse(null);
    }
    Preconditions.checkNotNull(runtime);
    Preconditions.checkArgument(runtime.axis instanceof GridAxisPoint);
    Preconditions.checkNotNull(time);
    Preconditions.checkNotNull(time.time2d);

    switch (type) {
      case SRC -> {
        // time2D (1 X ntimes) orthogonal:
        Preconditions.checkArgument(runtime.coord.getClass().isAssignableFrom(CoordinateRuntime.class));
        Preconditions.checkArgument(runtime.coord.getNCoords() == 1);
        return new SingleRuntime(runtime, time);
      }
      case MRUTC, MRUTP -> {
        // time2D (nruns X 1) orthogonal:
        Preconditions.checkNotNull(time);
        CalendarDate refDate = time.time2d.getRefDate();
        CalendarPeriod period = time.time2d.getTimeUnit();
        CalendarDateUnit dateUnit = CalendarDateUnit.of(period, true, refDate);
        return new Observation(time, dateUnit);
      }
      case MRC, TwoD -> {
        if (time.time2d.isOrthogonal()) {
          return new Offset(type, runtime, time);
        } else if (time.time2d.isRegular()) {
          return new OffsetRegular(type, runtime, time);
        } else {
          return new OffsetIrregular(type, runtime, time);
        }
      }
      default -> throw new IllegalStateException("Type=" + type);
    }
  }

  //////////////////////////////////////////////////////////////////////////////
  GribGridTimeCoordinateSystem(Type type, @Nullable GridAxisPoint runTimeAxis, GridAxis<?> timeOffsetAxis,
      CalendarDateUnit runtimeDateUnit) {
    super(type, runTimeAxis, timeOffsetAxis, runtimeDateUnit);
  }

  // TODO so what is special about GribGridTimeCoordinateSystem ??
  // TODO should this be MaterializedTimeCoordinateSystem ?
  // it would appear the only thing needed out of it (besides geo referencing) is getSubsetRanges()
  // TODO if it has a runtime, does it have a dimension of length 1?
  // are you allowed to subset across runtimes?
  public abstract Optional<GribGridTimeCoordinateSystem> subset(GridSubset params, Formatter errlog);

  //////////////////////////////////////////////////////////////////////////////
  private static class Observation extends GribGridTimeCoordinateSystem {

    private Observation(GribGridDataset.CoordAndAxis time, CalendarDateUnit runtimeDateUnit) {
      // TODO MRMS_Radar_20201027_0000.grib2.ncx4 time2D has runtime in seconds, but period name is minutes
      super(Type.Observation, null, time.axis, runtimeDateUnit);
    }

    private Observation(GridAxis<?> timeOffset, CalendarDateUnit dateUnit) {
      super(Type.Observation, null, timeOffset, dateUnit);
    }

    @Override
    public List<Integer> getNominalShape() {
      return ImmutableList.of(timeOffsetAxis.getNominalSize());
    }

    @Override
    public Optional<GribGridTimeCoordinateSystem> subset(GridSubset params, Formatter errlog) {
      SubsetTimeHelper helper = new SubsetTimeHelper(this);
      return helper.subsetTime(params, errlog).map(t -> new Observation(t, this.runtimeDateUnit));
    }
  }

  //////////////////////////////////////////////////////////////////////////////
  private static class SingleRuntime extends GribGridTimeCoordinateSystem {
    private final CalendarDate runtimeDate;

    private SingleRuntime(GribGridDataset.CoordAndAxis runtime, GribGridDataset.CoordAndAxis timeOffset) {
      super(Type.SingleRuntime, (GridAxisPoint) runtime.axis, timeOffset.axis, null);
      Preconditions.checkArgument(runtime.coord.getNCoords() == 1);
      CoordinateRuntime runtimeCoord = (CoordinateRuntime) runtime.coord;
      this.runtimeDate = runtimeCoord.getFirstDate();
    }

    private SingleRuntime(GridAxisPoint runtime, GridAxis<?> timeOffset, CalendarDate runtimeDate) {
      super(Type.SingleRuntime, runtime, timeOffset, null);
      this.runtimeDate = runtimeDate;
    }

    @Override
    public CalendarDate getBaseDate() {
      return runtimeDate;
    }

    @Override
    public List<Integer> getNominalShape() {
      return ImmutableList.of(1, timeOffsetAxis.getNominalSize());
    }

    @Override
    public CalendarDate getRuntimeDate(int idx) {
      return runtimeDate;
    }

    @Override
    public Optional<GribGridTimeCoordinateSystem> subset(GridSubset params, Formatter errlog) {
      SubsetTimeHelper helper = new SubsetTimeHelper(this);
      return helper.subsetTime(params, errlog).map(t -> new SingleRuntime(runTimeAxis, t, this.runtimeDate));
    }

    @Override
    public String toString() {
      return super.toString() + "\nSingleRuntime{" + "runtimeDate=" + runtimeDate + "}";
    }
  }

  //////////////////////////////////////////////////////////////////////////////
  private static class Offset extends GribGridTimeCoordinateSystem {
    private final CoordinateTime2D coord2D;
    private final CollectionType gribType;

    private Offset(CollectionType gribType, GribGridDataset.CoordAndAxis runtime,
        GribGridDataset.CoordAndAxis time) {
      super(Type.Offset, (GridAxisPoint) runtime.axis, time.axis, null);
      this.coord2D = time.time2d;
      this.gribType = gribType;
    }

    private Offset(CollectionType gribType, CoordinateTime2D coord2D, GridAxisPoint runtime,
        GridAxis<?> timeOffset) {
      super(Type.Offset, runtime, timeOffset, null);
      this.coord2D = coord2D;
      this.gribType = gribType;
    }

    @Override
    public CalendarDate getBaseDate() {
      return getRuntimeDate(0);
    }

    @Override
    public List<Integer> getNominalShape() {
      return ImmutableList.of(coord2D.getNruns(), coord2D.getNtimes());
    }

    @Override
    public Optional<GribGridTimeCoordinateSystem> subset(GridSubset params, Formatter errlog) {
      SubsetTimeHelper helper = new SubsetTimeHelper(this);
      return helper.subsetOffset(params, errlog)
          .map(t -> new Offset(this.gribType, this.coord2D, helper.runtimeAxis, t));
    }

    @Override
    public String toString() {
      return "Offset{" + "coord2D=" + coord2D + ", gribType=" + gribType + "} " + super.toString();
    }
  }

  //////////////////////////////////////////////////////////////////////////////
  private static class OffsetRegular extends GribGridTimeCoordinateSystem {
    private final CoordinateTime2D coord2D;
    private final CollectionType gribType;

    private OffsetRegular(CollectionType gribType, GribGridDataset.CoordAndAxis runtime,
        GribGridDataset.CoordAndAxis time) {
      super(Type.OffsetRegular, (GridAxisPoint) runtime.axis, time.axis, null);
      this.coord2D = time.time2d;
      this.gribType = gribType;
    }

    private OffsetRegular(CollectionType gribType, CoordinateTime2D coord2D, GridAxisPoint runtime,
        GridAxis<?> timeOffset) {
      super(Type.OffsetRegular, runtime, timeOffset, null);
      this.coord2D = coord2D;
      this.gribType = gribType;
    }

    @Override
    public CalendarDate getBaseDate() {
      return getRuntimeDate(0);
    }

    @Override
    public List<Integer> getNominalShape() {
      return ImmutableList.of(coord2D.getNruns(), coord2D.getNtimes());
    }

    @Override
    public GridAxis<?> getTimeOffsetAxis(int runIdx) {
      CoordinateTimeAbstract timeCoord = coord2D.getTimeCoordinate(runIdx);
      return GribGridAxis.create(gribType, timeCoord, null).axis;
    }

    @Override
    public Optional<GribGridTimeCoordinateSystem> subset(GridSubset params, Formatter errlog) {
      SubsetTimeHelper helper = new SubsetTimeHelper(this);
      return helper.subsetOffset(params, errlog)
          .map(t -> new OffsetRegular(this.gribType, this.coord2D, helper.runtimeAxis, t));
    }

    @Override
    public String toString() {
      Formatter f = new Formatter();
      f.format("%s%n", super.toString());
      f.format("OffsetRegular gribType= %s ", gribType);
      coord2D.showInfo(f, new Indent(2));
      return f.toString();
    }
  }

  //////////////////////////////////////////////////////////////////////////////
  private static class OffsetIrregular extends GribGridTimeCoordinateSystem {
    private final CoordinateTime2D coord2D;
    private final CollectionType gribType;

    private OffsetIrregular(CollectionType gribType, GribGridDataset.CoordAndAxis runtime,
        GribGridDataset.CoordAndAxis time) {
      super(Type.OffsetIrregular, (GridAxisPoint) runtime.axis, time.axis, null);
      this.coord2D = time.time2d;
      this.gribType = gribType;
    }

    private OffsetIrregular(CollectionType gribType, CoordinateTime2D coord2D, GridAxisPoint runtime,
        GridAxis<?> timeOffset) {
      super(Type.OffsetRegular, runtime, timeOffset, null);
      this.coord2D = coord2D;
      this.gribType = gribType;
    }

    @Override
    public CalendarDate getBaseDate() {
      return getRuntimeDate(0);
    }

    @Override
    public List<Integer> getNominalShape() {
      return ImmutableList.of(coord2D.getNruns(), coord2D.getNtimes());
    }

    @Override
    public List<Range> getSubsetRanges() {
      return ImmutableList.of(new Range(coord2D.getNruns()), new Range(coord2D.getNtimes()));
    }

    @Override
    public GridAxis<?> getTimeOffsetAxis(int runIdx) {
      CoordinateTimeAbstract timeCoord = coord2D.getTimeCoordinate(runIdx);
      GridAxis<?> axis = GribGridAxis.create(gribType, timeCoord, null).axis;
      if (timeCoord.getTimeUnit() != this.offsetPeriod) {
        double factor = this.offsetPeriod.getConvertFactor(timeCoord.getTimeUnit());
        if (axis instanceof GridAxisPoint) {
          return ((GridAxisPoint) axis).toBuilder().setUnits(offsetPeriod.toString()).scaleValues(factor).build();
        } else {
          return ((GridAxisInterval) axis).toBuilder().setUnits(offsetPeriod.toString()).scaleValues(factor).build();
        }
      }
      return axis;
    }

    @Override
    public Optional<GribGridTimeCoordinateSystem> subset(GridSubset params, Formatter errlog) {
      SubsetTimeHelper helper = new SubsetTimeHelper(this);
      return helper.subsetOffset(params, errlog)
          .map(t -> new OffsetIrregular(this.gribType, this.coord2D, helper.runtimeAxis, t));
    }

    @Override
    public String toString() {
      return "OffsetIrregular{" + "coord2D=" + coord2D + ", gribType=" + gribType + "} " + super.toString();
    }
  }

}
