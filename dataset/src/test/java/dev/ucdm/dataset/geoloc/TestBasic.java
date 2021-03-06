/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.ucdm.dataset.geoloc;

import dev.ucdm.core.util.Format;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

import static com.google.common.truth.Truth.assertThat;

/** Test basic projection methods */
public class TestBasic {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final boolean debug1 = false;

  //////////////////// testLatLonNormal ///////////////////////////

  void showLatLonNormal(double lon, double center) {
    System.out.println(
        Format.formatDouble(lon, 8, 5) + " => " + Format.formatDouble(LatLonPoints.lonNormal(lon, center), 8, 5));
  }

  void runCenter(double center) {
    for (double lon = 0.0; lon < 380.0; lon += 22.5) {
      if (debug1)
        showLatLonNormal(lon, center);
      double result = LatLonPoints.lonNormal(lon, center);
      assertThat(result).isAtLeast(center - 180.);
      assertThat(result).isAtMost(center + 180.);
      assertThat((result == lon) || (Math.abs(result - lon) == 360) || (Math.abs(result - lon) == 720)).isTrue();
    }
  }

  @Test
  public void testLatLonNormal() {
    runCenter(10.45454545454547);
    runCenter(110.45454545454547);
    runCenter(210.45454545454547);
    runCenter(-10.45454545454547);
    runCenter(-110.45454545454547);
    runCenter(-210.45454545454547);
    runCenter(310.45454545454547);
  }

}
