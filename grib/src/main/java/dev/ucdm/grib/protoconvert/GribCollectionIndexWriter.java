/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package dev.ucdm.grib.protoconvert;

import com.google.protobuf.ByteString;
import dev.ucdm.grib.inventory.MCollection;
import dev.ucdm.grib.coord.*;
import dev.ucdm.grib.protogen.GribCollectionProto;

/** Common superclass for writing Grib Collection index (ncx) files */
public class GribCollectionIndexWriter {
  public static final String PARTITION2_START = "Grib2Partition2Index"; // was Grib2Partition0Index
  public static final String PARTITION1_START = "Grib1Partition2Index"; // was Grib1Partition0Index

  static final int currentVersion = 1;

  protected final MCollection dcm;

  public GribCollectionIndexWriter(MCollection dcm) {
    this.dcm = dcm;
  }

  static GribCollectionProto.Gds publishGdsProto(byte[] rawGds, int predefinedGridDefinition) {
    GribCollectionProto.Gds.Builder b = GribCollectionProto.Gds.newBuilder();

    if (predefinedGridDefinition >= 0)
      b.setPredefinedGridDefinition(predefinedGridDefinition);
    else {
      b.setGds(ByteString.copyFrom(rawGds));
    }

    return b.build();
  }

  GribCollectionProto.Coord publishCoordinateRuntime(CoordinateRuntime coord) {
    GribCollectionProto.Coord.Builder b = GribCollectionProto.Coord.newBuilder();
    b.setAxisType(convertAxisType(coord.getType()));
    b.setCode(coord.getCode());
    if (coord.getUnit() != null)
      b.setUnit(coord.getUnit());

    for (int idx = 0; idx < coord.getSize(); idx++) {
      long runtime = coord.getRuntime(idx);
      b.addMsecs(runtime);
    }
    return b.build();
  }

  GribCollectionProto.Coord publishCoordinateTime(CoordinateTime coord) {
    GribCollectionProto.Coord.Builder b = GribCollectionProto.Coord.newBuilder();
    b.setAxisType(convertAxisType(coord.getType()));
    b.setCode(coord.getCode());
    b.setUnit(coord.getTimeUnit().toString());
    b.addMsecs(coord.getRefDate().getMillisFromEpoch());
    for (Long offset : coord.getOffsetSorted()) {
      b.addValues(offset);
    }

    int[] time2runtime = coord.getTime2runtime();
    if (time2runtime != null) {
      for (int val : time2runtime) {
        b.addTime2Runtime(val);
      }
    }

    return b.build();
  }

  GribCollectionProto.Coord publishCoordinateTimeIntv(CoordinateTimeIntv coord) {
    GribCollectionProto.Coord.Builder b = GribCollectionProto.Coord.newBuilder();
    b.setAxisType(convertAxisType(coord.getType()));
    b.setCode(coord.getCode());
    b.setUnit(coord.getTimeUnit().toString());
    b.addMsecs(coord.getRefDate().getMillisFromEpoch());

    for (TimeCoordIntvValue tinv : coord.getTimeIntervals()) {
      b.addValues(tinv.getBounds1());
      b.addBound(tinv.getBounds2());
    }

    int[] time2runtime = coord.getTime2runtime();
    if (time2runtime != null)
      for (int val : time2runtime)
        b.addTime2Runtime(val);

    return b.build();
  }

  GribCollectionProto.Coord publishCoordinateVert(CoordinateVert coord) {
    GribCollectionProto.Coord.Builder b = GribCollectionProto.Coord.newBuilder();
    b.setAxisType(convertAxisType(coord.getType()));
    b.setCode(coord.getCode());

    if (coord.getUnit() != null)
      b.setUnit(coord.getUnit());
    for (VertCoordValue level : coord.getLevelSorted()) {
      if (coord.isLayer()) {
        b.addValues((float) level.getValue1());
        b.addBound((float) level.getValue2());
      } else {
        b.addValues((float) level.getValue1());
      }
    }
    return b.build();
  }

  GribCollectionProto.Coord publishCoordinateEns(CoordinateEns coord) {
    GribCollectionProto.Coord.Builder b = GribCollectionProto.Coord.newBuilder();
    b.setAxisType(convertAxisType(coord.getType()));
    b.setCode(coord.getCode());

    if (coord.getUnit() != null)
      b.setUnit(coord.getUnit());
    for (EnsCoordValue level : coord.getEnsSorted()) {
      b.addValues((float) level.getCode()); // lame
      b.addBound((float) level.getEnsMember());
    }
    return b.build();
  }

  GribCollectionProto.Coord publishCoordinateTime2D(CoordinateTime2D coord) {
    GribCollectionProto.Coord.Builder b = GribCollectionProto.Coord.newBuilder();
    b.setAxisType(convertAxisType(coord.getType()));
    b.setCode(coord.getCode());
    b.setUnit(coord.getTimeUnit().toString());
    CoordinateRuntime runtimeCoord = coord.getRuntimeCoordinate();
    for (int idx = 0; idx < runtimeCoord.getSize(); idx++) {
      long runtime = runtimeCoord.getRuntime(idx);
      b.addMsecs(runtime);
    }

    b.setIsOrthogonal(coord.isOrthogonal());
    b.setIsRegular(coord.isRegular());
    for (Coordinate time : coord.getTimesForSerialization()) {
      if (time.getType() == Coordinate.Type.time)
        b.addTimes(publishCoordinateTime((CoordinateTime) time));
      else
        b.addTimes(publishCoordinateTimeIntv((CoordinateTimeIntv) time));
    }

    int[] time2runtime = coord.getTime2runtime();
    if (time2runtime != null)
      for (int val : time2runtime)
        b.addTime2Runtime(val);

    return b.build();
  }

  private static GribCollectionProto.GribAxisType convertAxisType(Coordinate.Type type) {
    return switch (type) {
      case runtime -> GribCollectionProto.GribAxisType.runtime;
      case time -> GribCollectionProto.GribAxisType.time;
      case time2D -> GribCollectionProto.GribAxisType.time2D;
      case timeIntv -> GribCollectionProto.GribAxisType.timeIntv;
      case ens -> GribCollectionProto.GribAxisType.ens;
      case vert -> GribCollectionProto.GribAxisType.vert;
    };
  }

}
