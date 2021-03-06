/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package dev.ucdm.dataset.geoloc.projection;

import dev.ucdm.dataset.geoloc.CurvilinearProjection;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import dev.ucdm.array.NumericCompare;
import dev.ucdm.dataset.geoloc.Earth;
import dev.ucdm.dataset.geoloc.EarthEllipsoid;
import dev.ucdm.dataset.geoloc.LatLonPoint;
import dev.ucdm.dataset.geoloc.LatLonPoints;
import dev.ucdm.dataset.geoloc.Projection;
import dev.ucdm.dataset.geoloc.ProjectionPoint;
import dev.ucdm.dataset.geoloc.projection.proj4.AlbersEqualAreaEllipse;
import dev.ucdm.dataset.geoloc.projection.proj4.CylindricalEqualAreaProjection;
import dev.ucdm.dataset.geoloc.projection.proj4.EquidistantAzimuthalProjection;
import dev.ucdm.dataset.geoloc.projection.proj4.LambertConformalConicEllipse;
import dev.ucdm.dataset.geoloc.projection.proj4.PolyconicProjection;
import dev.ucdm.dataset.geoloc.projection.proj4.StereographicAzimuthalProjection;
import dev.ucdm.dataset.geoloc.projection.proj4.TransverseMercatorProjection;
import dev.ucdm.dataset.geoloc.projection.sat.Geostationary;
import dev.ucdm.dataset.geoloc.projection.sat.MSGnavigation;
import dev.ucdm.dataset.geoloc.projection.sat.VerticalPerspectiveView;

import static com.google.common.truth.Truth.assertThat;

/** Test the standard methods of Projections. */
public class TestProjections {
  private static final boolean show = false;
  private static final int NTRIALS = 10000;
  private static final double tolerence = 5.0e-4;

  @Test
  public void testAlbersEqualArea() {
    doProjection(new AlbersEqualArea());
    AlbersEqualArea p = new AlbersEqualArea();
    testEquals(p);
  }

  @Test
  public void testCurvilinear() {
    doProjectionLatLonMax(new CurvilinearProjection(), 180, 90, false);
    doProjectionProjMax(new CurvilinearProjection(), 180, 90);
    CurvilinearProjection p = new CurvilinearProjection();
    testEquals(p);
  }

  @Test
  public void testEquidistantAzimuthalProjection() {
    doProjection(new EquidistantAzimuthalProjection());
    EquidistantAzimuthalProjection p = new EquidistantAzimuthalProjection();
    testEquals(p);
  }

  @Test
  public void testFlatEarth() {
    doProjectionProjMax(new FlatEarth(), 5000, 5000);
    FlatEarth p = new FlatEarth();
    testEquals(p);
  }

  /*
   * grid_south_pole_latitude = -30.000001907348633
   * grid_south_pole_longitude = -15.000000953674316
   * grid_south_pole_angle = 0.0
   */
  @Test
  public void testRotatedLatLon() {
    doProjectionLatLonMax(new GribRotatedLatLon(-30, -15, 0), 360, 88, false);
    GribRotatedLatLon p = new GribRotatedLatLon();
    testEquals(p);
  }

  @Test
  public void testLambertAzimuthalEqualArea() {
    doProjection(new LambertAzimuthalEqualArea());
    LambertAzimuthalEqualArea p = new LambertAzimuthalEqualArea();
    testEquals(p);
  }

  @Test
  public void testLambertConformal() {
    doProjection(new LambertConformal());
    LambertConformal p = new LambertConformal();
    testEquals(p);
  }

  @Test
  public void testLCseam() {
    // test seam crossing
    LambertConformal lc = new LambertConformal(40.0, 180.0, 20.0, 60.0);
    ProjectionPoint p1 = lc.latLonToProj(new LatLonPoint(0.0, -1.0));
    ProjectionPoint p2 = lc.latLonToProj(new LatLonPoint(0.0, 1.0));
    if (show) {
      System.out.printf(" p1= x=%f y=%f%n", p1.getX(), p1.getY());
      System.out.printf(" p2= x=%f y=%f%n", p2.getX(), p2.getY());
    }
    assertThat(lc.crossSeam(p1, p2)).isTrue();
  }

  @Test
  public void testMercator() {
    doProjection(new Mercator());
    Mercator p = new Mercator();
    testEquals(p);
  }

