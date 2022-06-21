/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.cdm.gcdm;

import static com.google.common.truth.Truth.assertThat;

import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;

import dev.cdm.core.api.CdmFile;
import dev.cdm.core.util.CompareArrayToArray;
import dev.cdm.dataset.api.CdmDatasets;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import dev.cdm.gcdm.client.GcdmCdmFile;

/** Test {@link GcdmCdmFile} */
@RunWith(Parameterized.class)
public class TestGcdmCdmFile {
  @Parameterized.Parameters(name = "{0}")
  public static List<Object[]> getTestParameters() {
    List<Object[]> result = new ArrayList<>(500);
    try {
      TestDir.actOnAllParameterized(TestDir.cdmLocalTestDataDir, new SuffixFileFilter(".nc"), result, true);

      FileFilter ff = TestDir.FileFilterSkipSuffix(".cdl .ncml perverse.nc");
      TestDir.actOnAllParameterized(TestDir.cdmUnitTestDir + "formats/bufr/userExamples", ff, result, false);
      TestDir.actOnAllParameterized(TestDir.cdmUnitTestDir + "/formats/netcdf3", ff, result, true);
      TestDir.actOnAllParameterized(TestDir.cdmUnitTestDir + "/formats/netcdf4/files", ff, result, true);
      TestDir.actOnAllParameterized(TestDir.cdmUnitTestDir + "/formats/netcdf4/vlen", ff, result, true);
      TestDir.actOnAllParameterized(TestDir.cdmUnitTestDir + "/formats/hdf5/samples", ff, result, true);
      TestDir.actOnAllParameterized(TestDir.cdmUnitTestDir + "/formats/hdf5/support", ff, result, true);
      TestDir.actOnAllParameterized(TestDir.cdmUnitTestDir + "/formats/hdf5/wrf", ff, result, true);
      // TestDir.actOnAllParameterized(TestDir.cdmUnitTestDir + "/formats/hdf4", ff, result, true);

    } catch (Exception e) {
      e.printStackTrace();
    }
    return result;
  }

  private final String filename;
  private final String gcdmUrl;

  public TestGcdmCdmFile(String filename) {
    this.filename = filename.replace("\\", "/");

    // kludge for now. Also, need to auto start up CmdrServer
    this.gcdmUrl = "gcdm://localhost:16111/" + this.filename;
  }

  @Test
  public void doOne() throws Exception {
    System.out.printf("TestGcdmCdmFile %s%n", filename);
    try (CdmFile ncfile = CdmDatasets.openFile(filename, null);
         GcdmCdmFile gcdmFile = GcdmCdmFile.builder().setRemoteURI(gcdmUrl).build()) {

      boolean ok = CompareArrayToArray.compareFiles(ncfile, gcdmFile);
      assertThat(ok).isTrue();
    }
  }

}
