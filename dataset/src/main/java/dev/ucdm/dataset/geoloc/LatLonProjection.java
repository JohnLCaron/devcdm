/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.ucdm.dataset.geoloc;

import dev.ucdm.core.constants.CF;

import dev.ucdm.dataset.geoloc.projection.AbstractProjection;
import org.jetbrains.annotations.Nullable;
import dev.ucdm.array.Immutable;

/**
 * This is the "fake" identity projection where world coord = latlon coord.
 * Topologically its the same as a cylinder tangent to the earth at the equator.
 * The cylinder is cut at the "seam" = centerLon +- 180.
 * Longitude values are always kept in the range [centerLon +-180]
 */
@Immutable
public class LatLonProjection extends AbstractProjection {
  private final double centerLon;
  private final Earth earth;

  @Override
  public Projection constructCopy() {
    return new LatLonProjection(getName(), getEarth(), getCenterLon());
  }

  public LatLonProjection() {
    this(CF.LATITUDE_LONGITUDE, EarthEllipsoid.DEFAULT, 0.0);
  }

  public LatLonProjection(Earth earth) {
    this(CF.LATITUDE_LONGITUDE, earth, 0.0);
  }

  public LatLonProjection(String name, @Nullable Earth earth, double centerLon) {
    super(name, true);
    this.earth = earth == null ? EarthEllipsoid.DEFAULT : earth;
    this.centerLon = centerLon;

    addParameter(CF.GRID_MAPPING_NAME, CF.LATITUDE_LONGITUDE);
    if (this.earth.isSpherical()) {
      addParameter(CF.EARTH_RADIUS, this.earth.getEquatorRadius());
    } else {
      addParameter(CF.SEMI_MAJOR_AXIS, this.earth.getEquatorRadius());
      addParameter(CF.SEMI_MINOR_AXIS, this.earth.getPoleRadius());
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;

    LatLonProjection that = (LatLonProjection) o;
    return Double.compare(that.centerLon, centerLon) == 0;
  }

  @Override
  public int hashCode() {
    int result;
    long temp;
    temp = Double.doubleToLongBits(centerLon);
    result = (int) (temp ^ (temp >>> 32));
    return result;
  }

  @Override
  public String toString() {
    return "LatLonProjection{" + "centerLon=" + centerLon + ", earth=" + earth + '}';
  }

  @Override
  public ProjectionPoint latLonToProj(LatLonPoint latlon) {
    return latLonToProj(latlon, centerLon);
  }

  private ProjectionPoint latLonToProj(LatLonPoint latlon, double centerLon) {
    return ProjectionPoint.create(LatLonPoints.lonNormal(latlon.longitude(), centerLon), latlon.latitude());
  }

  @Override
  public LatLonPoint projToLatLon(ProjectionPoint world) {
    return new LatLonPoint(world.x(), world.y());
  }

  /** Get the center of the Longitude range. It is normalized to +/- 180. */
  public double getCenterLon() {
    return centerLon;
  }

  /** Get the Earth used in this projection. */
  public Earth getEarth() {
    return earth;
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
    return Math.abs(pt1.x() - pt2.x()) > 270.0;
  }

  @Override
  public LatLonRect projToLatLonBB(ProjectionRect world) {
    double startLat = world.getMinY();
    double startLon = world.getMinX();

    double deltaLat = world.getHeight();
    double deltaLon = world.getWidth();

    LatLonPoint llpt = new LatLonPoint(startLat, startLon);
    return LatLonRect.builder(llpt, deltaLat, deltaLon).build();
  }


  // Override so we can adjust centerLon to center of the rectangle.
  @Override
  public ProjectionRect latLonToProjBB(LatLonRect latlonRect) {
    double centerLon = latlonRect.getCenterLon();

    LatLonPoint ll = latlonRect.getLowerLeftPoint();
    LatLonPoint ur = latlonRect.getUpperRightPoint();
    ProjectionPoint w1 = latLonToProj(ll, centerLon);
    ProjectionPoint w2 = latLonToProj(ur, centerLon);

    // make bounding box out of those two corners
    ProjectionRect.Builder world = ProjectionRect.builder(w1.x(), w1.y(), w2.x(), w2.y());

    LatLonPoint la = new LatLonPoint(ur.latitude(), ll.longitude());
    LatLonPoint lb = new LatLonPoint(ll.latitude(), ur.longitude());

    // now extend if needed to the other two corners
    world.add(latLonToProj(la, centerLon));
    world.add(latLonToProj(lb, centerLon));

    return world.build();
  }

  /**
   * Split a latlon rectangle to the equivalent ProjectionRect(s).
   * using this LatLonProjection to split it at the seam if needed.
   *
   * @param latlonR the latlon rectangle to transform
   * @return 1 or 2 ProjectionRect. If it doesnt cross the seam,
   *         the second rectangle is null.
   *         see {@link #latLonToProjRect(double, double, double, double)}
   */
  public ProjectionRect[] latLonToProjRect(LatLonRect latlonR) {
    double lat0 = latlonR.getLowerLeftPoint().latitude();
    double height = Math.abs(latlonR.getUpperRightPoint().latitude() - lat0);
    double width = latlonR.getWidth();
    double lon0 = LatLonPoints.lonNormal(latlonR.getLowerLeftPoint().longitude(), centerLon);

    ProjectionRect[] rects = new ProjectionRect[2];
    if (lon0 + width <= centerLon + 180) {
      rects[0] = ProjectionRect.builder().setRect(lon0, lat0, width, height).build();
      rects[1] = null;
    } else {
      double width1 = centerLon + 180 - lon0;
      rects[0] = ProjectionRect.builder().setRect(lon0, lat0, width1, height).build();
      rects[1] = ProjectionRect.builder().setRect(centerLon - 180, lat0, width - width1, height).build();
    }

    return rects;
  }

  /**
   * Create a latlon rectangle from two points. The ending point is always to the east of the
   * starting point. The longitudes do not have to be normalized to +-180.
   *
   * Using the center longitude of this projection, split into 2 rectangles if the rectangle crosses
   * the seam = centerLon + 180.
   *
   * @param lat0 lat of starting point
   * @param lon0 lon of starting point, unnormalized
   * @param lat1 lat of ending point
   * @param lon1 lon of ending point, unnormalized
   * @return 1 or 2 ProjectionRect. If it doesnt cross the seam, the second rectangle is null.
   */
  public ProjectionRect[] latLonToProjRect(double lat0, double lon0, double lat1, double lon1) {
    LatLonRect rect = new LatLonRect(lat0, lon0, lat1, lon1);
    return latLonToProjRect(rect);
  }

}
