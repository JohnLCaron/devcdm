/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.cdm.grid.api;

import dev.cdm.array.Array;
import dev.cdm.array.ArrayType;

/**
 * A Grid's data array with Geo referencing.
 * The materializedCoordinateSystem matches the data.
 */
public record GridReferencedArray(String name,
                                  ArrayType arrayType,
                                  Array<Number> data,
                                  MaterializedCoordinateSystem materializedCoordinateSystem) {
}