  @Test
  public void testOrthographic() {
    doProjection(new Orthographic());
    Orthographic p = new Orthographic();
    testEquals(p);
  }

  @Test
  public void testRotatedPole() {
    doProjectionLatLonMax(new RotatedPole(37, 177), 360, 88, false);
    RotatedPole p = new RotatedPole();
    testEquals(p);
  }

  @Test
  public void testSinusoidal() {
    doOne(new Sinusoidal(0, 0, 0, 6371.007), 20, 40, true);
    doProjection(new Sinusoidal(0, 0, 0, 6371.007));
    Sinusoidal p = new Sinusoidal();
    testEquals(p);
  }

  @Test
  public void testStereographic() {
    doProjection(new Stereographic());
    Stereographic p = new Stereographic();
    testEquals(p);
  }

  @Test
  public void testTransverseMercator() {
    doProjection(new TransverseMercator());
    TransverseMercator p = new TransverseMercator();
    testEquals(p);
  }

  @Test
  // java.lang.AssertionError: .072111263S 165.00490E expected:<-0.07211126381547306> but was:<39.99999999999999>
  public void testTMproblem() {
    double lat = -.072111263;
    double lon = 165.00490;
    LatLonPoint endL = doOne(new TransverseMercator(), lat, lon, true);
    if (endL.equals(LatLonPoint.INVALID))
      return;
    assertThat(lat).isWithin(tolerence).of(endL.latitude());
    assertThat(lon).isWithin(tolerence).of(endL.longitude());
  }

  @Test
  public void testUTM() {
    // The central meridian = (zone * 6 - 183) degrees, where zone in [1,60].
    // zone = (lon + 183)/6
    // 33.75N 15.25E end = 90.0N 143.4W
    // doOne(new UtmProjection(10, true), 33.75, -122);
    testProjectionUTM(-12.89, .07996);

    testProjectionUTM(NTRIALS);

    UtmProjection p = new UtmProjection();
    testEquals(p);
  }

 /////////////////////////////////////////////////////////////////////////
 // proj4

  @Test
  public void testAlbersEqualAreaEllipse() {
    doProjectionLatLonMax(new AlbersEqualAreaEllipse(), 180, 80, true);
    AlbersEqualAreaEllipse p = new AlbersEqualAreaEllipse();
    testEquals(p);
  }


  @Test
  public void testCylindricalEqualAreaProjection() {
    doProjection(new CylindricalEqualAreaProjection());
    CylindricalEqualAreaProjection p = new CylindricalEqualAreaProjection();
    testEquals(p);
  }

  @Test
  public void testLambertConformalConicEllipse() {
    doProjectionLatLonMax(new LambertConformalConicEllipse(), 360, 80, true);
    LambertConformalConicEllipse p = new LambertConformalConicEllipse();
    testEquals(p);
  }

  @Disabled("fails")
  @Test
  public void testPolyconicProjection() {
    doProjection(new PolyconicProjection());
    PolyconicProjection p = new PolyconicProjection();
    testEquals(p);
  }

  @Test
  public void testStereographicAzimuthalProjection() {
    doProjection(new StereographicAzimuthalProjection());
    StereographicAzimuthalProjection p = new StereographicAzimuthalProjection();
    testEquals(p);
  }

  @Disabled("fails")
  @Test
  public void testTransverseMercatorProjection() {
    doProjectionLatLonMax(new TransverseMercatorProjection(), 45, 30, true);
    TransverseMercatorProjection p = new TransverseMercatorProjection();
    testEquals(p);
  }

  /////////////////////////////////////////////////////////////////////////////////////////
  // sat

  @Disabled("fails")
  @Test
  public void testGeostationary() {
    doProjection(new Geostationary());
    Geostationary p = new Geostationary();
    testEquals(p);
  }

  @Test
  public void testMSGnavigation() {
    doOne(new MSGnavigation(), 60, 60, true);
    doProjection(new MSGnavigation());
    testEquals(new MSGnavigation());

    MSGnavigation m = new MSGnavigation();
    showProjVal(m, 0, 0);
    showProjVal(m, 60, 0);
    showProjVal(m, -60, 0);
    showProjVal(m, 0, 60);
    showProjVal(m, 0, -60);
  }

  @Test
  public void testVerticalPerspectiveView() {
    doProjection(new VerticalPerspectiveView());
    VerticalPerspectiveView p = new VerticalPerspectiveView();
    testEquals(p);
  }

