/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.cdm.dataset;

import dev.cdm.dataset.api.SimpleUnit;
import dev.cdm.grid.api.TestGridDatasets;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

import static com.google.common.truth.Truth.assertThat;

/**
 * Open VerticalTransforms.
 * These are all the test files I can find with a vertical transform.
 * We can port/implement more transforms if we get a test file.
 */
public class TestCompareVerticalTransforms {

  public static Stream<Arguments> params() {
    return Stream.of(
      Arguments.of(TestGridDatasets.gridTestDir + "transforms/erie_test.ncml", "temp",
          dev.cdm.dataset.transform.vertical.OceanSigma.class, SimpleUnit.kmUnit),
      Arguments.of(TestGridDatasets.gridTestDir + "transforms/gomoos.ncml", "temp",
          dev.cdm.dataset.transform.vertical.OceanSigma.class, SimpleUnit.kmUnit),
      Arguments.of(TestGridDatasets.gridTestDir + "transforms/OceanSigma.nc", "temp",
          dev.cdm.dataset.transform.vertical.OceanSigma.class, SimpleUnit.kmUnit),

      Arguments.of(TestGridDatasets.gridTestDir + "transforms/bora_feb_001.nc", "AKs",
          dev.cdm.dataset.transform.vertical.OceanS.class, SimpleUnit.kmUnit),
      Arguments.of(TestGridDatasets.gridTestDir + "transforms/bora_feb_001.nc", "salt",
          dev.cdm.dataset.transform.vertical.OceanS.class, SimpleUnit.kmUnit),
      Arguments.of(TestGridDatasets.gridTestDir + "transforms/OceanS.nc", "salt",
          dev.cdm.dataset.transform.vertical.OceanS.class, SimpleUnit.kmUnit),
      Arguments.of(TestGridDatasets.gridTestDir + "transforms/OceanS2.nc", "salt",
          dev.cdm.dataset.transform.vertical.OceanS.class, SimpleUnit.kmUnit),

      Arguments.of(TestGridDatasets.gridTestDir + "transforms/ocean_his_g1.nc", "u",
          dev.cdm.dataset.transform.vertical.OceanSG1.class, SimpleUnit.kmUnit),
      Arguments.of(TestGridDatasets.gridTestDir + "transforms/ocean_his_g2.nc", "u",
          dev.cdm.dataset.transform.vertical.OceanSG2.class, SimpleUnit.kmUnit),

      Arguments.of(TestGridDatasets.gridTestDir + "transforms/VExisting3D_NUWG.nc", "rhu_hybr",
          dev.cdm.dataset.transform.vertical.ExistingFieldVerticalTransform.class, SimpleUnit.geopotentialHeight),

      Arguments.of(TestGridDatasets.gridTestDir + "transforms/temperature.nc", "Temperature",
          dev.cdm.dataset.transform.vertical.AtmosSigma.class, SimpleUnit.pressureUnit),
      Arguments.of(TestGridDatasets.gridTestDir + "transforms/Sigma_LC.nc", "Temperature",
          dev.cdm.dataset.transform.vertical.AtmosSigma.class, SimpleUnit.pressureUnit),

      Arguments.of(TestGridDatasets.gridTestDir + "transforms/ccsm2.nc", "MQ",
          dev.cdm.dataset.transform.vertical.AtmosHybridSigmaPressure.class, SimpleUnit.pressureUnit),
      Arguments.of(TestGridDatasets.gridTestDir + "transforms/ha0001.nc", "CME",
          dev.cdm.dataset.transform.vertical.AtmosHybridSigmaPressure.class, SimpleUnit.pressureUnit),
      Arguments.of(TestGridDatasets.gridTestDir + "transforms/ha0001.nc", "CGS",
          dev.cdm.dataset.transform.vertical.AtmosHybridSigmaPressure.class, SimpleUnit.pressureUnit),
      Arguments.of(TestGridDatasets.gridTestDir + "transforms/HybridSigmaPressure.nc", "T",
          dev.cdm.dataset.transform.vertical.AtmosHybridSigmaPressure.class, SimpleUnit.pressureUnit),
      Arguments.of(TestGridDatasets.gridTestDir + "transforms/climo.cam2.h0.0000-09.nc", "T",
          dev.cdm.dataset.transform.vertical.AtmosHybridSigmaPressure.class, SimpleUnit.pressureUnit),
      // @Disabled("fails because not correctly slicing vertical dimension out")
      // Arguments.of(TestGridDatasets.gridTestDir + "transforms/HIRLAMhybrid.ncml", "Relative_humidity_hybrid",
      // dev.cdm.dataset.transform.vertical.AtmosHybridSigmaPressure.class, SimpleUnit.pressureUnit),

      Arguments.of(TestGridDatasets.gridTestDir + "transforms/espresso_his_20130505_0000_0001.nc", "u",
          dev.cdm.dataset.transform.vertical.OceanSG1.class, SimpleUnit.kmUnit),
      Arguments.of(TestGridDatasets.gridTestDir + "transforms/espresso_his_20130505_0000_0001.nc", "w",
          dev.cdm.dataset.transform.vertical.OceanSG1.class, SimpleUnit.kmUnit),

      Arguments.of(TestGridDatasets.gridTestDir + "transforms/wrfout_v2_Lambert.nc", "U",
          dev.cdm.dataset.transform.vertical.WrfEta.class, SimpleUnit.pressureUnit),
      Arguments.of(TestGridDatasets.gridTestDir + "transforms/wrfout_v2_Lambert.nc", "V",
          dev.cdm.dataset.transform.vertical.WrfEta.class, SimpleUnit.pressureUnit),
      Arguments.of(TestGridDatasets.gridTestDir + "transforms/wrfout_v2_Lambert.nc", "W",
          dev.cdm.dataset.transform.vertical.WrfEta.class, SimpleUnit.kmUnit), // z_stag has meters coord
      Arguments.of(TestGridDatasets.gridTestDir + "transforms/wrfout_v2_Lambert.nc", "T",
          dev.cdm.dataset.transform.vertical.WrfEta.class, SimpleUnit.pressureUnit),
      Arguments.of(TestGridDatasets.gridTestDir + "transforms/wrfout_d01_2006-03-08_21-00-00", "U",
          dev.cdm.dataset.transform.vertical.WrfEta.class, SimpleUnit.pressureUnit)
    );
  }

  @ParameterizedTest
  @MethodSource("params")
  public void compare(String filename, String gridName, Class<?> vtClass, SimpleUnit vunit) throws Exception {
    assertThat(TestVertical.open(filename, gridName, vtClass, vunit)).isTrue();
  }

}
