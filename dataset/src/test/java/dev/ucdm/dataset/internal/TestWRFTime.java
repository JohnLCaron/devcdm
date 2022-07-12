/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package dev.ucdm.dataset.internal;

import org.junit.jupiter.api.Test;
import dev.ucdm.array.Array;
import dev.ucdm.core.constants.AxisType;
import dev.ucdm.dataset.api.*;

import java.io.IOException;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static com.google.common.truth.Truth.assertThat;

public class TestWRFTime {

  @Test
  public void testWrfTimeUnderscore() throws IOException {
    String tstFile = TestCdmDatasets.datasetLocalDir + "dataset/WrfTimesStrUnderscore.nc";
    System.out.println(tstFile);
    try (CdmDatasetCS ncd = CdmDatasets.openDatasetWithCS(tstFile, true)) {
      // make sure this file went through the WrfConvention
      assertThat(ncd.getConventionBuilder()).isEqualTo("WrfConventions");
      CoordinateAxis tca = ncd.findCoordinateAxis(AxisType.Time);
      Array<Number> times = (Array<Number>) tca.readArray();
      // first date in this file is 1214524800 [seconds since 1970-01-01T00:00:00],
      // which is 2008-06-27 00:00:00
      assertThat(times.get(0).intValue()).isEqualTo(1214524800);
    }
  }

  @Test
  public void testWrfNoTimeVar() throws IOException {
    String tstFile = TestCdmDatasets.datasetLocalDir + "dataset/WrfNoTimeVar.nc";
    System.out.printf("Open %s%n", tstFile);
    Set<CdmDataset.Enhance> defaultEnhanceMode = CdmDataset.getDefaultEnhanceMode();
    EnumSet<CdmDataset.Enhance> enhanceMode = EnumSet.copyOf(defaultEnhanceMode);
    // enhanceMode.add(CdmDataset.Enhance.IncompleteCoordSystems);
    DatasetUrl durl = DatasetUrl.findDatasetUrl(tstFile);
    try (CdmDatasetCS ncd = CdmDatasets.openDatasetWithCS(tstFile, true)) {
      List<CoordinateSystem> cs = ncd.getCoordinateSystems();
      assertThat(cs.size()).isEqualTo(1);
      CoordinateSystem dsCs = cs.get(0);
      assertThat(dsCs.getCoordinateAxes().size()).isEqualTo(2);

      VariableDS var = (VariableDS) ncd.findVariable("T2");
      assertThat(var).isNotNull();
      List<CoordinateSystem> varCs = ncd.makeCoordinateSystemsFor(var);
      assertThat(varCs.size()).isEqualTo(1);
      assertThat(dsCs).isEqualTo(varCs.get(0));
    }
  }
}
