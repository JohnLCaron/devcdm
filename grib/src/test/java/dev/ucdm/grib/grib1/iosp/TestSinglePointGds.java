/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.ucdm.grib.grib1.iosp;

import dev.ucdm.array.Array;
import dev.ucdm.core.api.*;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

import static com.google.common.truth.Truth.assertThat;

public class TestSinglePointGds {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  /**
   * See https://github.com/Unidata/netcdf-java/issues/297
   *
   * This tests reading a GRIB message with a GDS describing a single lat/lon point.
   *
   */
  @Test
  public void checkLatLon() throws IOException {
    final String testfile = "../grib/src/test/data/single_point_gds.grib1";
    final float expectedLon = 76.21f;
    final float expectedLat = 18.95f;
    final float tol = 0.001f;

    try (CdmFile nc = CdmFiles.open(testfile)) {
      checkVal(nc.findVariable("lon"), expectedLon, tol);
      checkVal(nc.findVariable("lat"), expectedLat, tol);
    }
  }

  private void checkVal(Variable variable, float expectedValue, float tol) throws IOException {
    Array<Float> array = (Array<Float>) variable.readArray();
    assertThat(array.getSize()).isEqualTo(1);
    float val = array.get(0);
    assertThat(val).isWithin(tol).of(expectedValue);
  }
}
