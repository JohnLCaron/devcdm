/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
/*
 * This software is provided by the National Aeronatics and Space
 * Administration Goddard Institute for Space Studies (NASA GISS) for full,
 * free and open release. It is understood by the recipient/user that NASA
 * assumes no liability for any errors contained in the code. Although this
 * software is released without conditions or restrictions in its use, it is
 * expected that appropriate credit be given to its author and to NASA GISS
 * should the software be included by the recipient in acknowledgments or
 * credits in other product development.
 */
package dev.ucdm.dataset.geoloc.projection;

import dev.ucdm.core.constants.CF;
import dev.ucdm.dataset.geoloc.*;

import dev.ucdm.array.Immutable;

/**
 * Rotated-pole longitude-latitude grid.
 *
 * This is probably the same as rotated lat lon, using matrix to do rotation.
 * Follows CF convention with "north_pole lat/lon", whereas RotatedLatLon uses south pole.
 *
 * @author Robert Schmunk
 * @author jcaron
 */
@Immutable
public class RotatedPole extends AbstractProjection {
  private static final double RAD_PER_DEG = Math.PI / 180.;
  private static final double DEG_PER_RAD = 1. / RAD_PER_DEG;

  /* Coordinates of north pole for rotated pole. */
  private final ProjectionPoint northPole;

  /* Y-axis rotation matrix. */
  private final double[][] rotY = new double[3][3];

  /* Z-axis rotation matrix. */
  private final double[][] rotZ = new double[3][3];

  /**
   * Default Constructor, needed for beans.
   */
  public RotatedPole() {
    this(0.0, 0.0);
  }

  public RotatedPole(double northPoleLat, double northPoleLon) {
    super(CF.ROTATED_LATITUDE_LONGITUDE, false);

    northPole = ProjectionPoint.create(northPoleLon, northPoleLat);
    buildRotationMatrices();

    addParameter(CF.GRID_MAPPING_NAME, CF.ROTATED_LATITUDE_LONGITUDE);
    addParameter(CF.GRID_NORTH_POLE_LATITUDE, northPoleLat);
    addParameter(CF.GRID_NORTH_POLE_LONGITUDE, northPoleLon);
  }

  @Override
  public String projectionCoordUnits() {
    return "rad"; // TODO ??
  }

  private void buildRotationMatrices() {
    double betaRad;
    double gammaRad;

    if (northPole.y() == 90.) {
      betaRad = 0.;
      gammaRad = northPole.x() * RAD_PER_DEG;
    } else {
      betaRad = -(90. - northPole.y()) * RAD_PER_DEG;
      gammaRad = (northPole.x() + 180.) * RAD_PER_DEG;
    }

    double cosBeta = Math.cos(betaRad);
    double sinBeta = Math.sin(betaRad);

    double cosGamma = Math.cos(gammaRad);
    double sinGamma = Math.sin(gammaRad);

    rotY[0][0] = cosBeta;
    rotY[0][1] = 0.;
    rotY[0][2] = -sinBeta;
    rotY[1][0] = 0.;
    rotY[1][1] = 1.;
    rotY[1][2] = 0.;
    rotY[2][0] = sinBeta;
    rotY[2][1] = 0.;
    rotY[2][2] = cosBeta;

    rotZ[0][0] = cosGamma;
    rotZ[0][1] = sinGamma;
    rotZ[0][2] = 0.;
    rotZ[1][0] = -sinGamma;
    rotZ[1][1] = cosGamma;
    rotZ[1][2] = 0.;
    rotZ[2][0] = 0.;
    rotZ[2][1] = 0.;
    rotZ[2][2] = 1.;
  }

  public ProjectionPoint getNorthPole() {
    return northPole;
  }

  @Override
  public Projection constructCopy() {
    return new RotatedPole(northPole.y(), northPole.x());
  }

