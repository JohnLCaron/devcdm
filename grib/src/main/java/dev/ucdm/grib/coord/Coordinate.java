/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package dev.ucdm.grib.coord;

import dev.ucdm.array.Indent;
import dev.ucdm.core.constants.AxisType;
import dev.ucdm.core.util.Counters;

import java.util.Formatter;
import java.util.List;

/**
 * Abstract coordinate
 */
public interface Coordinate {
  /** Coordinate types */
  enum Type {
    runtime(0), //
    time(1), //
    timeIntv(1), //
    vert(3), //
    time2D(1), //
    ens(2);

    public final int order;

    Type(int order) {
      this.order = order;
    }
  }

  List<?> getValues(); // get sorted list of values

  Object getValue(int idx); // get the ith value

  int getIndex(Object val); // Assumes the values are unique;

  int getSize(); // how many values ??

  int getCode();

  Type getType();

  String getName();

  String getUnit();

  int getNCoords(); // how many coords ??

  void showInfo(Formatter info, Indent indent);

  void showCoords(Formatter info);

  Counters calcDistributions();

  int estMemorySize(); // estimated memory size in bytes (debugging)
}
