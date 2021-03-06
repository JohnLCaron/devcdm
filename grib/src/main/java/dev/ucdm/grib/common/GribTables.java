/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package dev.ucdm.grib.common;

import dev.ucdm.grib.coord.VertCoordUnit;

import org.jetbrains.annotations.Nullable;

/**
 * Abstraction of GribTable for Grib Collections.
 * Allows Grib1 and Grib2 to be handled through common interface.
 */
public interface GribTables {

  @Nullable
  String getSubCenterName(int center, int subcenter);

  String getLevelNameShort(int code);

  @Nullable
  GribStatType getStatType(int intvType);

  VertCoordUnit getVertUnit(int code);

  @Nullable
  String getGeneratingProcessName(int code);

  @Nullable
  String getGeneratingProcessTypeName(int code);

  interface Parameter {
    /** Unsigned byte */
    int getDiscipline();

    /** Unsigned byte */
    int getCategory();

    /** Unsigned byte */
    int getNumber();

    String getName();

    String getUnit();

    @Nullable
    String getAbbrev();

    String getDescription();

    /** Unique across all Parameter tables */
    String getId();

    @Nullable
    Float getFill();

    Float getMissing();

    @Nullable
    String getOperationalStatus();
  }

}
