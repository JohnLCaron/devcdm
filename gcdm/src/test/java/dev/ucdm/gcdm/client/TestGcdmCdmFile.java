/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.ucdm.gcdm.client;

import static com.google.common.truth.Truth.assertThat;
import static dev.ucdm.gcdm.client.GcdmCdmFile.PROTOCOL;
import static dev.ucdm.test.util.TestFilesKt.coreLocalDir;
import static dev.ucdm.test.util.TestFilesKt.coreLocalNetcdf3Dir;
import static dev.ucdm.test.util.TestFilesKt.testFilesIn;

import java.util.stream.Stream;

import dev.ucdm.core.api.CdmFile;
import dev.ucdm.dataset.api.CdmDatasets;
import dev.ucdm.test.util.CompareCdmDataset;
import dev.ucdm.gcdm.server.DataRoots;
import dev.ucdm.test.util.FileFilterSkipSuffixes;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** Test {@link GcdmCdmFile} */
public class TestGcdmCdmFile {
  private static DataRoots dataRoots = new DataRoots();

  public static Stream<Arguments> params() {
    return testFilesIn(coreLocalDir)
            .addNameFilter(new FileFilterSkipSuffixes(".cdl .txt"))
            .withRecursion()
            .build();
  }

  public static Stream<Arguments> paramsTest() {
    return Stream.of(
            Arguments.of(coreLocalNetcdf3Dir + "testWriteFill.nc")
    );
  }

  @ParameterizedTest
  @MethodSource("params")
  public void doOne(String filename) throws Exception {
    System.out.printf("TestGcdmCdmFile %s%n", filename);
    String gcdmUrl = dataRoots.makeGcdmUrl(filename);
    try (CdmFile ncfile = CdmDatasets.openFile(filename, null);
         GcdmCdmFile gcdmFile = GcdmCdmFile.builder().setRemoteURI(gcdmUrl).build()) {

      boolean ok = new CompareCdmDataset().compare(ncfile, gcdmFile);
      assertThat(ok).isTrue();
      assertThat(gcdmFile.getCdmFileTypeId()).isEqualTo(PROTOCOL);
      assertThat(gcdmFile.getCdmFileTypeDescription()).isEqualTo(PROTOCOL);
      assertThat(gcdmFile.getCdmFileTypeVersion()).isEqualTo("N/A");
    }
  }

}
