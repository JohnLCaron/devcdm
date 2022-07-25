package dev.ucdm.grib.coord;

import org.jetbrains.annotations.Nullable;

/** Encapsulate the semantics in GRIB level types (Grib1 table 3, Grib2 code table 4.5). */
public record VertCoordUnit(int code,
                            @Nullable String description, @Nullable String abbrev, @Nullable String units, @Nullable String datum,
                            boolean isPositiveUp, boolean isLayer) {

  public VertCoordUnit(int code, String units, String datum, boolean isPositiveUp) {
    this(code, null, null, units, datum, isPositiveUp, false);
  }

  public boolean isVerticalCoordinate() {
    return units != null;
  }
}

