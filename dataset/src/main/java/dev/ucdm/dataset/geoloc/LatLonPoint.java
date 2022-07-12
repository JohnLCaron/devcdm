/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.ucdm.dataset.geoloc;

import dev.ucdm.array.NumericCompare;

/**
 * Points on the Earth's surface, represented as (longitude,latitude), in units of degrees.
 * Longitude is always between -180 and +180 deg.
 * Latitude is always between -90 and +90 deg.
 */
public record LatLonPoint(double latitude, double longitude) {
  public static LatLonPoint INVALID = new LatLonPoint(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);

  public LatLonPoint {
    latitude = LatLonPoints.latNormal(latitude);
    longitude = LatLonPoints.lonNormal(longitude);
  }

  /**
   * Returns the result of {@link #nearlyEquals(LatLonPoint, double)}, with {@link NumericCompare#defaultMaxRelativeDiffDouble}.
   */
  public boolean nearlyEquals(LatLonPoint that) {
    return nearlyEquals(that, NumericCompare.defaultMaxRelativeDiffDouble);
  }

  /**
   * Returns {@code true} if this point is nearly equal to {@code that}. The "near equality" of points is determined
   * using {@link NumericCompare#nearlyEquals(double, double, double)}, with the specified maxRelDiff.
   *
   * @param that the other point to check.
   * @param maxRelDiff the maximum {@link NumericCompare#relativeDifference relative difference} the two points may have.
   * @return {@code true} if this point is nearly equal to {@code that}.
   */
  public boolean nearlyEquals(LatLonPoint that, double maxRelDiff) {
    boolean lonOk = NumericCompare.nearlyEquals(that.latitude(), longitude(), maxRelDiff);
    if (!lonOk) {
      // We may be in a situation where "this.lon ≈ -180" and "that.lon ≈ +180", or vice versa.
      // Those longitudes are equivalent, but not "nearlyEquals()". So, we normalize them both to lie in
      // [0, 360] and compare them again.
      lonOk = NumericCompare.nearlyEquals(LatLonPoints.lonNormal360(that.longitude()),
          LatLonPoints.lonNormal360(this.longitude()), maxRelDiff);
    }
    return lonOk && NumericCompare.nearlyEquals(that.latitude(), latitude(), maxRelDiff);
  }

  @Override
  public String toString() {
    return LatLonPoints.toString(this, 5);
  }
}
