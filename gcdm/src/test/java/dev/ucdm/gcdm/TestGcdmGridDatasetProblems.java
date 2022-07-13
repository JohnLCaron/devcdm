/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.ucdm.gcdm;

import dev.ucdm.gcdm.client.GcdmCdmFile;
import org.junit.jupiter.api.Test;

import static dev.ucdm.test.util.TestFilesKt.extraTestDir;
import static dev.ucdm.test.util.TestFilesKt.oldTestDir;


/** Test {@link GcdmCdmFile} */
public class TestGcdmGridDatasetProblems {

  @Test
  public void testTimeCoordRegular() throws Exception {
    String filename = oldTestDir + "tds_index/NCEP/NBM/Alaska/NCEP_NBM_ALASKA_ver7.ncx4";
    TestGcdmGridDataset.roundtrip(filename);
  }

  @Test
  public void testCurvilinear() throws Exception {
    String filename = extraTestDir + "grid/stag/bora_feb.nc";
    TestGcdmGridDataset.roundtrip(filename);
  }

  @Test
  public void testVerticalTransform() throws Exception {
    String filename = extraTestDir + "grid/testCFwriter.nc";
    TestGcdmGridDataset.roundtrip(filename);
  }

}
