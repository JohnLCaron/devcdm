/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package dev.ucdm.grib.grid;

import dev.ucdm.array.Array;
import dev.ucdm.array.ArrayType;
import dev.ucdm.array.InvalidRangeException;
import dev.ucdm.array.RangeIterator;
import dev.ucdm.core.api.Attribute;
import dev.ucdm.core.api.AttributeContainer;
import dev.ucdm.core.api.AttributeContainerMutable;
import dev.ucdm.core.constants.CDM;
import dev.ucdm.grid.api.Grid;
import dev.ucdm.grid.api.GridCoordinateSystem;
import dev.ucdm.grid.api.GridReferencedArray;
import dev.ucdm.grid.api.GridSubset;
import dev.ucdm.grid.api.MaterializedCoordinateSystem;
import dev.ucdm.grib.collection.GribCollection;
import dev.ucdm.grib.collection.VariableIndex;
import dev.ucdm.grib.common.GribArrayReader;
import dev.ucdm.grib.common.GribIosp;
import dev.ucdm.grib.common.util.SectionIterable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import java.util.Optional;

/** Grib implementation of {@link Grid} */
public class GribGrid implements Grid {
  private final GridCoordinateSystem coordinateSystem;
  public final GribCollection gribCollection;
  private final VariableIndex vi;
  private final String name;
  private final AttributeContainer attributes;

  public GribGrid(GribIosp iosp, GribCollection gribCollection, GridCoordinateSystem coordinateSystem,
                  VariableIndex vi) {
    this.coordinateSystem = coordinateSystem;
    this.gribCollection = gribCollection;
    this.vi = vi;
    this.name = iosp.makeVariableName(vi);

    AttributeContainerMutable atts = new AttributeContainerMutable(this.name);
    atts.addAttribute(new Attribute(CDM.LONG_NAME, iosp.makeVariableLongName(vi)));
    atts.addAttribute(new Attribute(CDM.UNITS, iosp.makeVariableUnits(vi)));
    gribCollection.addVariableAttributes(atts, vi);
    this.attributes = atts.toImmutable();
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getDescription() {
    return this.attributes.findAttributeString(CDM.LONG_NAME, null);
  }

  @Override
  public String getUnits() {
    return this.attributes.findAttributeString(CDM.UNITS, null);
  }

  @Override
  public AttributeContainer attributes() {
    return this.attributes;
  }

  @Override
  public ArrayType getArrayType() {
    return ArrayType.FLOAT;
  }

  @Override
  public GridCoordinateSystem getCoordinateSystem() {
    return coordinateSystem;
  }

  @Override
  public boolean hasMissing() {
    return true;
  }

  @Override
  public boolean isMissing(double val) {
    return Double.isNaN(val);
  }

  @Override
  public GridReferencedArray readData(GridSubset subset) throws IOException, InvalidRangeException {
    Formatter errLog = new Formatter();
    Optional<MaterializedCoordinateSystem> opt = coordinateSystem.subset(subset, errLog);
    if (opt.isEmpty()) {
      throw new InvalidRangeException(errLog.toString()); // TODO: Optional, empty, null?
    }
    MaterializedCoordinateSystem subsetCoordSys = opt.get();

    if (subsetCoordSys.specialReadNeeded()) {
      // handles longitude cylindrical coord when data has full (360 deg) longitude axis.
      Array<Number> data = subsetCoordSys.readSpecial(this);
      return new GridReferencedArray(getName(), getArrayType(), data, subsetCoordSys);

    } else {
      List<RangeIterator> ranges = new ArrayList<>(subsetCoordSys.getSubsetRanges());
      SectionIterable want = new SectionIterable(ranges, getCoordinateSystem().getNominalShape());

      GribArrayReader dataReader = GribArrayReader.factory(gribCollection, vi);
      Array<?> data = dataReader.readData(want);

      return new GridReferencedArray(this.name, getArrayType(), (Array<Number>) data, subsetCoordSys);
    }
  }

  @Override
  public Array<Number> readDataSection(dev.ucdm.array.Section section) throws InvalidRangeException, IOException {
    GribArrayReader dataReader = GribArrayReader.factory(gribCollection, vi);
    SectionIterable want = new SectionIterable(section, getCoordinateSystem().getNominalShape());
    return (Array<Number>) dataReader.readData(want);
  }

  @Override
  public String toString() {
    return name;
  }
}
