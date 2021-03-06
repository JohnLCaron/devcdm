/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.ucdm.grib.coord;

import dev.ucdm.array.Immutable;
import org.jetbrains.annotations.Nullable;

import dev.ucdm.array.NumericCompare;
import dev.ucdm.array.Indent;
import dev.ucdm.core.util.Counters;
import dev.ucdm.grib.grib1.record.Grib1ParamLevel;
import dev.ucdm.grib.grib1.record.Grib1Record;
import dev.ucdm.grib.grib1.record.Grib1SectionProductDefinition;
import dev.ucdm.grib.grib1.table.Grib1Customizer;
import dev.ucdm.grib.grib2.iosp.Grib2Utils;
import dev.ucdm.grib.grib2.record.Grib2Pds;
import dev.ucdm.grib.grib2.record.Grib2Record;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Formatter;
import java.util.List;

/**
 * Vertical GRIB coordinates
 * Effectively immutable; setName() can only be called once.
 */
@Immutable
public class CoordinateVert implements Coordinate {
  private final List<VertCoordValue> levelSorted;
  private final int code; // Grib1 - code table 3; Grib2 - Code table 4.5
  private String name;
  private final VertCoordUnit vunit;
  private final boolean isLayer;

  public CoordinateVert(int code, VertCoordUnit vunit, List<VertCoordValue> levelSorted) {
    this.levelSorted = Collections.unmodifiableList(levelSorted);
    this.code = code;
    this.vunit = vunit;
    this.isLayer = levelSorted.get(0).isLayer();
  }

  public List<VertCoordValue> getLevelSorted() {
    return levelSorted;
  }

  @Override
  public List<?> getValues() {
    return levelSorted;
  }

  @Override
  public int getIndex(Object val) {
    return levelSorted.indexOf(val);
  }

  @Override
  public Object getValue(int idx) {
    return levelSorted.get(idx);
  }

  @Override
  public int getSize() {
    return levelSorted.size();
  }

  @Override
  public int getNCoords() {
    return getSize();
  }

  @Override
  public Type getType() {
    return Type.vert;
  }

  @Override
  public int estMemorySize() {
    return 160 + getSize() * (40 + NumericCompare.referenceSize);
  }

  @Override
  @Nullable
  public String getUnit() {
    return vunit == null ? null : vunit.units();
  }

  public VertCoordUnit getVertUnit() {
    return vunit;
  }

  public boolean isLayer() {
    return isLayer;
  }

  public boolean isPositiveUp() {
    return vunit.isPositiveUp();
  }

  public int getCode() {
    return code;
  }

  @Override
  public String getName() {
    return name;
  }

  public void setName(String name) {
    if (this.name != null)
      throw new IllegalStateException("Cant modify");
    this.name = name;
  }

  @Override
  public void showInfo(Formatter info, Indent indent) {
    info.format("%s%s: ", indent, getType());
    for (VertCoordValue level : levelSorted)
      info.format(" %s", level);
    info.format(" (%d)%n", levelSorted.size());
  }

  @Override
  public void showCoords(Formatter info) {
    info.format("Levels: (%s)%n", getUnit());
    for (VertCoordValue level : levelSorted)
      info.format("   %s%n", level);
  }

  @Override
  public Counters calcDistributions() {
    Counters counters = new Counters();
    counters.add("resol");

    if (isLayer) {
      counters.add("intv");
      for (int i = 0; i < levelSorted.size(); i++) {
        double intv = (levelSorted.get(i)).getValue2() - (levelSorted.get(i)).getValue1();
        counters.count("intv", intv);
        if (i < levelSorted.size() - 1) {
          double resol = (levelSorted.get(i + 1)).getValue1() - (levelSorted.get(i)).getValue1();
          counters.count("resol", resol);
        }
      }

    } else {

      for (int i = 0; i < levelSorted.size() - 1; i++) {
        double diff = levelSorted.get(i + 1).getValue1() - levelSorted.get(i).getValue1();
        counters.count("resol", diff);
      }
    }

    return counters;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;

    CoordinateVert that = (CoordinateVert) o;

    if (code != that.code)
      return false;
    return levelSorted.equals(that.levelSorted);

  }

  @Override
  public int hashCode() {
    int result = levelSorted.hashCode();
    result = 31 * result + code;
    return result;
  }

  //////////////////////////////////////////////////////////////

  public static class Builder2 extends CoordinateBuilderImpl<Grib2Record> {
    int code;
    VertCoordUnit vunit;

    public Builder2(int code, VertCoordUnit vunit) {
      this.code = code;
      this.vunit = vunit;
    }

    @Override
    public Object extract(Grib2Record gr) {
      Grib2Pds pds = gr.getPDS();
      if (Grib2Utils.isLayer(pds))
        return new VertCoordValue(pds.getLevelValue1(), pds.getLevelValue2());
      else
        return new VertCoordValue(pds.getLevelValue1());
    }

    @Override
    public Coordinate makeCoordinate(List<Object> values) {
      List<VertCoordValue> levelSorted = new ArrayList<>(values.size());
      for (Object val : values)
        levelSorted.add((VertCoordValue) val);
      Collections.sort(levelSorted);
      return new CoordinateVert(code, vunit, levelSorted);
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
      boolean isLayer = cust.isLayer(pds.getLevelType());
      Grib1ParamLevel plevel = cust.getParamLevel(pds);
      if (isLayer)
        return new VertCoordValue(plevel.getValue1(), plevel.getValue2());
      else
        return new VertCoordValue(plevel.getValue1());
    }

    @Override
    public Coordinate makeCoordinate(List<Object> values) {
      List<VertCoordValue> levelSorted = new ArrayList<>(values.size());
      for (Object val : values)
        levelSorted.add((VertCoordValue) val);
      Collections.sort(levelSorted);
      return new CoordinateVert(code, cust.getVertUnit(code), levelSorted);
    }
  }

}