  ///////////////////////////////////////////////////////////////////////////////////

  private LatLonPoint doOne(Projection proj, double lat, double lon, boolean show) {
    LatLonPoint startL = new LatLonPoint(lat, lon);
    ProjectionPoint p = proj.latLonToProj(startL);
    if (Double.isNaN(p.getX()) || Double.isNaN(p.getY()))
      return LatLonPoint.INVALID;
    if (Double.isInfinite(p.getX()) || Double.isInfinite(p.getY()))
      return LatLonPoint.INVALID;
    LatLonPoint endL = proj.projToLatLon(p);

    if (show) {
      System.out.println("start  = " + LatLonPoints.toString(startL, 8));
      System.out.println("projection point  = " + p);
      System.out.println("end  = " + endL.toString());
    }
    return endL;
  }

  private void doProjection(Projection proj) {
    java.util.Random r = new java.util.Random(this.hashCode());

    int countT1 = 0;
    for (int i = 0; i < NTRIALS; i++) {
      // random latlon point
      LatLonPoint startL = new LatLonPoint(180.0 * (r.nextDouble() - .5), 360.0 * (r.nextDouble() - .5));

      ProjectionPoint p = proj.latLonToProj(startL);
      if (Double.isNaN(p.getX()) || Double.isNaN(p.getY()))
        continue;
      LatLonPoint endL = proj.projToLatLon(p);
      if (Double.isNaN(endL.latitude()) || Double.isNaN(endL.longitude()) || endL.equals(LatLonPoint.INVALID))
        continue;

      assertThat(startL.latitude()).isWithin(1.0e-3).of(endL.latitude());
      assertThat(startL.longitude()).isWithin(1.0e-3).of(endL.longitude());
      countT1++;
    }

    int countT2 = 0;
    for (int i = 0; i < NTRIALS; i++) {
      ProjectionPoint startP = ProjectionPoint.create(10000.0 * (r.nextDouble() - .5), // random proj point
          10000.0 * (r.nextDouble() - .5));

      LatLonPoint ll = proj.projToLatLon(startP);
      if (Double.isNaN(ll.latitude()) || Double.isNaN(ll.longitude()))
        continue;
      ProjectionPoint endP = proj.latLonToProj(ll);
      if (Double.isNaN(endP.getX()) || Double.isNaN(endP.getY()))
        continue;

      assertThat(startP.getX()).isWithin(tolerence).of(endP.getX());
      assertThat(startP.getY()).isWithin(tolerence).of(endP.getY());
      countT2++;
    }
    if (show)
      System.out.printf("Tested %d, %d pts for projection %s %n", countT1, countT2, proj.getClassName());
  }

  // must have lon within +/- lonMax, lat within +/- latMax
  private void doProjectionLatLonMax(Projection proj, double lonMax, double latMax, boolean show) {
    java.util.Random r = new java.util.Random(this.hashCode());

    double minx = Double.MAX_VALUE;
    double maxx = -Double.MAX_VALUE;
    double miny = Double.MAX_VALUE;
    double maxy = -Double.MAX_VALUE;

    int countFail = 0;
    if (show) System.out.printf("projection %s%n", proj);
    for (int i = 0; i < NTRIALS; i++) {
      // random latlon point between +- latMax, +- lonMAx
      LatLonPoint startL = new LatLonPoint(latMax * (2 * r.nextDouble() - 1), lonMax * (2 * r.nextDouble() - 1));
      assertThat(Math.abs(startL.latitude())).isLessThan(latMax);
      assertThat(Math.abs(startL.longitude())).isLessThan(lonMax);

      // roundtrip
      ProjectionPoint p = proj.latLonToProj(startL);
      LatLonPoint endL = proj.projToLatLon(p);
      if (show) System.out.printf("  ll %f, %f -> pt %f, %f -> %f, %f%n",
              startL.latitude(), startL.longitude(), p.x(), p.y(), endL.latitude(), endL.longitude());

      assertThat(endL.nearlyEquals(startL, tolerence)).isTrue();

      minx = Math.min(minx, p.getX());
      maxx = Math.max(maxx, p.getX());
      miny = Math.min(miny, p.getY());
      maxy = Math.max(maxy, p.getY());
    }

    if (show)
      System.out.printf("Failed %d/%d for projection %s%n", countFail, NTRIALS, proj.getClassName());
  }

