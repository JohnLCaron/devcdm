/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.cdm.dataset.geoloc;

import dev.cdm.array.NumericCompare;

/** Points on the Projective geometry plane. */
public record ProjectionPoint(double x, double y) {

  /** Create a ProjectionPoint. */
  public static ProjectionPoint create(double x, double y) {
    return new ProjectionPoint(x, y);
  }

  /**
   * Returns the result of {@link #nearlyEquals(ProjectionPoint, double)}, with
   * {@link NumericCompare#defaultMaxRelativeDiffDouble}.
   */
  public boolean nearlyEquals(ProjectionPoint other) {
    return nearlyEquals(other, NumericCompare.defaultMaxRelativeDiffDouble);
  }

  /**
   * Returns {@code true} if this point is nearly equal to {@code other}. The "near equality" of points is determined
   * using {@link NumericCompare#nearlyEquals(double, double, double)}, with the specified maxRelDiff.
   *
   * @param other the other point to check.
   * @param maxRelDiff the maximum {@link NumericCompare#relativeDifference relative difference} the two points may have.
   * @return {@code true} if this point is nearly equal to {@code other}.
   */
  public boolean nearlyEquals(ProjectionPoint other, double maxRelDiff) {
    return NumericCompare.nearlyEquals(x(), other.x(), maxRelDiff) && NumericCompare.nearlyEquals(y(), other.y(), maxRelDiff);
  }
}
