/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.ucdm.grib.grib2.record;

import dev.ucdm.core.api.*;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static com.google.common.truth.Truth.assertThat;

public class TestMRMS {
  static final String testfile =
      "../grib/src/test/data/MRMS_LowLevelCompositeReflectivity_00.50_20141207-072038.grib2.gz";

  static final String testfile24BitPng =
      "../grib/src/test/data/pngEncoding/24-bit/MRMS_FLASH_HP_MAXUNITSTREAMFLOW_00.00_20210615-190000.grib2";

  @Test
  public void checkVariable() throws IOException {
    try (CdmFile nc = CdmFiles.open(testfile)) {
      Variable var = nc.findVariable("LowLevelCompositeReflectivity_altitude_above_msl");
      assertThat(var).isNotNull();

      Attribute att = var.findAttribute("missing_value");
      assertThat(att).isNotNull();
      assertThat(att.getNumericValue().doubleValue()).isWithin(1e-6).of(-99.);

      att = var.findAttribute("_FillValue");
      assertThat(att).isNotNull();
      assertThat(att.getNumericValue().doubleValue()).isWithin(1e-6).of(-999.);
    }
  }

  @Test
  public void checkVariable24bit() throws IOException {
    try (CdmFile nc = CdmFiles.open(testfile24BitPng)) {
      Variable var = nc.findVariable("FLASH_HP_MAXUNITSTREAMFLOW_surface");
      assertThat(var).isNotNull();

      Attribute att = var.findAttribute("missing_value");
      assertThat(att).isNotNull();
      assertThat(att.getNumericValue().doubleValue()).isWithin(1e-6).of(-9999.);

      att = var.findAttribute("_FillValue");
      assertThat(att).isNotNull();
      assertThat(att.getNumericValue().doubleValue()).isWithin(1e-6).of(-999.);
    }
  }
}
