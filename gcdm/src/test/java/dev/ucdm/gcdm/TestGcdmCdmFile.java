/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.ucdm.gcdm;

import static com.google.common.truth.Truth.assertThat;

import java.util.stream.Stream;

import dev.ucdm.core.api.CdmFile;
import dev.ucdm.dataset.api.CdmDatasets;
import dev.ucdm.dataset.util.CompareCdmDataset;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import dev.ucdm.gcdm.client.GcdmCdmFile;

/** Test {@link GcdmCdmFile} */
public class TestGcdmCdmFile {

  public static Stream<Arguments> params() {
    return Stream.of(
            Arguments.of());

    /* TestDir.actOnAllParameterized(TestDir.cdmLocalTestDataDir, new SuffixFileFilter(".nc"), result, true);
      FileFilter ff = TestDir.FileFilterSkipSuffix(".cdl .ncml perverse.nc");
      TestDir.actOnAllParameterized(TestDir.cdmUnitTestDir + "formats/bufr/userExamples", ff, result, false);
      TestDir.actOnAllParameterized(TestDir.cdmUnitTestDir + "/formats/netcdf3", ff, result, true);
      TestDir.actOnAllParameterized(TestDir.cdmUnitTestDir + "/formats/netcdf4/files", ff, result, true);
      TestDir.actOnAllParameterized(TestDir.cdmUnitTestDir + "/formats/netcdf4/vlen", ff, result, true);
      TestDir.actOnAllParameterized(TestDir.cdmUnitTestDir + "/formats/hdf5/samples", ff, result, true);
      TestDir.actOnAllParameterized(TestDir.cdmUnitTestDir + "/formats/hdf5/support", ff, result, true);
      TestDir.actOnAllParameterized(TestDir.cdmUnitTestDir + "/formats/hdf5/wrf", ff, result, true);
      // TestDir.actOnAllParameterized(TestDir.cdmUnitTestDir + "/formats/hdf4", ff, result, true); */
  }

  private final String filename;
  private final String gcdmUrl;

  public TestGcdmCdmFile(String filename) {
    this.filename = filename.replace("\\", "/");

    // kludge for now. Also, need to auto start up CmdrServer
    this.gcdmUrl = "gcdm://localhost:16111/" + this.filename;
  }

  @ParameterizedTest
  @MethodSource("params")
  public void doOne() throws Exception {
    System.out.printf("TestGcdmCdmFile %s%n", filename);
    try (CdmFile ncfile = CdmDatasets.openFile(filename, null);
         GcdmCdmFile gcdmFile = GcdmCdmFile.builder().setRemoteURI(gcdmUrl).build()) {

      boolean ok = new CompareCdmDataset().compare(ncfile, gcdmFile);
      assertThat(ok).isTrue();
    }
  }

}
