package dev.cdm.dataset.geoloc;

import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;

public class TestBearing {

  @Test
  public void testBearing() {
    LatLonPoint pt1 = new LatLonPoint(40, -105);
    LatLonPoint pt2 = new LatLonPoint(37.4, -118.4);
    Bearing b = Bearing.calculateBearing(pt1, pt2);
    assertThat(b).isEqualTo(new Bearing(260.35727464982597, 71.9534539707052, 1199.5844426951687));

    LatLonPoint pt3 = Bearing.findPoint(pt1, b.azimuth(), b.distance());
    assertThat(pt3).isEqualTo(new LatLonPoint(37.40000000000029, -118.39999999999851));

    LatLonPoint pt4 = Bearing.findPoint(pt2, b.backazimuth(), b.distance());
    assertThat(pt4).isEqualTo(new LatLonPoint(39.99999999999967, -105.0000000000029));

    Bearing b2 = Bearing.calculateBearing(pt1, pt1);
    assertThat(b2).isEqualTo(new Bearing(0, 0, 0));

    LatLonPoint pt5 = Bearing.findPoint(40.0, -105, 12.0, 0.0);
    assertThat(pt5).isEqualTo(new LatLonPoint(40.0, -105));
  }

}
