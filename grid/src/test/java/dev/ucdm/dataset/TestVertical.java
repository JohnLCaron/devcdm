/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.ucdm.dataset;

import com.google.common.base.Strings;
import dev.ucdm.dataset.api.CdmDatasetCS;
import dev.ucdm.dataset.transform.vertical.AtmosHybridSigmaPressure;
import dev.ucdm.dataset.transform.vertical.AtmosSigma;
import dev.ucdm.dataset.transform.vertical.ExistingFieldVerticalTransform;
import dev.ucdm.dataset.transform.vertical.OceanS;
import dev.ucdm.dataset.transform.vertical.OceanSG2;
import dev.ucdm.dataset.transform.vertical.OceanSigma;
import dev.ucdm.dataset.transform.vertical.VerticalTransform;
import dev.ucdm.dataset.transform.vertical.WrfEta;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import dev.ucdm.array.Array;
import dev.ucdm.array.InvalidRangeException;
import dev.ucdm.dataset.api.CdmDatasets;
import dev.ucdm.grid.api.Grid;
import dev.ucdm.grid.api.GridCoordinateSystem;
import dev.ucdm.grid.api.GridHorizCoordinateSystem;
import dev.ucdm.grid.api.GridTimeCoordinateSystem;
import dev.ucdm.grid.internal.GridNetcdfDataset;
import dev.ucdm.dataset.api.SimpleUnit;

import java.io.IOException;
import java.util.Formatter;
import java.util.Optional;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static dev.ucdm.test.util.TestFilesKt.extraTestDir;

/** Test basic projection methods */
public class TestVertical {

  @Test
  public void testOceanCF_OceanSigma() throws Exception {
    open(extraTestDir + "transforms/erie_test.ncml", "temp", OceanSigma.class, SimpleUnit.kmUnit);
  }

  @Test
  public void testOceanSG2() throws Exception {
    open(extraTestDir + "transforms/ocean_his_g2.nc", "u", OceanSG2.class, SimpleUnit.kmUnit);
  }

  @Test
  public void testExistingFieldVerticalTransform() throws Exception {
    open(extraTestDir + "transforms/VExisting3D_NUWG.nc", "rhu_hybr", ExistingFieldVerticalTransform.class,
            SimpleUnit.geopotentialHeight);
  }

  @Test
  @Disabled("fails because not correctly slicing vertical dimension out")
  public void testHIRLAMhybrid() throws Exception {
    open(extraTestDir + "transforms/HIRLAMhybrid.ncml", "Relative_humidity_hybrid",
        AtmosHybridSigmaPressure.class, SimpleUnit.pressureUnit);
  }

  @Test
  public void testOceanS() throws Exception {
    open(extraTestDir + "transforms/roms_ocean_s_coordinate.nc", "temp", OceanS.class, SimpleUnit.kmUnit);
  }

  @Test
  public void testOceanSigma() throws Exception {
    open(extraTestDir + "transforms/gomoos_cf.nc", "temp", OceanSigma.class, SimpleUnit.kmUnit);
  }

  @Test
  public void testAtmSigma() throws Exception {
    open(extraTestDir + "transforms/temperature.nc", "Temperature", AtmosSigma.class,
        SimpleUnit.pressureUnit);
  }

  @Test
  public void testAtmHybrid() throws Exception {
    open(extraTestDir + "transforms/ccsm2.nc", "T", AtmosHybridSigmaPressure.class,
        SimpleUnit.pressureUnit);
  }

  @Test
  public void testHybridSigmaPressure() throws Exception {
    String filename = extraTestDir + "transforms/HybridSigmaPressure.nc";
    open(filename, "T", AtmosHybridSigmaPressure.class, SimpleUnit.pressureUnit);
  }

  @Test
  public void testWrfEta() throws Exception {
    open(extraTestDir + "transforms/wrfout_v2_Lambert.nc", "T", WrfEta.class, SimpleUnit.pressureUnit);
  }

  @Test
  public void testWrfEta2() throws Exception {
    open(extraTestDir + "transforms/wrfout_d01_2006-03-08_21-00-00", "T", WrfEta.class,
        SimpleUnit.pressureUnit);
  }

  static boolean open(String filename, String gridName, Class<?> vtClass, SimpleUnit vunit)
      throws IOException, InvalidRangeException {
    System.out.printf("compare %s %s%n", filename, gridName);

    Formatter errlog = new Formatter();
    try (CdmDatasetCS ds = CdmDatasets.openDatasetWithCS(filename, true)) {
      Optional<GridNetcdfDataset> grido = GridNetcdfDataset.create(ds, errlog);
      assertWithMessage(errlog.toString()).that(grido.isPresent()).isTrue();
      GridNetcdfDataset gridNetcdfDataset = grido.get();
      Grid grid = gridNetcdfDataset.findGrid(gridName).orElseThrow(() -> new RuntimeException("Cant open Grid " + gridName));

      GridCoordinateSystem gcs = grid.getCoordinateSystem();
      assertThat(gcs).isNotNull();
      VerticalTransform vt = gcs.getVerticalTransform();
      assertThat(vt).isNotNull();
      System.out.printf(" VerticalTransform new %s%n", vt.getClass().getName());
      assertThat(vt.getClass()).isEqualTo(vtClass);

      // should be compatible with vunit
      String vertCoordUnit = vt.getUnitString();
      if (vunit != null) {
        assertWithMessage(String.format("%s expect %s", vertCoordUnit, vunit)).that(vunit.isCompatible(vertCoordUnit))
            .isTrue();
      } else {
        assertWithMessage(String.format("%s expect %s", vertCoordUnit, vunit))
            .that(Strings.isNullOrEmpty(vertCoordUnit)).isTrue();
      }

      GridTimeCoordinateSystem tcs = grid.getTimeCoordinateSystem();
      int ntimes = tcs == null ? 1 : tcs.getTimeOffsetAxis(0).getNominalSize();
      for (int timeIndex = 0; timeIndex < ntimes; timeIndex++) {
        Array<Number> zt = vt.getCoordinateArray3D(timeIndex);
      }

      GridHorizCoordinateSystem hcs = grid.getHorizCoordinateSystem();
      int yindex = hcs.getShape().get(0) / 2;
      int xindex = hcs.getShape().get(1) / 2;

      for (int timeIndex = 0; timeIndex < ntimes; timeIndex++) {
        Array<Number> zt = vt.getCoordinateArray1D(timeIndex, xindex, yindex);
      }
    }
    return true;
  }

}
