/*
 * Copyright (c) 1998-2022 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package dev.ucdm.grib.protoconvert;

import com.google.common.collect.ImmutableList;
import dev.cdm.array.Immutable;
import dev.ucdm.grib.grib1.record.Grib1Record;
import dev.ucdm.grib.grib1.record.Grib1SectionGridDefinition;
import dev.ucdm.grib.grib2.record.Grib2Record;
import dev.ucdm.grib.grib2.record.Grib2SectionGridDefinition;

import java.util.List;

@Immutable
public class Grib1Index {

  private final ImmutableList<Grib1SectionGridDefinition> gdsList;
  private final ImmutableList<Grib1Record> records;

  public Grib1Index(List<Grib1SectionGridDefinition> gdsList, List<Grib1Record> records) {
    this.gdsList = ImmutableList.copyOf(gdsList);
    this.records = ImmutableList.copyOf(records);
  }

  public List<Grib1SectionGridDefinition> getGds() {
    return gdsList;
  }

  public List<Grib1Record> getRecords() {
    return records;
  }

  public int getNRecords() {
    return records.size();
  }


}
