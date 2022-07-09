/*
 * Copyright (c) 1998-2022 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package dev.ucdm.grib.protoconvert;

import com.google.common.collect.ImmutableList;
import dev.cdm.array.Immutable;
import dev.ucdm.grib.grib2.record.Grib2Record;
import dev.ucdm.grib.grib2.record.Grib2SectionGridDefinition;

import java.util.List;

@Immutable
public class Grib2Index {

  private final ImmutableList<Grib2SectionGridDefinition> gdsList;
  private final ImmutableList<Grib2Record> records;

  public Grib2Index(List<Grib2SectionGridDefinition> gdsList, List<Grib2Record> records) {
    this.gdsList = ImmutableList.copyOf(gdsList);
    this.records = ImmutableList.copyOf(records);
  }

  public List<Grib2SectionGridDefinition> getGds() {
    return gdsList;
  }

  public List<Grib2Record> getRecords() {
    return records;
  }

  public int getNRecords() {
    return records.size();
  }


}