  // must have lon within +/- lonMax, lat within +/- latMax
  private void doProjectionInRange(Projection proj, double minx, double maxx, double miny, double maxy, boolean show) {
    java.util.Random r = new java.util.Random(this.hashCode());

    double rangex = maxx - minx;
    double rangey = maxy - miny;

    int countFail = 0;
    if (show) System.out.printf("  proj %s%n", proj);

    for (int i = 0; i < NTRIALS; i++) {
      double x = minx + rangex * r.nextDouble();
      double y = miny + rangey * r.nextDouble();
      assertThat(x).isGreaterThan(minx);
      assertThat(x).isLessThan(maxx);
      assertThat(y).isGreaterThan(miny);
      assertThat(y).isLessThan(maxy);

      ProjectionPoint startP = ProjectionPoint.create(x, y);
      if (show) System.out.println("startP  = " + startP);

      try {
        LatLonPoint ll = proj.projToLatLon(startP);
        ProjectionPoint endP = proj.latLonToProj(ll);

        if (!NumericCompare.nearlyEquals(endP.getX(), endP.getX()) ||
                !NumericCompare.nearlyEquals(endP.getY(), endP.getY())) {
          System.out.println("****\n start  = " + startP);
          System.out.println(" interL  = " + ll);
          System.out.println(" end  = " + endP);
        }

        // assertThat(endP.getX()).isWithin(tolerence).of(startP.getX());
        // assertThat(endP.getY()).isWithin(tolerence).of(startP.getY());
      } catch (IllegalArgumentException e) {
        System.out.printf("IllegalArgumentException=%s%n", e.getMessage());
        // for now, just swallow
        countFail++;
      }
    }

    if (show)
      System.out.printf("Failed %d/%d for projection %s%n", countFail, NTRIALS, proj.getClassName());
  }

  // must have x within +/- xMax, y within +/- yMax
  private void doProjectionProjMax(Projection proj, double xMax, double yMax) {
    java.util.Random r = new java.util.Random(this.hashCode());
    for (int i = 0; i < NTRIALS; i++) {
      double x = xMax * (2 * r.nextDouble() - 1);
      double y = yMax * (2 * r.nextDouble() - 1);
      ProjectionPoint startP = ProjectionPoint.create(x, y);
      try {
        LatLonPoint ll = proj.projToLatLon(startP);
        ProjectionPoint endP = proj.latLonToProj(ll);
        if (show) {
          System.out.println("start  = " + startP);
          System.out.println("interL  = " + ll);
          System.out.println("end  = " + endP);
        }

        assertThat(startP.getX()).isWithin(tolerence).of(endP.getX());
        assertThat(startP.getY()).isWithin(tolerence).of(endP.getY());
      } catch (IllegalArgumentException e) {
        System.out.printf("IllegalArgumentException=%s%n", e.getMessage());
      }
    }
    if (show)
      System.out.println("Tested " + NTRIALS + " pts for projection " + proj.getClassName());
  }

  private void testProjectionUTM(double lat, double lon) {
    LatLonPoint startL = new LatLonPoint(lat, lon);
    int zone = (int) ((lon + 183) / 6);
    UtmProjection proj = new UtmProjection(zone, lat >= 0.0);

    ProjectionPoint p = proj.latLonToProj(startL);
    LatLonPoint endL = proj.projToLatLon(p);

    if (show) {
      System.out.println("startL  = " + startL);
      System.out.println("inter  = " + p);
      System.out.println("endL  = " + endL);
    }

    assertThat(startL.latitude()).isWithin(1.3e-4).of(endL.latitude());
    assertThat(startL.longitude()).isWithin(1.3e-4).of(endL.longitude());
  }

  private void testProjectionUTM(int n) {
    java.util.Random r = new java.util.Random((long) this.hashCode());
    for (int i = 0; i < n; i++) {
      // random latlon point
      LatLonPoint startL = new LatLonPoint(180.0 * (r.nextDouble() - .5), 360.0 * (r.nextDouble() - .5));
      double lat = startL.latitude();
      double lon = startL.longitude();
      int zone = (int) ((lon + 183) / 6);
      UtmProjection proj = new UtmProjection(zone, lat >= 0.0);

      ProjectionPoint p = proj.latLonToProj(startL);
      LatLonPoint endL = proj.projToLatLon(p);

      if (show) {
        System.out.println("startL  = " + startL);
        System.out.println("inter  = " + p);
        System.out.println("endL  = " + endL);
      }

      assertThat(startL.latitude()).isWithin(1.0e-4).of(endL.latitude());
      assertThat(startL.longitude()).isWithin(.02).of(endL.longitude());
    }

    if (show) {
      System.out.println("Tested " + n + " pts for UTM projection ");
    }
  }

