/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.ucdm.dataset.transform.vertical;

import dev.ucdm.array.Array;
import dev.ucdm.array.ArrayType;
import dev.ucdm.array.Arrays;
import dev.ucdm.array.InvalidRangeException;
import dev.ucdm.core.api.AttributeContainer;
import dev.ucdm.dataset.api.CoordinateSystem;
import dev.ucdm.dataset.api.CdmDataset;

import org.jetbrains.annotations.Nullable;
import java.io.IOException;
import java.util.Formatter;
import java.util.Optional;

/**
 * A transformation to a vertical reference coordinate system, such as height or pressure.
 */
public interface VerticalTransform {

  /** The name of the Vertical Transform, must be unique in the dataset. */
  String getName();

  /** The unit string for the vertical coordinate. */
  @Nullable
  String getUnitString();

  /**
   * Get the 3D vertical coordinate array for this time step.
   * Must be in "canonical order" : z, y, x.
   *
   * @param timeIndex the time index. Ignored if !isTimeDependent().
   *
   * @return 3D vertical coordinate array, for the given t.
   */
  Array<Number> getCoordinateArray3D(int timeIndex) throws IOException, InvalidRangeException;

  /**
   * Get the 1D vertical coordinate array for this time step and point
   *
   * @param timeIndex the time index. Ignored if !isTimeDependent().
   * @param xIndex the x index
   * @param yIndex the y index
   * @return vertical coordinate array
   */
  default Array<Number> getCoordinateArray1D(int timeIndex, int xIndex, int yIndex) throws IOException, InvalidRangeException {
    Array<Number> array3D = getCoordinateArray3D(timeIndex);
    int nz = array3D.getShape()[0];
    double[] result = new double[nz];

    int count = 0;
    for (int z = 0; z < nz; z++) {
      result[count++] = array3D.get(z, yIndex, xIndex).doubleValue();
    }

    return Arrays.factory(ArrayType.DOUBLE, new int[] {nz}, result);
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * A Builder of VerticalTransforms.
   * Note the use of CdmDataset and CoordinateSystem. VerticalTransform are only
   * available on Grids built on CdmDataset and GcdmGridDataset. GRIB does not have these.
   */
  interface Builder {
    Optional<VerticalTransform> create(CdmDataset ds, CoordinateSystem csys, AttributeContainer params,
                                       Formatter errlog);
  }
}

