/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.ucdm.gcdm;

import org.junit.Test;
import dev.ucdm.gcdm.client.GcdmCdmFile;

/** Test {@link GcdmCdmFile} */
public class TestGcdmGridDatasetProblems {

  /* TODO GRIB @Test
  public void testTimeCoordRegular() throws Exception {
    String filename = TestGcdmDatasets.testDir + "tds_index/NCEP/NBM/Alaska/NCEP_ALASKA_MODEL_BLEND.ncx4";
    TestGcdmGridConverter.roundtrip(Paths.get(filename));
  } */

  @Test
  public void testCurvilinear() throws Exception {
    String filename = TestGcdmDatasets.testDir + "grid/stag/bora_feb.nc";
    TestGcdmGridDataset.roundtrip(filename);
  }

  @Test
  public void testVerticalTransform() throws Exception {
    String filename = TestGcdmDatasets.testDir + "grid/testCFwriter.nc";
    TestGcdmGridDataset.roundtrip(filename);
  }

}
