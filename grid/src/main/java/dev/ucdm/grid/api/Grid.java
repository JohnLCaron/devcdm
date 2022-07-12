/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.ucdm.grid.api;

import dev.ucdm.array.Array;
import dev.ucdm.array.ArrayType;
import dev.ucdm.array.InvalidRangeException;
import dev.ucdm.array.IsMissingEvaluator;
import dev.ucdm.core.api.AttributeContainer;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

/**
 * A georeferenced Field.
 * Always described by an orthogonal collection of 1D GridAxes.
 */
public interface Grid extends IsMissingEvaluator {

  /** The Grid name. */
  String getName();

  /** The Grid description. */
  String getDescription();

  /** The Grid unit string. */
  String getUnits();

  /** The Grid attributes. */
  AttributeContainer attributes();

  /** The underlying data type. Must imply a subclass of Number. */
  ArrayType getArrayType();

  /** The Grid's GridCoordinateSystem. */
  GridCoordinateSystem getCoordinateSystem();

  /** The Grid's GridTimeCoordinateSystem. May be null. */
  @Nullable
  default GridTimeCoordinateSystem getTimeCoordinateSystem() {
    return getCoordinateSystem().getTimeCoordinateSystem();
  }

  /** The Grid's GridHorizCoordinateSystem. */
  default GridHorizCoordinateSystem getHorizCoordinateSystem() {
    return getCoordinateSystem().getHorizCoordinateSystem();
  }

  /**
   * Read the specified subset of data, return result as a Georeferenced Array.
   * The GridReferencedArray has a Materialized CoordinateSystem, which may be somewhat different
   * than the shape of the GridCoordinateSystem.
   */
  GridReferencedArray readData(GridSubset subset) throws IOException, dev.ucdm.array.InvalidRangeException;

  /** A GridReader to read data out of this Grid. */
  default GridReader getReader() {
    return new GridReader(this);
  }

  // experimental
  Array<Number> readDataSection(dev.ucdm.array.Section subset) throws InvalidRangeException, IOException;

}
