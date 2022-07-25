/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package dev.ucdm.grib.grib2.iosp;

import dev.ucdm.grib.common.GribTables;
import dev.ucdm.grib.common.wmo.WmoUtils;

import javax.annotation.Nonnull;
import dev.ucdm.array.Immutable;

/** A Grib-2 parameter from a table. */
public record Grib2Parameter(
      int discipline, int category, int number, String name, String unit, String abbrev, String description, Float fill, Float missing)
      implements GribTables.Parameter, Comparable<Grib2Parameter> {

  public Grib2Parameter(int discipline, int category, int number, String name, String unit, String abbrev, String desc) {
    this(discipline, category, number, name, unit, abbrev, desc, null, Float.NaN);
  }

  public Grib2Parameter(Grib2Parameter from, String name, String unit) {
    this(from.discipline, from.category, from.number, name.trim(), WmoUtils.cleanUnit(unit),
            from.abbrev, from.description, null, Float.NaN);
  }

  public String getId() {
    return discipline + "." + category + "." + number;
  }

  public int compareTo(@Nonnull Grib2Parameter o) {
    int c = discipline - o.discipline;
    if (c != 0)
      return c;
    c = category - o.category;
    if (c != 0)
      return c;
    return number - o.number;
  }

  @Override
  public int getDiscipline() {
    return discipline;
  }

  @Override
  public int getCategory() {
    return category;
  }

  @Override
  public int getNumber() {
    return number;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getUnit() {
    return unit;
  }

  @Override
  public String getAbbrev() {
    return abbrev;
  }

  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public Float getMissing() {
    return missing;
  }

  @Override
  public String getOperationalStatus() {
    return null;
  }

  @Override
  public Float getFill() {
    return fill;
  }

}

