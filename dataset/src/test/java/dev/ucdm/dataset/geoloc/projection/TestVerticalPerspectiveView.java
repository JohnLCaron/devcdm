package dev.ucdm.dataset.geoloc.projection;

import org.junit.jupiter.api.Test;
import dev.ucdm.dataset.geoloc.LatLonPoint;
import dev.ucdm.dataset.geoloc.LatLonRect;
import dev.ucdm.dataset.geoloc.ProjectionPoint;
import dev.ucdm.dataset.geoloc.ProjectionRect;
import dev.ucdm.dataset.geoloc.projection.sat.VerticalPerspectiveView;

public class TestVerticalPerspectiveView {

  private static void test(double lat, double lon) {
    double radius = 6371.0;

    VerticalPerspectiveView a = new VerticalPerspectiveView(0, 0, radius, 5.62 * radius);
    ProjectionPoint p = a.latLonToProj(lat, lon);
    System.out.println("-----\nproj point = " + p);
    System.out.println("x/r = " + p.getX() / radius); // see snyder p 174
    System.out.println("y/r = " + p.getY() / radius);

    LatLonPoint ll = a.projToLatLon(p);
    System.out.println(" lat = " + ll.latitude() + " should be= " + lat);
    System.out.println(" lon = " + ll.longitude() + " should be= " + lon);
  }

  @Test
  public void testStuff() {
    double radius = 6371.0;
    double height = 35747.0;

    VerticalPerspectiveView a = new VerticalPerspectiveView(0, 0, radius, height);

    double limit = .99 * Math.sqrt((a.getP() - 1) / (a.getP() + 1));
    System.out.println(" limit = " + limit);
    System.out.println(" limit*90 = " + limit * 90);

    LatLonRect rect = LatLonRect.builder(new LatLonPoint(-45.0, -45.0), -45.0, -45.0).build();
    ProjectionRect r = a.latLonToProjBB(rect);
    System.out.println(" ProjectionRect result = " + r);
  }

}