  /**
   * Transform a "real" longitude and latitude to the rotated longitude (X) and
   * rotated latitude (Y).
   */
  @Override
  public ProjectionPoint latLonToProj(LatLonPoint latlon) {
    double lon = latlon.longitude();
    double lat = latlon.latitude();

    double lonRad = Math.toRadians(lon);
    double latRad = Math.toRadians(lat);

    // Lon-lat pair to xyz coordinates on sphere with radius 1
    double[] p0 = {Math.cos(latRad) * Math.cos(lonRad), Math.cos(latRad) * Math.sin(lonRad), Math.sin(latRad)};

    // Rotate around Z-axis
    // double[] p1 = new double[] {
    // rotZ[0][0] * p0[0] + rotZ[0][1] * p0[1] + rotZ[0][2] * p0[2],
    // rotZ[1][0] * p0[0] + rotZ[1][1] * p0[1] + rotZ[1][2] * p0[2],
    // rotZ[2][0] * p0[0] + rotZ[2][1] * p0[1] + rotZ[2][2] * p0[2]};
    double[] p1 = {rotZ[0][0] * p0[0] + rotZ[0][1] * p0[1], rotZ[1][0] * p0[0] + rotZ[1][1] * p0[1], p0[2]};

    // Rotate around Y-axis
    // double[] p2 = new double[] {
    // rotY[0][0] * p1[0] + rotY[0][1] * p1[1] + rotY[0][2] * p1[2],
    // rotY[1][0] * p1[0] + rotY[1][1] * p1[1] + rotY[1][2] * p1[2],
    // rotY[2][0] * p1[0] + rotY[2][1] * p1[1] + rotY[2][2] * p1[2]};
    double[] p2 = {rotY[0][0] * p1[0] + rotY[0][2] * p1[2], p1[1], rotY[2][0] * p1[0] + rotY[2][2] * p1[2]};

    double lonR = LatLonPoints.range180(Math.atan2(p2[1], p2[0]) * DEG_PER_RAD);
    double latR = Math.asin(p2[2]) * DEG_PER_RAD;
    return ProjectionPoint.create(lonR, latR);
  }

  /**
   * Transform a rotated longitude (X) and rotated latitude (Y) to a "real"
   * longitude-latitude pair.
   */
  @Override
  public LatLonPoint projToLatLon(ProjectionPoint ppt) {
    // "x" and "y" input for rotated pole coords are actually a lon-lat pair
    double lonR = LatLonPoints.range180(ppt.x());
    double latR = ppt.y();

    if (Math.abs(latR) > 90.) {
      throw new IllegalArgumentException("ProjectionPoint y must be in range [-90,90].");
    }

    double lonRRad = Math.toRadians(lonR);
    double latRRad = Math.toRadians(latR);

    // Lon-lat pair to xyz coordinates on sphere with radius 1
    double[] p0 = {Math.cos(latRRad) * Math.cos(lonRRad), Math.cos(latRRad) * Math.sin(lonRRad), Math.sin(latRRad)};

    // Inverse rotate around Y-axis (using transpose of Y matrix)
    // final double[] p1 = new double[] {
    // rotY[0][0] * p0[0] + rotY[1][0] * p0[1] + rotY[2][0] * p0[2],
    // rotY[0][1] * p0[0] + rotY[1][1] * p0[1] + rotY[2][1] * p0[2],
    // rotY[0][2] * p0[0] + rotY[1][2] * p0[1] + rotY[2][2] * p0[2]};
    double[] p1 = {rotY[0][0] * p0[0] + rotY[2][0] * p0[2], p0[1], rotY[0][2] * p0[0] + rotY[2][2] * p0[2]};

    // Inverse rotate around Z-axis (using transpose of Z matrix)
    // final double[] p2 = new double[] {
    // rotZ[0][0] * p1[0] + rotZ[1][0] * p1[1] + rotZ[2][0] * p1[2],
    // rotZ[0][1] * p1[0] + rotZ[1][1] * p1[1] + rotZ[2][1] * p1[2],
    // rotZ[0][2] * p1[0] + rotZ[1][2] * p1[1] + rotZ[2][2] * p1[2]};
    double[] p2 = {rotZ[0][0] * p1[0] + rotZ[1][0] * p1[1], rotZ[0][1] * p1[0] + rotZ[1][1] * p1[1], p1[2]};

    double lon = Math.atan2(p2[1], p2[0]) * DEG_PER_RAD;
    double lat = Math.asin(p2[2]) * DEG_PER_RAD;
    return new LatLonPoint(lat, lon);
  }

  public boolean crossSeam(ProjectionPoint pt1, ProjectionPoint pt2) {
    return Math.abs(pt1.x() - pt2.x()) > 270.0;
  }

  @Override
  public String toString() {
    return "RotatedPole{" + "northPole=" + northPole + '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    RotatedPole that = (RotatedPole) o;

    return northPole.equals(that.northPole);
  }

  @Override
  public int hashCode() {
    return northPole.hashCode();
  }
}
