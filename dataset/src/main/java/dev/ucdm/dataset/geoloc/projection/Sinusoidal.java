/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.ucdm.dataset.geoloc.projection;

import com.google.common.base.Preconditions;
import com.google.common.math.DoubleMath;
import dev.ucdm.core.constants.CDM;
import dev.ucdm.core.constants.CF;
import dev.ucdm.array.NumericCompare;
import dev.ucdm.dataset.geoloc.*;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static dev.ucdm.dataset.geoloc.LatLonPoint.INVALID;

/**
 * Sinusoidal projection, spherical earth.
 * See John Snyder, Map Projections used by the USGS, Bulletin 1532, 2nd edition (1983), p 243
 */

public class Sinusoidal extends AbstractProjection {
  private final double earthRadius;
  private final double centMeridian; // central Meridian in degrees
  private final double falseEasting, falseNorthing;

  @Override
  public Projection constructCopy() {
    return new Sinusoidal(getCentMeridian(), getFalseEasting(), getFalseNorthing(), getEarthRadius());
  }

  /**
   * Constructor with default parameters
   */
  public Sinusoidal() {
    this(0.0, 0.0, 0.0, EARTH_RADIUS);
  }

  /**
   * Construct a Sinusoidal Projection.
   *
   * @param centMeridian central Meridian (degrees)
   * @param false_easting false_easting in km
   * @param false_northing false_northing in km
   * @param radius earth radius in km
   */
  public Sinusoidal(double centMeridian, double false_easting, double false_northing, double radius) {
    super(CF.SINUSOIDAL, false);

    this.centMeridian = centMeridian;
    this.falseEasting = false_easting;
    this.falseNorthing = false_northing;
    this.earthRadius = radius;

    addParameter(CF.GRID_MAPPING_NAME, CF.SINUSOIDAL);
    addParameter(CF.LONGITUDE_OF_CENTRAL_MERIDIAN, centMeridian);
    addParameter(CF.EARTH_RADIUS, earthRadius * 1000);

    if ((false_easting != 0.0) || (false_northing != 0.0)) {
      addParameter(CF.FALSE_EASTING, false_easting);
      addParameter(CF.FALSE_NORTHING, false_northing);
      addParameter(CDM.UNITS, "km");
    }

  }

  /**
   * Get the central Meridian in degrees
   *
   * @return the central Meridian
   */
  public double getCentMeridian() {
    return centMeridian;
  }

  /**
   * Get the false easting, in km.
   *
   * @return the false easting.
   */
  public double getFalseEasting() {
    return falseEasting;
  }

  /**
   * Get the false northing, in km.
   *
   * @return the false northing.
   */
  public double getFalseNorthing() {
    return falseNorthing;
  }

  public double getEarthRadius() {
    return earthRadius;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    Sinusoidal that = (Sinusoidal) o;
    if (Double.compare(that.centMeridian, centMeridian) != 0) {
      return false;
    }
    if (Double.compare(that.earthRadius, earthRadius) != 0) {
      return false;
    }
    if (Double.compare(that.falseEasting, falseEasting) != 0) {
      return false;
    }
    return Double.compare(that.falseNorthing, falseNorthing) == 0;

  }

  @Override
  public int hashCode() {
    int result;
    long temp;
    temp = earthRadius != +0.0d ? Double.doubleToLongBits(earthRadius) : 0L;
    result = (int) (temp ^ (temp >>> 32));
    temp = centMeridian != +0.0d ? Double.doubleToLongBits(centMeridian) : 0L;
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    temp = falseEasting != +0.0d ? Double.doubleToLongBits(falseEasting) : 0L;
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    temp = falseNorthing != +0.0d ? Double.doubleToLongBits(falseNorthing) : 0L;
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    return result;
  }

  @Override
  public String toString() {
    return "Sinusoidal{" + "earthRadius=" + earthRadius + ", centMeridian=" + centMeridian + ", falseEasting="
        + falseEasting + ", falseNorthing=" + falseNorthing + '}';
  }

  @Override
  public boolean crossSeam(ProjectionPoint pt1, ProjectionPoint pt2) {
    // either point is infinite
    if (LatLonPoints.isInfinite(pt1) || LatLonPoints.isInfinite(pt2)) {
      return true;
    }

    // opposite signed long lines
    double x1 = pt1.x() - falseEasting;
    double x2 = pt2.x() - falseEasting;
    return (x1 * x2 < 0) && (Math.abs(x1 - x2) > earthRadius);
  }

  @Override
  public ProjectionPoint latLonToProj(LatLonPoint latLon) {
    double deltaLon_d = LatLonPoints.range180(latLon.longitude() - centMeridian);
    double fromLat_r = Math.toRadians(latLon.latitude());

    double toX = earthRadius * Math.toRadians(deltaLon_d) * Math.cos(fromLat_r);
    double toY = earthRadius * fromLat_r; // p 247 Snyder

    return ProjectionPoint.create(toX + falseEasting, toY + falseNorthing);
  }

