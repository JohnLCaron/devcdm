package dev.cdm.grid.internal;

import dev.cdm.array.Immutable;
import dev.cdm.core.api.Dimension;
import dev.cdm.core.constants.AxisType;
import dev.cdm.dataset.api.VariableDS;
import dev.cdm.grid.api.*;

import java.util.Arrays;
import java.util.List;

@Immutable
class GridIndexPermuter {
  private final int[] shape;
  private final int xDimOrgIndex, yDimOrgIndex, zDimOrgIndex, eDimOrgIndex, toDimOrgIndex, rtDimOrgIndex;

  GridIndexPermuter(GridCoordinateSystem gcs, VariableDS vds) {
    this.shape = vds.getShape();
    this.xDimOrgIndex = findDimension(vds, gcs.getXHorizAxis());
    this.yDimOrgIndex = findDimension(vds, gcs.getYHorizAxis());
    this.zDimOrgIndex = findDimension(vds, gcs.getVerticalAxis());
    this.eDimOrgIndex = findDimension(vds, gcs.getEnsembleAxis());
    if (gcs.getTimeCoordinateSystem() != null) {
      this.toDimOrgIndex = findDimension(vds, gcs.getTimeCoordinateSystem().getTimeOffsetAxis(0));
      this.rtDimOrgIndex = findDimension(vds, gcs.getTimeCoordinateSystem().getRunTimeAxis());
    } else {
      this.toDimOrgIndex = -1;
      this.rtDimOrgIndex = -1;
    }
  }

  // TODO this depends on the coord axis being a coord variable, or being dependent on a coord variable. Not robust?
  private int findDimension(VariableDS vds, GridAxis<?> want) {
    if (want == null) {
      return -1;
    }
    List<Dimension> dims = vds.getDimensions();
    for (int i = 0; i < dims.size(); i++) {
      Dimension d = dims.get(i);
      if (d.getShortName().equals(want.getName())) {
        return i;
      }
    }

    // This is the case where its a coordinate alias. TODO all the dependsOn hoopla
    String depends = (want.getDependsOn().size() == 1) ? want.getDependsOn().get(0) : null;
    for (int i = 0; i < dims.size(); i++) {
      Dimension d = dims.get(i);
      if (d.getShortName().equals(depends)) {
        return i;
      }
    }

    if (want.getDependenceType() == GridAxisDependenceType.scalar) {
      return -1;
    }

    throw new IllegalStateException("Cant find dimension index for " + want.getName());
  }

  dev.cdm.array.Section permute(dev.cdm.array.Section subset) {
    // get the ranges list in the order of the variable; a null range means "all" to vs.read()
    dev.cdm.array.Range[] varRange = new dev.cdm.array.Range[this.shape.length];
    for (dev.cdm.array.Range r : subset.getRanges()) {
      AxisType type = AxisType.valueOf(r.name());
      switch (type) {
        case Lon:
        case GeoX:
          varRange[xDimOrgIndex] = r;
          break;
        case Lat:
        case GeoY:
          varRange[yDimOrgIndex] = r;
          break;
        case Height:
        case Pressure:
        case GeoZ:
          if (zDimOrgIndex >= 0) {
            varRange[zDimOrgIndex] = r;
          }
          break;
        case Time:
        case TimeOffset:
          if (toDimOrgIndex >= 0) {
            varRange[toDimOrgIndex] = r;
          }
          break;
        case RunTime:
          if (rtDimOrgIndex >= 0) {
            varRange[rtDimOrgIndex] = r;
          }
          break;
        case Ensemble:
          if (eDimOrgIndex >= 0) {
            varRange[eDimOrgIndex] = r;
          }
          break;
        default:
          throw new RuntimeException("Unknown axis type " + type);
      }
    }
    dev.cdm.array.Section s = new dev.cdm.array.Section(Arrays.asList(varRange));

    // TODO could check that unfilled dimensions are length 1
    try {
      return dev.cdm.array.Section.fill(s, shape);
    } catch (dev.cdm.array.InvalidRangeException e) {
      throw new RuntimeException(e); // cant happen
    }
  }

  @Override
  public String toString() {
    return "GridIndexPermuter{" + "shape=" + Arrays.toString(shape) + ", eDimOrgIndex=" + eDimOrgIndex
        + ", rtDimOrgIndex=" + rtDimOrgIndex + ", toDimOrgIndex=" + toDimOrgIndex + ", zDimOrgIndex=" + zDimOrgIndex
        + ", yDimOrgIndex=" + yDimOrgIndex + ", xDimOrgIndex=" + xDimOrgIndex + '}';
  }
}
