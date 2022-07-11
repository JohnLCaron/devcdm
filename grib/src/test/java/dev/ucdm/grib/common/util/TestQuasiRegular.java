/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package dev.ucdm.grib.common.util;

import org.junit.jupiter.api.Test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

import static com.google.common.truth.Truth.assertThat;

public class TestQuasiRegular {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final double x1d = 1.0e30; /* derivative of the first end point */
  private static final double xnd = 1.0e30; /* derivative of the nth end point */

  @Test
  public void testCubicSpline() throws IOException {
    float[] in = new float[] {0.0f, 9.0f, 18.0f, 27.0f};

    double[] d2 = new double[4];
    QuasiRegular.secondDerivative(in, 0, 4, x1d, xnd, d2);

    float[] out = new float[8];
    QuasiRegular.cubicSpline(in, 0, d2, 0.0, out, 0);
    assertThat(out[0]).isEqualTo(0.0f);
    QuasiRegular.cubicSpline(in, 0, d2, 0.5, out, 1);
    assertThat(out[1]).isEqualTo(4.5f);
    QuasiRegular.cubicSpline(in, 0, d2, 1.0, out, 2);
    assertThat(out[2]).isEqualTo(9.0f);
    QuasiRegular.cubicSpline(in, 0, d2, 1.5, out, 3);
    assertThat(out[3]).isEqualTo(13.5f);
    QuasiRegular.cubicSpline(in, 0, d2, 2.0, out, 4);
    assertThat(out[4]).isEqualTo(18.0f);
    QuasiRegular.cubicSpline(in, 0, d2, 2.5, out, 5);
    assertThat(out[5]).isEqualTo(22.5f);
    QuasiRegular.cubicSpline(in, 0, d2, 3.0, out, 6);
    assertThat(out[6]).isEqualTo(27.0f);
    QuasiRegular.cubicSpline(in, 0, d2, 3.5, out, 7);
    assertThat(out[7]).isEqualTo(13.5f);
  }

}
