/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.cdm.dataset.geoloc.projection;

import dev.cdm.array.Immutable;
import dev.cdm.core.constants.CDM;
import dev.cdm.core.constants.CF;
import dev.cdm.dataset.geoloc.LatLonPoint;
import dev.cdm.dataset.geoloc.LatLonPoints;
import dev.cdm.dataset.geoloc.Projection;
import dev.cdm.dataset.geoloc.ProjectionPoint;

import java.util.Objects;

/**
 * Equidistant Cylindrical, spherical earth.
 * AKA Equirectangular, Rectangular, La Carte Parallelogrammatique, Die Rechteckige Plattkarte,
 * and Equirectangular.
 * Includes the special case of the plate carrée projection (also called the geographic projection,
 * lat/lon projection, or plane chart), when standardParellel = 0 (tangent at the equator).
 * See John Snyder, Map Projections used by the USGS, Bulletin 1532, 2nd edition (1983), p 90
 */
@Immutable
public class EquidistantCylindrical extends AbstractProjection {
  private final double earthRadius;
  private final double centralLat; // latitude of the origin in degrees
  private final double centralLon; // longitude of the origin in degrees
  private final double standardParellel; // standard parallel in degrees
  private final double falseEasting, falseNorthing;
  private final double A;

  @Override
  public Projection constructCopy() {
    return new EquidistantCylindrical(centralLat, centralLon, standardParellel, falseEasting, falseNorthing, earthRadius);
  }

  public EquidistantCylindrical() {
    this(-105, 20.0, 0.0, 0.0, 0.0, EARTH_RADIUS);
  }

  public EquidistantCylindrical(double centralLat, double centralLon, double standardParellel) {
    this(centralLat, centralLon, standardParellel, 0.0, 0.0, EARTH_RADIUS);
  }

  public EquidistantCylindrical(double centralLat, double centralLon, double standardParellel, double falseEasting, double falseNorthing) {
    this(centralLat, centralLon, standardParellel, falseEasting, falseNorthing, EARTH_RADIUS);
  }

  /**
   * Construct a EquidistantCylindrical Projection.
   *
   * @param centralLat central parallel of the map (degrees)
   * @param centralLon central meridian of the map (degrees)
   * @param standardParellel standard parallel (degrees), (north and south of the equator), cylinder cuts earth at these latitudes.
   * @param falseEasting false_easting in km
   * @param falseNorthing false_northing in km
   * @param radius earth radius in km
   */
  public EquidistantCylindrical(double centralLat, double centralLon, double standardParellel, double falseEasting, double falseNorthing, double radius) {
    super("EquidistantCylindrical", false);

    this.centralLat = centralLat;
    this.centralLon = centralLon;
    this.standardParellel = standardParellel;
    this.falseEasting = falseEasting;
    this.falseNorthing = falseNorthing;
    this.earthRadius = radius;

    // standard parallel in radians
    this.A = earthRadius * Math.cos(Math.toRadians(standardParellel)); // incorporates the scale factor at par

    addParameter(CF.GRID_MAPPING_NAME, CF.MERCATOR);
    addParameter("latitude_of_central_meridian", centralLat);
    addParameter(CF.LONGITUDE_OF_CENTRAL_MERIDIAN, centralLon);
    addParameter(CF.STANDARD_PARALLEL, standardParellel);
    addParameter(CF.EARTH_RADIUS, earthRadius * 1000);
    if ((falseEasting != 0.0) || (falseNorthing != 0.0)) {
      addParameter(CF.FALSE_EASTING, falseEasting);
      addParameter(CF.FALSE_NORTHING, falseNorthing);
      addParameter(CDM.UNITS, "km");
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    EquidistantCylindrical that = (EquidistantCylindrical) o;
    return Double.compare(that.earthRadius, earthRadius) == 0 && Double.compare(that.centralLat, centralLat) == 0 && Double.compare(that.centralLon, centralLon) == 0 && Double.compare(that.standardParellel, standardParellel) == 0 && Double.compare(that.falseEasting, falseEasting) == 0 && Double.compare(that.falseNorthing, falseNorthing) == 0;
  }

  @Override
  public int hashCode() {
    return Objects.hash(earthRadius, centralLat, centralLon, standardParellel, falseEasting, falseNorthing);
  }

  @Override
  public String toString() {
    return "EquidistantCylindrical{" +
            "earthRadius=" + earthRadius +
            ", centralLat=" + centralLat +
            ", centralLon=" + centralLon +
            ", standardParellel=" + standardParellel +
            ", falseEasting=" + falseEasting +
            ", falseNorthing=" + falseNorthing +
            '}';
  }

  /**
   * Does the line between these two points cross the projection "seam".
   *
   * @param pt1 the line goes between these two points
   * @param pt2 the line goes between these two points
   * @return false if there is no seam
   */
  @Override
  public boolean crossSeam(ProjectionPoint pt1, ProjectionPoint pt2) {
    // either point is infinite
    if (LatLonPoints.isInfinite(pt1) || LatLonPoints.isInfinite(pt2)) {
      return true;
    }

    // opposite signed long lines
    return (pt1.x() * pt2.x() < 0);
  }

  @Override
  public ProjectionPoint latLonToProj(LatLonPoint latLon) {
    double fromLatRadians = Math.toRadians(latLon.latitude());
    double fromLonRadians = Math.toRadians(latLon.longitude());

    // infinite projection
    double toX = A * Math.toRadians(LatLonPoints.range180(latLon.longitude() - this.centralLon));
    double toY = this.earthRadius * Math.toRadians(latLon.latitude() - this.centralLat);

    return ProjectionPoint.create(toX + falseEasting, toY + falseNorthing);
  }

  @Override
  public LatLonPoint projToLatLon(ProjectionPoint world) {
    double fromX = world.x() - falseEasting;
    double fromY = world.y() - falseNorthing;

    double toLon = Math.toDegrees(fromX / A) + this.centralLon;
    double toLat = Math.toDegrees( fromY / this.earthRadius) + this.centralLat;

    return new LatLonPoint(toLat, toLon);
  }

}