  @Override
  public LatLonPoint projToLatLon(ProjectionPoint world) {
    double fromX = world.x() - falseEasting;
    double fromY = world.y() - falseNorthing;

    double toLat_r = fromY / earthRadius;
    double toLon_r;

    if (NumericCompare.nearlyEquals(Math.abs(toLat_r), PI_OVER_2, 1e-10)) {
      toLat_r = toLat_r < 0 ? -PI_OVER_2 : +PI_OVER_2;
      toLon_r = Math.toRadians(centMeridian); // if lat == +- pi/2, set lon = centMeridian (Snyder 248)
    } else if (Math.abs(toLat_r) < PI_OVER_2) {
      toLon_r = Math.toRadians(centMeridian) + fromX / (earthRadius * Math.cos(toLat_r));
    } else {
      return INVALID; // Projection point is off the map.
    }

    if (NumericCompare.nearlyEquals(Math.abs(toLon_r), PI, 1e-10)) {
      toLon_r = toLon_r < 0 ? -PI : +PI;
    } else if (Math.abs(toLon_r) > PI) {
      return INVALID; // Projection point is off the map.
    }
    return new LatLonPoint(Math.toDegrees(toLat_r), Math.toDegrees(toLon_r));
  }

  @Override
  public LatLonRect projToLatLonBB(ProjectionRect projBB) {
    List<ProjectionPoint> pointsOfInterest = new LinkedList<>();

    ProjectionPoint northPole = latLonToProj(new LatLonPoint(90, 0));
    if (projBB.contains(northPole)) {
      pointsOfInterest.add(northPole);
    }

    ProjectionPoint southPole = latLonToProj(new LatLonPoint(-90, 0));
    if (projBB.contains(southPole)) {
      pointsOfInterest.add(southPole);
    }

    if (pointsOfInterest.size() == 2) { // projBB contains both north and south poles, and thus, the entire map.
      return new LatLonRect();
    }

    List<ProjectionPoint> corners = Arrays.asList(projBB.getLowerLeftPoint(), projBB.getLowerRightPoint(),
        projBB.getUpperLeftPoint(), projBB.getUpperRightPoint());

    for (ProjectionPoint corner : corners) {
      if (projToLatLon(corner) != INVALID) {
        pointsOfInterest.add(corner);
      }
    }

    pointsOfInterest.addAll(getMapEdgeIntercepts(projBB));

    return makeLatLonRect(pointsOfInterest);
  }

  /**
   * Returns the points at which {@code projBB} intersects the map edge.
   *
   * @param projBB defines a bounding box that may intersect the map edge, in projection coordinates.
   * @return the points at which {@code projBB} intersects the map edge. May be empty.
   */
  public List<ProjectionPoint> getMapEdgeIntercepts(ProjectionRect projBB) {
    List<ProjectionPoint> intercepts = new LinkedList<>();

    for (ProjectionPoint topIntercept : getMapEdgeInterceptsAtY(projBB.getUpperRightPoint().y())) {
      if (pointIsBetween(topIntercept, projBB.getUpperLeftPoint(), projBB.getUpperRightPoint())) {
        intercepts.add(topIntercept);
      }
    }

    for (ProjectionPoint rightIntercept : getMapEdgeInterceptsAtX(projBB.getUpperRightPoint().x())) {
      if (pointIsBetween(rightIntercept, projBB.getUpperRightPoint(), projBB.getLowerRightPoint())) {
        intercepts.add(rightIntercept);
      }
    }

    for (ProjectionPoint bottomIntercept : getMapEdgeInterceptsAtY(projBB.getLowerLeftPoint().y())) {
      if (pointIsBetween(bottomIntercept, projBB.getLowerLeftPoint(), projBB.getLowerRightPoint())) {
        intercepts.add(bottomIntercept);
      }
    }

    for (ProjectionPoint leftIntercept : getMapEdgeInterceptsAtX(projBB.getLowerLeftPoint().x())) {
      if (pointIsBetween(leftIntercept, projBB.getLowerLeftPoint(), projBB.getUpperLeftPoint())) {
        intercepts.add(leftIntercept);
      }
    }

    return intercepts;
  }