  @Test
  // Test known values from the port to 6. It was sad how many mistakes I made without test failure.
  // These values are gotten from the 5.X version, before porting.
  public void makeSanityTest() {
    ProjectionPoint ppt = ProjectionPoint.create(-4000, -2000);
    LatLonPoint lpt = new LatLonPoint(11, -22);

    AlbersEqualArea albers = new AlbersEqualArea();
    test(albers, ppt, new LatLonPoint(-2.99645329, -126.78340473));
    test(albers, lpt, ProjectionPoint.create(7858.99117, 1948.34216));

    FlatEarth flat = new FlatEarth();
    test(flat, ppt, new LatLonPoint(-17.98578564, -37.81970091));
    test(flat, lpt, ProjectionPoint.create(-2401.42949, 1223.18815));

    LambertAzimuthalEqualArea lea = new LambertAzimuthalEqualArea();
    test(lea, ppt, new LatLonPoint(15.02634094, 62.50447310));
    test(lea, lpt, ProjectionPoint.create(-8814.26563, 5087.96966));

    LambertConformal lc = new LambertConformal();
    test(lc, ppt, new LatLonPoint(13.70213773, -141.55784873));
    test(lc, lpt, ProjectionPoint.create(8262.03211, 1089.40638));

    Mercator merc = new Mercator();
    test(merc, ppt, new LatLonPoint(-18.79370670, -143.28014659));
    test(merc, lpt, ProjectionPoint.create(8672.90304, 1156.54768));

    Orthographic orth = new Orthographic();
    test(orth, ppt, new LatLonPoint(-18.29509511, -41.39503231));
    test(orth, lpt, ProjectionPoint.create(-2342.85390, 1215.68780));

    GribRotatedLatLon rll = new GribRotatedLatLon();
    test(rll, ppt, new LatLonPoint(-46.04179300, 119.52015163));
    test(rll, lpt, ProjectionPoint.create(-62.5755691, -65.5259331));

    RotatedPole rpole = new RotatedPole();
    test(rpole, ProjectionPoint.create(-105, -40), new LatLonPoint(-11.43562982, 130.98084177));
    test(rpole, lpt, ProjectionPoint.create(62.5755691, 65.5259331));

    Sinusoidal ss = new Sinusoidal();
    test(ss, ppt, new LatLonPoint(-17.98578564, -37.81970091));
    test(ss, lpt, ProjectionPoint.create(-2401.42949, 1223.18815));

    Stereographic stereo = new Stereographic(90.0, 255.0, 0.9330127018922193, 0, 0, 6371229.0);
    test(stereo, ppt, new LatLonPoint(89.95689508, -168.43494882));
    test(stereo, lpt, ProjectionPoint.create(9727381.45, -1194372.26));

    UtmProjection utm = new UtmProjection();
    test(utm, ppt, new LatLonPoint(-14.20675125, 168.02702665));
    test(utm, lpt, ProjectionPoint.create(39757.9030, 24224.2087));

    VerticalPerspectiveView vv = new VerticalPerspectiveView();
    test(vv, ppt, new LatLonPoint(-19.41473686, -44.82061281));
    test(vv, lpt, ProjectionPoint.create(-2305.97999, 1196.55423));
  }

  @Test
  public void makeSatSanityTest() {
    ProjectionPoint ppt = ProjectionPoint.create(1000, -1000);
    LatLonPoint lpt = new LatLonPoint(11, -22);

    Geostationary geo = new Geostationary();
    test(geo, ProjectionPoint.create(-.07, .04), new LatLonPoint(13.36541946, -24.35896030));
    test(geo, lpt, ProjectionPoint.create(-.0644264897, .0331714453));

    MSGnavigation msg = new MSGnavigation();
    test(msg, ppt, new LatLonPoint(9.12859974, 9.18008048));
    test(msg, lpt, ProjectionPoint.create(-2305.57189, -1187.00138));
  }

