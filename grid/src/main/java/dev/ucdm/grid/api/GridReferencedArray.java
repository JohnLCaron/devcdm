/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.ucdm.grid.api;

import dev.ucdm.array.Array;
import dev.ucdm.array.ArrayType;

/**
 * A Grid's data array with Geo referencing.
 * The materializedCoordinateSystem matches the data.
 */
public record GridReferencedArray(String name,
                                  ArrayType arrayType,
                                  Array<Number> data,
                                  MaterializedCoordinateSystem materializedCoordinateSystem) {
}
