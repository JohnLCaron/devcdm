/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package dev.ucdm.grib.coord;

import dev.ucdm.array.Indent;
import dev.ucdm.array.NumericCompare;
import dev.ucdm.core.util.Counters;
import dev.ucdm.grib.grib1.record.Grib1Record;
import dev.ucdm.grib.grib1.record.Grib1SectionProductDefinition;
import dev.ucdm.grib.grib1.table.Grib1Customizer;
import dev.ucdm.grib.grib2.record.Grib2Pds;
import dev.ucdm.grib.grib2.record.Grib2Record;

import dev.ucdm.array.Immutable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Formatter;
import java.util.List;
import java.util.stream.Collectors;

/** Ensemble coordinates */
@Immutable
public class CoordinateEns implements Coordinate {
  private final List<EnsCoordValue> ensSorted;
  private final int code;
  private String name = "ens";

  public CoordinateEns(int code, List<EnsCoordValue> ensSorted) {
    this.ensSorted = Collections.unmodifiableList(ensSorted);
    this.code = code;
  }

  public List<EnsCoordValue> getEnsSorted() {
    return ensSorted;
  }

  @Override
  public List<?> getValues() {
    return ensSorted;
  }

  @Override
  public int getIndex(Object val) {
    return ensSorted.indexOf(val);
  }

  @Override
  public Object getValue(int idx) {
    return ensSorted.get(idx);
  }

  public int getIndexByMember(double need) {
    for (int i = 0; i < ensSorted.size(); i++) {
      EnsCoordValue coord = ensSorted.get(i);
      if (NumericCompare.nearlyEquals(need, coord.ensMember()))
        return i;
    }
    return -1;
  }

  @Override
  public int getSize() {
    return ensSorted.size();
  }

  @Override
  public int getNCoords() {
    return getSize();
  }

  @Override
  public int estMemorySize() {
    return 160 + getSize() * (8 + NumericCompare.referenceSize);
  }

  @Override
  public Type getType() {
    return Type.ens;
  }

  @Override
  public String getUnit() {
    return "";
  }

  public int getCode() {
    return code;
  }

  @Override
  public String getName() {
    return name;
  }

  public void setName(String name) {
    if (!this.name.equals("ens"))
      throw new IllegalStateException("Cant modify");
    this.name = name;
  }

  @Override
  public void showInfo(Formatter info, Indent indent) {
    info.format("%s%s: ", indent, getType());
    for (EnsCoordValue level : ensSorted)
      info.format(" %s", level);
    info.format(" (%d)%n", ensSorted.size());
  }

  @Override
  public void showCoords(Formatter info) {
    info.format("Ensemble coords: (%s)%n", getUnit());
    for (EnsCoordValue level : ensSorted)
      info.format("   %s%n", level);
  }

  @Override
  public Counters calcDistributions() {
    Counters counters = new Counters();
    counters.add("resol");

    for (int i = 0; i < ensSorted.size() - 1; i++) {
      int diff = ensSorted.get(i + 1).ensMember() - ensSorted.get(i).ensMember();
      counters.count("resol", diff);
    }

    return counters;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;

    CoordinateEns that = (CoordinateEns) o;

    if (code != that.code)
      return false;
    return ensSorted.equals(that.ensSorted);

  }

  @Override
  public int hashCode() {
    int result = ensSorted.hashCode();
    result = 31 * result + code;
    return result;
  }

  //////////////////////////////////////////////////////////////

  public static class Builder2 extends CoordinateBuilderImpl<Grib2Record> {
    int code;

    public Builder2(int code) {
      this.code = code;
    }

    @Override
    public Object extract(Grib2Record gr) {
      Grib2Pds pds = gr.getPDS();
      Grib2Pds.PdsEnsemble pdse = (Grib2Pds.PdsEnsemble) pds;
      return new EnsCoordValue(pdse.getPerturbationType(), pdse.getPerturbationNumber());
    }

    @Override
    public Coordinate makeCoordinate(List<Object> values) {
      List<EnsCoordValue> levelSorted = new ArrayList<>(values.size());
      for (Object val : values)
        levelSorted.add((EnsCoordValue) val);
      Collections.sort(levelSorted);
      return new CoordinateEns(code, levelSorted);
    }
  }

  public static class Builder1 extends CoordinateBuilderImpl<Grib1Record> {
    int code;
    Grib1Customizer cust;

    public Builder1(Grib1Customizer cust, int code) {
      this.cust = cust;
      this.code = code;
    }

    @Override
    public Object extract(Grib1Record gr) {
      Grib1SectionProductDefinition pds = gr.getPDSsection();
      return new EnsCoordValue(pds.getPerturbationType(), pds.getPerturbationNumber());
    }

    @Override
    public Coordinate makeCoordinate(List<Object> values) {
      List<EnsCoordValue> levelSorted =
              values.stream().map(val -> (EnsCoordValue) val).sorted().collect(Collectors.toList());
      return new CoordinateEns(code, levelSorted);
    }
  }
}
