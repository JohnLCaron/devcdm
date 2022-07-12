package dev.ucdm.grid.internal;

import dev.ucdm.array.Array;
import dev.ucdm.array.ArrayType;
import dev.ucdm.array.Immutable;
import dev.ucdm.array.InvalidRangeException;
import dev.ucdm.core.api.AttributeContainer;
import dev.ucdm.dataset.api.VariableDS;
import dev.ucdm.grid.api.*;

import java.io.IOException;
import java.util.Formatter;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Wraps a VariableDS, turns into a Grid */
@Immutable
public class GridVariable implements Grid {
  private final GridCoordinateSystem cs;
  private final VariableDS vds;
  private final GridIndexPermuter permuter;

  GridVariable(GridCoordinateSystem cs, VariableDS vds) {
    this.cs = cs;
    this.vds = vds;
    this.permuter = new GridIndexPermuter(cs, vds);
  }

  @Override
  public GridCoordinateSystem getCoordinateSystem() {
    return this.cs;
  }

  @Override
  public String getName() {
    return vds.getShortName();
  }

  @Override
  public String getUnits() {
    return vds.getUnitsString() == null ? "" : vds.getUnitsString();
  }

  @Override
  public String getDescription() {
    return vds.getDescription() == null ? "" : vds.getDescription();
  }

  @Override
  public AttributeContainer attributes() {
    return vds.attributes();
  }

  @Override
  public ArrayType getArrayType() {
    return vds.getArrayType();
  }

  @Override
  public boolean hasMissing() {
    return vds.hasMissing();
  }

  @Override
  public boolean isMissing(double val) {
    return vds.isMissing(val);
  }

  /** Subsetting the coordinate system, then using that subset to do the read. Special to Netcdf, not general. */
  @Override
  public GridReferencedArray readData(GridSubset subset) throws IOException, InvalidRangeException {
    Formatter errlog = new Formatter();
    Optional<MaterializedCoordinateSystem> opt = this.cs.subset(subset, errlog);
    if (opt.isEmpty()) {
      throw new InvalidRangeException(errlog.toString());
    }
    MaterializedCoordinateSystem subsetCoordSys = opt.get();

    if (subsetCoordSys.specialReadNeeded()) {
      // handles longitude cylindrical coord
      Array<Number> data = subsetCoordSys.readSpecial(this);
      return new GridReferencedArray(getName(), getArrayType(), data, subsetCoordSys);
    } else {
      List<dev.ucdm.array.Range> ranges = subsetCoordSys.getSubsetRanges();
      Array<Number> data = readDataSection(new dev.ucdm.array.Section(ranges));
      return new GridReferencedArray(getName(), getArrayType(), data, subsetCoordSys);
    }
  }

  /**
   * This reads an arbitrary data section, returning the data in
   * canonical order (rt-e-t-z-y-x). If any dimension does not exist, ignore it.
   *
   * @param subset - each Range must be named by the axisType that its used for. order not important
   *
   * @return data[rt, e, t, z, y, x], eliminating missing dimensions. length=1 not eliminated
   */
  @Override
  public Array<Number> readDataSection(dev.ucdm.array.Section subset) throws InvalidRangeException, IOException {
    Array<?> dataVolume = vds.readArray(this.permuter.permute(subset));
    return (Array<Number>) dataVolume;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    GridVariable that = (GridVariable) o;
    return vds.equals(that.vds);
  }

  @Override
  public int hashCode() {
    return Objects.hash(vds);
  }

  @Override
  public String toString() {
    return vds + "\n permuter=" + permuter + '}';
  }

}