  @Test
  public void makeProj4SanityTest() {
    ProjectionPoint ppt = ProjectionPoint.create(999, 666);
    LatLonPoint lpt = new LatLonPoint(11.1, -222);

    AlbersEqualAreaEllipse aea = new AlbersEqualAreaEllipse();
    test(aea, ppt, new LatLonPoint(28.58205667, -85.79021175));
    test(aea, lpt, ProjectionPoint.create(-10854.6125, 7215.68490));

    AlbersEqualAreaEllipse aeaSpherical = new AlbersEqualAreaEllipse(23.0, -96.0, 29.5, 45.5, 0, 0, new Earth());
    test(aeaSpherical, ppt, new LatLonPoint(28.56099715, -85.77391141));
    test(aeaSpherical, lpt, ProjectionPoint.create(-10846.3062, 7202.23178));

    CylindricalEqualAreaProjection cea = new CylindricalEqualAreaProjection();
    test(cea, ppt, new LatLonPoint(6.03303479, 8.97552756));
    test(cea, lpt, ProjectionPoint.create(15359.7656, 1220.09762));

    CylindricalEqualAreaProjection ceaSpherical = new CylindricalEqualAreaProjection(0, 1, 0, 0, new Earth());
    test(ceaSpherical, ppt, new LatLonPoint(5.99931086, 8.98526842));
    test(ceaSpherical, lpt, ProjectionPoint.create(15343.1142, 1226.78838));

    makeEquidistantAzimuthalProjectionTest(0, false, new LatLonPoint(5.99817760, 9.00687785),
        ProjectionPoint.create(4612.89929, 1343.46918));
    makeEquidistantAzimuthalProjectionTest(45, false, new LatLonPoint(50.18393050, 14.04526295),
        ProjectionPoint.create(5360.13920, 5340.66199));
    makeEquidistantAzimuthalProjectionTest(90, false, new LatLonPoint(79.24928617, 123.69006753),
        ProjectionPoint.create(5871.24515, 6520.67834));
    makeEquidistantAzimuthalProjectionTest(-90, false, new LatLonPoint(-79.24928617, 56.30993247),
        ProjectionPoint.create(7513.99763, -8345.13980));

    makeEquidistantAzimuthalProjectionTest(0, true, new LatLonPoint(5.96464789, 9.01660332),
        ProjectionPoint.create(14599.9299, 4280.76724));
    makeEquidistantAzimuthalProjectionTest(45, true, new LatLonPoint(50.18063010, 14.08788032),
        ProjectionPoint.create(8862.91638, 8797.76181));
    makeEquidistantAzimuthalProjectionTest(90, true, new LatLonPoint(79.20269606, 123.69006753),
        ProjectionPoint.create(5870.68098, 6520.05176));
    makeEquidistantAzimuthalProjectionTest(-90, true, new LatLonPoint(-79.20269606, 56.30993247),
        ProjectionPoint.create(7522.50757, -8354.59105));

    LambertConformalConicEllipse lcc = new LambertConformalConicEllipse();
    test(lcc, ppt, new LatLonPoint(28.45959294, -85.80653902));
    test(lcc, lpt, ProjectionPoint.create(-10921.9485, 7293.53362));

    PolyconicProjection pc = new PolyconicProjection();
    test(pc, ppt, new LatLonPoint(29.15559062, 86.84046566));
    test(pc, lpt, ProjectionPoint.create(6658.86657, -695.508749));

    makeStereographicAzimuthalProjectionTest(0, false, new LatLonPoint(0, 0), ProjectionPoint.create(0, 2297.78968));
    makeStereographicAzimuthalProjectionTest(45, false, new LatLonPoint(50.47225526, 15.09952044),
        ProjectionPoint.create(11084.6954, 2585.73594));
    makeStereographicAzimuthalProjectionTest(90, false, new LatLonPoint(78.51651479, 123.69006753),
        ProjectionPoint.create(6540.11179, 7263.53001));
    makeStereographicAzimuthalProjectionTest(-90, false, new LatLonPoint(-78.51651479, 56.30993247),
        ProjectionPoint.create(9633.88149, -10699.5093));

    makeStereographicAzimuthalProjectionTest(0, true, new LatLonPoint(6.36756802, 9.63623417),
        ProjectionPoint.create(.0, 1155.24055));
    makeStereographicAzimuthalProjectionTest(45, true, new LatLonPoint(50.46642756, 15.15040679),
        ProjectionPoint.create(11070.7987, 2591.21171));
    makeStereographicAzimuthalProjectionTest(90, true, new LatLonPoint(78.46658754, 123.69006753),
        ProjectionPoint.create(6546.11795, 7270.20052));
    makeStereographicAzimuthalProjectionTest(-90, true, new LatLonPoint(-78.46658754, 56.30993247),
        ProjectionPoint.create(9667.61835, -10736.9779));


    TransverseMercatorProjection tm = new TransverseMercatorProjection();
    test(tm, ppt, new LatLonPoint(5.95132079, 8.98953399));
    test(tm, lpt, ProjectionPoint.create(68160.1123, 69441.2299));

    TransverseMercatorProjection tmSpherical = new TransverseMercatorProjection(new Earth(), 0, 0, 0.9996, 0, 0);
    test(tmSpherical, ppt, new LatLonPoint(5.91843536, 8.99922690));
    test(tmSpherical, lpt, ProjectionPoint.create(5009.10324, 18363.9569));
  }