  /**
   * Returns the points at which the line {@code x = x0} intersects the map edge.
   *
   * @param x0 defines a line that may intersect the map edge, in projection coordinates.
   * @return the points at which the line {@code x = x0} intersects the map edge. May be empty.
   */
  public List<ProjectionPoint> getMapEdgeInterceptsAtX(double x0) {
    List<ProjectionPoint> mapEdgeIntercepts = new LinkedList<>();
    if (projToLatLon(x0, falseNorthing) == INVALID) { // The line {@code x = x0} does not intersect the map.
      return mapEdgeIntercepts; // Empty list.
    }

    double x0natural = x0 - falseEasting;
    double limitLon_r = (x0natural < 0) ? -PI : +PI;
    double deltaLon_r = limitLon_r - Math.toRadians(centMeridian);

    // This formula comes from solving 30-1 for phi, and then plugging it into 30-2. See Snyder, p 247.
    double minY = -earthRadius * Math.acos(x0natural / (earthRadius * deltaLon_r));
    double maxY = +earthRadius * Math.acos(x0natural / (earthRadius * deltaLon_r));

    mapEdgeIntercepts.add(ProjectionPoint.create(x0, minY + falseNorthing));
    mapEdgeIntercepts.add(ProjectionPoint.create(x0, maxY + falseNorthing));
    return mapEdgeIntercepts;
  }

  /**
   * Returns the points at which the line {@code y = y0} intersects the map edge.
   *
   * @param y0 defines a line that intersects the map edge, in projection coordinates.
   * @return the points at which the line {@code y = y0} intersects the map edge. May be empty.
   */
  public List<ProjectionPoint> getMapEdgeInterceptsAtY(double y0) {
    List<ProjectionPoint> mapEdgeIntercepts = new LinkedList<>();
    if (projToLatLon(falseEasting, y0) == INVALID) { // The line {@code y = y0} does not intersect the map.
      return mapEdgeIntercepts; // Empty list.
    }

    double minX = getXAt(y0, -PI);
    double maxX = getXAt(y0, +PI);

    mapEdgeIntercepts.add(ProjectionPoint.create(minX, y0));
    mapEdgeIntercepts.add(ProjectionPoint.create(maxX, y0));
    return mapEdgeIntercepts;
  }

  private double getXAt(double y0, double lon_r) {
    double y0natural = y0 - falseNorthing;
    double deltaLon_r = lon_r - Math.toRadians(centMeridian);

    // This formula comes from plugging 30-6 into 30-1. See Snyder, p 247-248.
    double x = earthRadius * deltaLon_r * Math.cos(y0natural / earthRadius);

    return x + falseEasting;
  }

  private boolean pointIsBetween(ProjectionPoint point, ProjectionPoint linePoint1, ProjectionPoint linePoint2) {
    if (linePoint1.x() == linePoint2.x()) { // No fuzzy comparison necessary.
      Preconditions.checkArgument(point.x() == linePoint1.x(), "point should have the same X as the line.");

      double minY = Math.min(linePoint1.y(), linePoint2.y());
      double maxY = Math.max(linePoint1.y(), linePoint2.y());

      // Returns true if point.getY() is in the range [minY, maxY], with fuzzy math.
      return DoubleMath.fuzzyCompare(minY, point.y(), TOLERANCE) <= 0
          && DoubleMath.fuzzyCompare(point.y(), maxY, TOLERANCE) <= 0;
    } else if (linePoint1.y() == linePoint2.y()) { // No fuzzy comparison necessary.
      Preconditions.checkArgument(point.y() == linePoint1.y(), "point should have the same Y as the line.");

      double minX = Math.min(linePoint1.x(), linePoint2.x());
      double maxX = Math.max(linePoint1.x(), linePoint2.x());

      // Returns true if point.getX() is in the range [minX, maxX], with fuzzy math.
      return DoubleMath.fuzzyCompare(minX, point.x(), TOLERANCE) <= 0
          && DoubleMath.fuzzyCompare(point.x(), maxX, TOLERANCE) <= 0;
    } else {
      throw new AssertionError("CAN'T HAPPEN: linePoint1 and linePoint2 are corners on the same side of a "
          + "bounding box; they must have *identical* x or y values.");
    }
  }

  private LatLonRect makeLatLonRect(List<ProjectionPoint> projPoints) {
    if (projPoints.isEmpty()) {
      return LatLonRect.INVALID;
    }

    double minLat = +Double.MAX_VALUE;
    double minLon = +Double.MAX_VALUE;
    double maxLat = -Double.MAX_VALUE;
    double maxLon = -Double.MAX_VALUE;

    for (ProjectionPoint projPoint : projPoints) {
      LatLonPoint latLonPoint = projToLatLon(projPoint);
      Preconditions.checkArgument(latLonPoint != INVALID,
          "We should have filtered out bad points and added good ones. WTF?");

      minLat = Math.min(minLat, latLonPoint.latitude());
      minLon = Math.min(minLon, latLonPoint.longitude());
      maxLat = Math.max(maxLat, latLonPoint.latitude());
      maxLon = Math.max(maxLon, latLonPoint.longitude());
    }

    return new LatLonRect(minLat, minLon, maxLat, maxLon);
  }
}
