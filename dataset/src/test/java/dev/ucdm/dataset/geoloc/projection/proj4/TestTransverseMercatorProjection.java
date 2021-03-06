package dev.ucdm.dataset.geoloc.projection.proj4;

import org.junit.jupiter.api.Test;
import dev.ucdm.dataset.geoloc.Earth;
import dev.ucdm.dataset.geoloc.LatLonPoint;
import dev.ucdm.dataset.geoloc.Projection;
import dev.ucdm.dataset.geoloc.ProjectionPoint;

public class TestTransverseMercatorProjection {

  static private void test(Projection proj, double[] lat, double[] lon) {
    double[] x = new double[lat.length];
    double[] y = new double[lat.length];
    for (int i = 0; i < lat.length; ++i) {
      LatLonPoint lp = new LatLonPoint(lat[i], lon[i]);
      ProjectionPoint p = proj.latLonToProj(lp);
      System.out.println(lp.latitude() + ", " + lp.longitude() + ": " + p.getX() + ", " + p.getY());
      x[i] = p.getX();
      y[i] = p.getY();
    }
    for (int i = 0; i < lat.length; ++i) {
      ProjectionPoint p = ProjectionPoint.create(x[i], y[i]);
      LatLonPoint lp = proj.projToLatLon(p);
      if ((Math.abs(lp.latitude() - lat[i]) > 1e-5) || (Math.abs(lp.longitude() - lon[i]) > 1e-5)) {
        if (Math.abs(lp.latitude()) > 89.99 && (Math.abs(lp.latitude() - lat[i]) < 1e-5)) {
          // ignore longitude singularities at poles
        } else {
          System.err.print("ERROR:");
        }
      }
      System.out.println("reverse:" + p.getX() + ", " + p.getY() + ": " + lp.latitude() + ", " + lp.longitude());
    }
  }

  @Test
  public void testStuff() {
    // test-code
    Earth e = new Earth(6378.137, 6356.7523142, 0);
    Projection proj = new TransverseMercatorProjection(e, 9., 0., 0.9996, 500.000, 0.);

    double[] lat = {60., 90., 60.};
    double[] lon = {0., 0., 10.};
    test(proj, lat, lon);

    proj = new TransverseMercatorProjection(e, 9., 0., 0.9996, 500., 0.);
    test(proj, lat, lon);
  }

}