  private void makeEquidistantAzimuthalProjectionTest(double lat0, boolean isSpherical, LatLonPoint expectl,
      ProjectionPoint expectp) {
    ProjectionPoint ppt = ProjectionPoint.create(999, 666);
    LatLonPoint lpt = new LatLonPoint(11.1, -222);

    EquidistantAzimuthalProjection ea =
        new EquidistantAzimuthalProjection(lat0, 0, 0, 0, isSpherical ? new Earth() : EarthEllipsoid.WGS84);
    test(ea, ppt, expectl);
    test(ea, lpt, expectp);
  }

  private void makeStereographicAzimuthalProjectionTest(double lat0, boolean isSpherical, LatLonPoint expectl,
      ProjectionPoint expectp) {
    ProjectionPoint ppt = ProjectionPoint.create(999, 666);
    LatLonPoint lpt = new LatLonPoint(11.1, -222);

    StereographicAzimuthalProjection ea = new StereographicAzimuthalProjection(lat0, 0.0, 0.9330127018922193, 60., 0, 0,
        isSpherical ? new Earth() : EarthEllipsoid.WGS84);
    test(ea, ppt, expectl);
    test(ea, lpt, expectp);
  }

  private void test(Projection p, ProjectionPoint ppt, LatLonPoint expect) {
    System.out.printf("%s%n", p);
    LatLonPoint lpt = p.projToLatLon(ppt);
    System.out.printf("  projToLatLon %s -> %s%n", LatLonPoints.toString(ppt, 9), LatLonPoints.toString(lpt, 9));
    assertThat(NumericCompare.nearlyEquals(lpt.latitude(), expect.latitude())).isTrue();
    assertThat(NumericCompare.nearlyEquals(lpt.longitude(), expect.longitude())).isTrue();
  }

  private void test(Projection p, LatLonPoint lpt, ProjectionPoint expect) {
    ProjectionPoint ppt = p.latLonToProj(lpt);
    System.out.printf("  latLonToProj %s -> %s%n", LatLonPoints.toString(lpt, 9), LatLonPoints.toString(ppt, 9));
    assertThat(NumericCompare.nearlyEquals(ppt.getX(), expect.getX())).isTrue();
    assertThat(NumericCompare.nearlyEquals(ppt.getY(), expect.getY())).isTrue();
  }

  private void testEquals(AbstractProjection p) {
    Projection p2 = p.constructCopy();
    assertThat(p).isEqualTo(p2);
    assertThat(p.hashCode()).isEqualTo(p2.hashCode());
    assertThat(p.toString()).isEqualTo(p2.toString());
  }

  private void showProjVal(Projection proj, double lat, double lon) {
    LatLonPoint startL = new LatLonPoint(lat, lon);
    ProjectionPoint p = proj.latLonToProj(startL);
    if (show)
      System.out.printf("lat,lon= (%f, %f) x, y= (%f, %f) %n", lat, lon, p.getX(), p.getY());
  }

  @Test
  public void testPrecedence() {
    // proj has
    // dphi = .5 * com * com / cospi * (qs / Tone_es - sinpi / com + .5 / Te * Math.log((1. - con) / (1. + con)));

    double result1 = .5 / .4 * .3;
    double result2 = .5 / (.4 * .3);
    double result3 = (.5 / .4) * .3;
    assertThat(result1).isEqualTo(result3);
    assertThat(result1).isNotEqualTo(result2);
  }

}
