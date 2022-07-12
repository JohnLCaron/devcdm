/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.ucdm.dataset.geoloc.projection;

import dev.ucdm.array.NumericCompare;
import dev.ucdm.dataset.geoloc.LatLonPoint;
import dev.ucdm.dataset.geoloc.ProjectionPoint;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;

public class TestEquidistantCylindrical {
  static final double TOL = NumericCompare.defaultMaxRelativeDiffFloat;

  @Test
  public void testBasics() {
    EquidistantCylindrical lc = new EquidistantCylindrical(0.0, 15.0, 0.0);
    assertThat(lc.isLatLon()).isFalse();
  }

  @Test
  public void testCrossSeam() {
    EquidistantCylindrical lc = new EquidistantCylindrical(0., 0., 45.);
    assertThat(lc.crossSeam(ProjectionPoint.create(50, 50), ProjectionPoint.create(550, 550))).isFalse();
    assertThat(lc.crossSeam(ProjectionPoint.create(-50000, -50000), ProjectionPoint.create(50000, 50000))).isTrue();
  }

  /*
   :lat00 = 74.84433f; // float
  :lon00 = 100.29828f; // float
  :latNxNy = -14.944202f; // float
  :lonNxNy = -20.099197f; // float
  :centralLat = 30.0f; // float
  :centralLon = -139.95041f; // float
  :latDxDy = 30.0f; // float
  :lonDxDy = -139.95041f; // float
  :dyKm = 11.105749f; // float
  :dxKm = 9.6171255f; // float
   */
  @Test
  public void testAwipsSat() {
    int nx = 2400;
    int ny = 900;
    double lat00 = 74.84433f; // float
    double lon00 = 100.29828f; // float
    double latNxNy = -14.944202f; // float
    double lonNxNy = -20.099197f; // float
    double centralLat = 30.0f; // float
    double centralLon = -139.95041f; // float
    double dyKm = 11.105749f; // float
    double dxKm = 9.6171255f; // float

    if (lonNxNy < lon00) {
      lonNxNy += 360.0;
    }

    EquidistantCylindrical eq = new EquidistantCylindrical(centralLat, centralLon, centralLat);
    System.out.printf("%n==== x, y%n");
    System.out.printf("origin = %s%n", eq.latLonToProj(centralLat, centralLon));
    assertThat(eq.latLonToProj(centralLat, centralLon)).isEqualTo(new ProjectionPoint(0, 0));

    ProjectionPoint start = eq.latLonToProj(lat00, lon00);
    ProjectionPoint end = eq.latLonToProj(latNxNy, lonNxNy);
    System.out.printf(" lat00 = %s%n", start);
    System.out.printf("llNxNy = %s%n", end);

    double startx = start.x();
    double starty = start.y();
    double endx = end.x();
    double endy = end.y();

    double dx = (endx - startx) / (nx - 1);
    double dy = (endy - starty) / (ny - 1);

    System.out.printf("dx = %f, expect = %f%n", dx, dxKm);
    System.out.printf("dy = %f, expect = %f%n", dy, dyKm);
    //assertThat(NumericCompare.nearlyEquals(dx, dxKm)).isTrue();
    //assertThat(NumericCompare.nearlyEquals(Math.abs(dy), dyKm)).isTrue();

    System.out.printf("%n==== lat, lon%n");
    System.out.printf("center = %s%n", eq.projToLatLon(0,0));
    System.out.printf("    ul = %s%n", eq.projToLatLon(startx, starty));
    System.out.printf("    lr = %s%n", eq.projToLatLon(startx + dx * (nx-1), starty + dy * (ny-1)));

    assertThat(eq.projToLatLon(0,0)).isEqualTo(new LatLonPoint(centralLat, centralLon));
    assertThat(eq.projToLatLon(startx, starty)).isEqualTo(new LatLonPoint(lat00, lon00));

    LatLonPoint oppCorner = eq.projToLatLon(startx + dx * (nx-1), starty + dy * (ny-1));
    assertThat(oppCorner.nearlyEquals(new LatLonPoint(latNxNy, lonNxNy))).isTrue();

  }
}
