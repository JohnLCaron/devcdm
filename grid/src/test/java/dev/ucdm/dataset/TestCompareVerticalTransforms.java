/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.ucdm.dataset;

import dev.ucdm.dataset.api.SimpleUnit;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

import static com.google.common.truth.Truth.assertThat;
import static dev.ucdm.test.util.TestFilesKt.extraTestDir;

/**
 * Open VerticalTransforms.
 * These are all the test files I can find with a vertical transform.
 * We can port/implement more transforms if we get a test file.
 */
public class TestCompareVerticalTransforms {

  public static Stream<Arguments> params() {
    return Stream.of(
      Arguments.of(extraTestDir + "transforms/erie_test.ncml", "temp",
          dev.ucdm.dataset.transform.vertical.OceanSigma.class, SimpleUnit.kmUnit),
      Arguments.of(extraTestDir + "transforms/gomoos.ncml", "temp",
          dev.ucdm.dataset.transform.vertical.OceanSigma.class, SimpleUnit.kmUnit),
      Arguments.of(extraTestDir + "transforms/OceanSigma.nc", "temp",
          dev.ucdm.dataset.transform.vertical.OceanSigma.class, SimpleUnit.kmUnit),

      Arguments.of(extraTestDir + "transforms/bora_feb_001.nc", "AKs",
          dev.ucdm.dataset.transform.vertical.OceanS.class, SimpleUnit.kmUnit),
      Arguments.of(extraTestDir + "transforms/bora_feb_001.nc", "salt",
          dev.ucdm.dataset.transform.vertical.OceanS.class, SimpleUnit.kmUnit),
      Arguments.of(extraTestDir + "transforms/OceanS.nc", "salt",
          dev.ucdm.dataset.transform.vertical.OceanS.class, SimpleUnit.kmUnit),
      Arguments.of(extraTestDir + "transforms/OceanS2.nc", "salt",
          dev.ucdm.dataset.transform.vertical.OceanS.class, SimpleUnit.kmUnit),

      Arguments.of(extraTestDir + "transforms/ocean_his_g1.nc", "u",
          dev.ucdm.dataset.transform.vertical.OceanSG1.class, SimpleUnit.kmUnit),
      Arguments.of(extraTestDir + "transforms/ocean_his_g2.nc", "u",
          dev.ucdm.dataset.transform.vertical.OceanSG2.class, SimpleUnit.kmUnit),

      Arguments.of(extraTestDir + "transforms/VExisting3D_NUWG.nc", "rhu_hybr",
          dev.ucdm.dataset.transform.vertical.ExistingFieldVerticalTransform.class, SimpleUnit.geopotentialHeight),

      Arguments.of(extraTestDir + "transforms/temperature.nc", "Temperature",
          dev.ucdm.dataset.transform.vertical.AtmosSigma.class, SimpleUnit.pressureUnit),
      Arguments.of(extraTestDir + "transforms/Sigma_LC.nc", "Temperature",
          dev.ucdm.dataset.transform.vertical.AtmosSigma.class, SimpleUnit.pressureUnit),

      Arguments.of(extraTestDir + "transforms/ccsm2.nc", "MQ",
          dev.ucdm.dataset.transform.vertical.AtmosHybridSigmaPressure.class, SimpleUnit.pressureUnit),
      Arguments.of(extraTestDir + "transforms/ha0001.nc", "CME",
          dev.ucdm.dataset.transform.vertical.AtmosHybridSigmaPressure.class, SimpleUnit.pressureUnit),
      Arguments.of(extraTestDir + "transforms/ha0001.nc", "CGS",
          dev.ucdm.dataset.transform.vertical.AtmosHybridSigmaPressure.class, SimpleUnit.pressureUnit),
      Arguments.of(extraTestDir + "transforms/HybridSigmaPressure.nc", "T",
          dev.ucdm.dataset.transform.vertical.AtmosHybridSigmaPressure.class, SimpleUnit.pressureUnit),
      Arguments.of(extraTestDir + "transforms/climo.cam2.h0.0000-09.nc", "T",
          dev.ucdm.dataset.transform.vertical.AtmosHybridSigmaPressure.class, SimpleUnit.pressureUnit),
      // @Disabled("fails because not correctly slicing vertical dimension out")
      // Arguments.of(TestGridDatasets.gridTestDir + "transforms/HIRLAMhybrid.ncml", "Relative_humidity_hybrid",
      // dev.ucdm.dataset.transform.vertical.AtmosHybridSigmaPressure.class, SimpleUnit.pressureUnit),

      Arguments.of(extraTestDir + "transforms/espresso_his_20130505_0000_0001.nc", "u",
          dev.ucdm.dataset.transform.vertical.OceanSG1.class, SimpleUnit.kmUnit),
      Arguments.of(extraTestDir + "transforms/espresso_his_20130505_0000_0001.nc", "w",
          dev.ucdm.dataset.transform.vertical.OceanSG1.class, SimpleUnit.kmUnit),

      Arguments.of(extraTestDir + "transforms/wrfout_v2_Lambert.nc", "U",
          dev.ucdm.dataset.transform.vertical.WrfEta.class, SimpleUnit.pressureUnit),
      Arguments.of(extraTestDir + "transforms/wrfout_v2_Lambert.nc", "V",
          dev.ucdm.dataset.transform.vertical.WrfEta.class, SimpleUnit.pressureUnit),
      Arguments.of(extraTestDir + "transforms/wrfout_v2_Lambert.nc", "W",
          dev.ucdm.dataset.transform.vertical.WrfEta.class, SimpleUnit.kmUnit), // z_stag has meters coord
      Arguments.of(extraTestDir + "transforms/wrfout_v2_Lambert.nc", "T",
          dev.ucdm.dataset.transform.vertical.WrfEta.class, SimpleUnit.pressureUnit),
      Arguments.of(extraTestDir + "transforms/wrfout_d01_2006-03-08_21-00-00", "U",
          dev.ucdm.dataset.transform.vertical.WrfEta.class, SimpleUnit.pressureUnit)
    );
  }

  @ParameterizedTest
  @MethodSource("params")
  public void compare(String filename, String gridName, Class<?> vtClass, SimpleUnit vunit) throws Exception {
    assertThat(TestVertical.open(filename, gridName, vtClass, vunit)).isTrue();
  }

}
