/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package dev.ucdm.grib.grib1.iosp;

import dev.ucdm.core.util.StringUtil2;
import dev.ucdm.grib.common.GribTables;
import dev.ucdm.grib.common.util.GribUtils;
import dev.ucdm.grib.grib1.table.Grib1ParamTableReader;

import dev.ucdm.array.Immutable;
import org.jetbrains.annotations.Nullable;

/** A Grib-1 Parameter from a Grib table. */
@Immutable
public record Grib1Parameter(Grib1ParamTableReader table, int number, String name, String description, String unit,
                             @Nullable String cfName)
        implements GribTables.Parameter {

  public Grib1Parameter {
    name = (name == null) ? "" : StringUtil2.replace(name, ' ', "_"); // replace blanks
    description = GribUtils.cleanupDescription(description);
  }

  public Grib1Parameter(Grib1ParamTableReader table, int number, String name, String description, String unit) {
    this(table, number, name, description, unit, null);
  }

  public Grib1ParamTableReader getTable() {
    return table;
  }

  @Override
  public int getDiscipline() {
    return 0;
  }

  @Override
  public int getCategory() {
    return 0;
  }

  @Override
  public int getNumber() {
    return number;
  }

  @Override
  public String getName() {
    return name;
  }

  public boolean useName() {
    return (name != null) && table.useParamName();
  }

  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public String getId() {
    return table.getCenter_id() + "." + table.getSubcenter_id() + "." + number;
  }

  @Override
  public String getUnit() {
    return unit;
  }

  @Override
  public String getAbbrev() {
    return null;
  }

  @Override
  public Float getFill() {
    return null;
  }

  @Override
  public Float getMissing() {
    return Float.NaN;
  }

  @Override
  public String getOperationalStatus() {
    return null;
  }
}

