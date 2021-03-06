package dev.ucdm.dataset.geoloc.projection;

import org.junit.jupiter.api.Test;
import dev.ucdm.dataset.geoloc.LatLonPoint;
import dev.ucdm.dataset.geoloc.LatLonPoints;
import dev.ucdm.dataset.geoloc.ProjectionPoint;

import static com.google.common.truth.Truth.assertWithMessage;

/**
 * Tests for {@link RotatedPole}.
 * 
 * @author Ben Caradoc-Davies (Transient Software Limited)
 */
public class TestRotatedPole {

  /** Tolerance for coordinate comparisons. */
  private static final double TOLERANCE = 1e-6;

  /** A rotated lat/lon projection with origin at 54 degrees North, 254 degrees East. */
  private RotatedPole proj = new RotatedPole(90 - 54, LatLonPoints.lonNormal(254 + 180));

  /** Test that the unrotated centre lat/lon is the origin of the rotated projection. */
  @Test
  public void testLatLonToProj() {
    LatLonPoint latlon = new LatLonPoint(54, 254);
    ProjectionPoint result = proj.latLonToProj(latlon);
    assertWithMessage("Unexpected rotated longitude").that(result.getX()).isWithin(TOLERANCE).of(0);
    assertWithMessage("Unexpected rotated latitude").that(result.getY()).isWithin(TOLERANCE).of(0);
  }

  /** Test that the origin of the rotated projection is the unrotated centre lat/lon. */
  @Test
  public void testProjToLatLon() {
    ProjectionPoint p = ProjectionPoint.create(0, 0);
    LatLonPoint latlonResult = proj.projToLatLon(p);
    assertWithMessage("Unexpected rotated longitude").that(latlonResult.longitude()).isWithin(TOLERANCE)
        .of(LatLonPoints.lonNormal(254));
    assertWithMessage("Unexpected rotated latitude").that(latlonResult.latitude()).isWithin(TOLERANCE).of(54);
  }

}
