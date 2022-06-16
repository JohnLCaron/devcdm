/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.cdm.core.constants;

/** Enumeration of CDM Feature types, aka "Scientific Data Types". */

public enum FeatureType {
  ANY, // No specific type

  COVERAGE, // any of the coverage types: GRID, FMRC, SWATH, CURVILINEAR
  GRID, // seperable coordinates
  FMRC, // two time dimensions, runtime and forecast time
  SWATH, // 2D latlon, dependent time, polar orbiting satellites
  CURVILINEAR, // 2D latlon, independent time

  ANY_POINT, // Any of the point types
  POINT, // unconnected points
  PROFILE, // fixed x,y with data along z
  STATION, // timeseries at named location
  STATION_PROFILE, // timeseries of profiles
  TRAJECTORY, // connected points in space and time
  TRAJECTORY_PROFILE, // trajectory of profiles

  RADIAL, // polar coordinates
  STATION_RADIAL, // time series of radial data

  SIMPLE_GEOMETRY, // geospatial associations with data

  // experimental
  IMAGE, // pixels, may not be geolocatable
  UGRID; // unstructured grids

  /**
   * Find the FeatureType that matches this name.
   *
   * @param name find FeatureType with this name, case insensitive.
   * @return FeatureType or null if no match.
   */
  public static FeatureType getType(String name) {
    if (name == null)
      return null;
    try {
      return valueOf(name.toUpperCase());
    } catch (IllegalArgumentException e) { // lame!
      return null;
    }
  }

  public boolean isPointFeatureType() {
    return (this == FeatureType.ANY_POINT) || (this == FeatureType.POINT) || (this == FeatureType.STATION)
        || (this == FeatureType.TRAJECTORY) || (this == FeatureType.PROFILE) || (this == FeatureType.STATION_PROFILE)
        || (this == FeatureType.TRAJECTORY_PROFILE);
  }

  public boolean isCoverageFeatureType() {
    return (this == FeatureType.COVERAGE) || (this == FeatureType.GRID) || (this == FeatureType.FMRC)
        || (this == FeatureType.SWATH) || (this == FeatureType.CURVILINEAR);
  }

  public boolean isUnstructuredGridFeatureType() {
    return this == FeatureType.UGRID;
  }

  public boolean isSimpleGeometry() {
    return this == FeatureType.SIMPLE_GEOMETRY;
  }
}
