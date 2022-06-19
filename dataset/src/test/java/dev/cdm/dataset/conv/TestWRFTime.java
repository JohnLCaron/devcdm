/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package dev.cdm.dataset.conv;

import org.junit.jupiter.api.Test;
import dev.cdm.array.Array;
import dev.cdm.core.constants.AxisType;
import dev.cdm.dataset.api.*;

import java.io.IOException;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static com.google.common.truth.Truth.assertThat;

public class TestWRFTime {

  @Test
  public void testWrfTimeUnderscore() throws IOException {
    String tstFile = TestCdmDatasets.cdmLocalDir + "dataset/WrfTimesStrUnderscore.nc";
    System.out.println(tstFile);
    try (CdmDataset ncd = CdmDatasets.openDataset(tstFile)) {
      // make sure this file went through the WrfConvention
      assertThat(ncd.getConventionUsed()).isEqualTo("WRF");
      CoordinateAxis tca = ncd.findCoordinateAxis(AxisType.Time);
      Array<Number> times = (Array<Number>) tca.readArray();
      // first date in this file is 1214524800 [seconds since 1970-01-01T00:00:00],
      // which is 2008-06-27 00:00:00
      assertThat(times.get(0).intValue()).isEqualTo(1214524800);
    }
  }

  @Test
  public void testWrfNoTimeVar() throws IOException {
    String tstFile = TestCdmDatasets.cdmLocalDir + "dataset/WrfNoTimeVar.nc";
    System.out.printf("Open %s%n", tstFile);
    Set<CdmDataset.Enhance> defaultEnhanceMode = CdmDataset.getDefaultEnhanceMode();
    EnumSet<CdmDataset.Enhance> enhanceMode = EnumSet.copyOf(defaultEnhanceMode);
    enhanceMode.add(CdmDataset.Enhance.IncompleteCoordSystems);
    DatasetUrl durl = DatasetUrl.findDatasetUrl(tstFile);
    try (CdmDataset ncd = CdmDatasets.openDataset(durl, enhanceMode, -1, null, null)) {
      List<CoordinateSystem> cs = ncd.getCoordinateSystems();
      assertThat(cs.size()).isEqualTo(1);
      CoordinateSystem dsCs = cs.get(0);
      assertThat(dsCs.getCoordinateAxes().size()).isEqualTo(2);

      VariableDS var = (VariableDS) ncd.findVariable("T2");
      assertThat(var).isNotNull();
      List<CoordinateSystem> varCs = var.getCoordinateSystems();
      assertThat(varCs.size()).isEqualTo(1);
      assertThat(dsCs).isEqualTo(varCs.get(0));
    }
  }
}
