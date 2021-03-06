/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.ucdm.dataset.geoloc.projection;

import dev.ucdm.array.NumericCompare;
import org.junit.jupiter.api.Test;
import dev.ucdm.dataset.geoloc.ProjectionPoint;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Some basic tests of the Lambert conformal conic projection class,
 * checking that it accepts or rejects certain projection parameters.
 */
public class TestLambertConformal {
  static final double TOL = NumericCompare.defaultMaxRelativeDiffFloat;

  @Test
  public void testGetScale() {
    LambertConformal lc = new LambertConformal(-90., 0., 45., 45.);
    assertThat(lc.getScale(90.0)).isWithin(TOL).of(73931.862766);
    assertThat(lc.getScale(45.0)).isEqualTo(1.0);
    assertThat(lc.getScale(0.0)).isWithin(TOL).of(1.3187069192749543);
    assertThat(lc.getScale(-45.0)).isWithin(TOL).of(3.47797587788728050);
    assertThat(lc.getScale(-80.0)).isWithin(TOL).of(42.52370772463266);
    assertThat(lc.getScale(-90)).isEqualTo(Double.POSITIVE_INFINITY);
  }

  @Test
  public void testMisc() {
    LambertConformal lc = new LambertConformal(-90., 0., 45., 45.);
    assertThat(lc.toWKS()).contains(
        "PROJCS[\"lambert_conformal_conic\",GEOGCS[\"Normal Sphere (r=6371007)\",DATUM[\"unknown\",SPHEROID[\"sphere\",6371007,0]],PRIMEM[\"Greenwich\",0],UNIT[\"degree\",0.0174532925199433]],PROJECTION[\"Lambert_Conformal_Conic_1SP\"],PARAMETER[\"latitude_of_origin\",-90.0],PARAMETER[\"central_meridian\",0.0],PARAMETER[\"scale_factor\",1],PARAMETER[\"false_easting\",0.0],PARAMETER[\"false_northing\",0.0]");

    assertThat(lc.crossSeam(ProjectionPoint.create(50, 50), ProjectionPoint.create(550, 550))).isFalse();
    assertThat(lc.crossSeam(ProjectionPoint.create(-50000, -50000), ProjectionPoint.create(50000, 50000))).isTrue();
  }

  /** LambertConformal should accept latitude-at-origin that is at either pole. */
  @Test
  public void acceptCenterLatAtAPole() {
    assertThat(new LambertConformal(-90., 0., 45., 45.)).isNotNull();
    assertThat(new LambertConformal(90., 0., 45., 45.)).isNotNull();
  }

  /** LambertConformal should reject latitude-at-origin less than -90. */
  @Test
  public void rejectCenterLatTooNegative() {
    assertThrows(IllegalArgumentException.class, () ->
            new LambertConformal(-91., 0., 30., 45.));
  }

  /**
   * LambertConformal should reject latitude-at-origin greater than +90.
   */
  @Test
  public void rejectCenterLatTooPositive() {
    assertThrows(IllegalArgumentException.class, () ->
            new LambertConformal(+91., 0., 30., 45.));
  }

  /**
   * LambertConformal should reject standard parallel #1 set to -90.
   */
  @Test
  public void rejectParallel1AtSouthPole() {
    assertThrows(IllegalArgumentException.class, () ->
            new LambertConformal(45., 0., -90., -45.));
  }

  /**
   * LambertConformal should reject standard parallel #1 set to +90.
   */
  @Test
  public void rejectParallel1AtNorthPole() {
    assertThrows(IllegalArgumentException.class, () ->
            new LambertConformal(45., 0., 90., 45.));
  }

  /**
   * LambertConformal should reject standard parallel #2 set to -90.
   */
  @Test
  public void rejectParallel2AtSouthPole() {
    assertThrows(IllegalArgumentException.class, () ->
            new LambertConformal(45., 0., -45., -90.));
  }

  /**
   * LambertConformal should reject standard parallel #2 set to +90.
   */
  @Test
  public void rejectParallel2AtNorthPole() {
    assertThrows(IllegalArgumentException.class, () ->
            new LambertConformal(45., 0., 45., 90.));
  }
}
