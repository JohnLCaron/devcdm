package dev.ucdm.grib.grib1.record;

import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TestGrib1GdsPredefined {
  @Test
  public void testGrib1GdsPredefined() {
    assertThat(Grib1GdsPredefined.factory(7, 21)).isInstanceOf(Grib1Gds.LatLon.class);
    assertThat(Grib1GdsPredefined.factory(7, 22)).isInstanceOf(Grib1Gds.LatLon.class);
    assertThat(Grib1GdsPredefined.factory(7, 23)).isInstanceOf(Grib1Gds.LatLon.class);
    assertThat(Grib1GdsPredefined.factory(7, 24)).isInstanceOf(Grib1Gds.LatLon.class);
    assertThat(Grib1GdsPredefined.factory(7, 25)).isInstanceOf(Grib1Gds.LatLon.class);
    assertThat(Grib1GdsPredefined.factory(7, 26)).isInstanceOf(Grib1Gds.LatLon.class);
    assertThat(Grib1GdsPredefined.factory(7, 61)).isInstanceOf(Grib1Gds.LatLon.class);
    assertThat(Grib1GdsPredefined.factory(7, 62)).isInstanceOf(Grib1Gds.LatLon.class);
    assertThat(Grib1GdsPredefined.factory(7, 63)).isInstanceOf(Grib1Gds.LatLon.class);
    assertThat(Grib1GdsPredefined.factory(7, 64)).isInstanceOf(Grib1Gds.LatLon.class);
    assertThat(Grib1GdsPredefined.factory(7, 87)).isInstanceOf(Grib1Gds.PolarStereographic.class);

    assertThrows(IllegalArgumentException.class, () -> Grib1GdsPredefined.factory(7, 42));
    assertThrows(IllegalArgumentException.class, () -> Grib1GdsPredefined.factory(8, 21));

    assertThat(Grib1GdsPredefined.factory(7, 63)).isNotEqualTo(Grib1GdsPredefined.factory(7, 64));
    assertThat(Grib1GdsPredefined.factory(7, 87)).isEqualTo(Grib1GdsPredefined.factory(7, 87));

  }

}
