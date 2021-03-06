/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.ucdm.gcdm.client;

import static com.google.common.truth.Truth.assertThat;
import static dev.ucdm.test.util.TestFilesKt.coreLocalNetcdf4Dir;
import static dev.ucdm.test.util.TestFilesKt.testFilesIn;

import java.util.function.Predicate;
import java.util.stream.Stream;

import dev.ucdm.core.api.CdmFile;
import dev.ucdm.dataset.api.CdmDatasets;
import dev.ucdm.test.util.CompareCdmDataset;
import dev.ucdm.gcdm.server.DataRoots;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** Test {@link GcdmCdmFile} */
public class TestGcdmNetcdf4 {
  private static DataRoots dataRoots = new DataRoots();

  private static final Predicate<Object[]> filesToSkip = new Predicate<Object[]>() {
    @Override
    public boolean test(Object[] filenameParam) {
      // these files are removed because they cause an OutOfMemeoryError
      // todo: why do these cause an OutOfMemoryError?
      String fname = (String) filenameParam[0];
      return !(fname.endsWith("/e562p1_fp.inst3_3d_asm_Nv.20100907_00z+20100909_1200z.nc4")
          || fname.endsWith("/tiling.nc4"));
    }
  };

  public static Stream<Arguments> params() {
    return testFilesIn(coreLocalNetcdf4Dir)
            .build();
  }

  @ParameterizedTest
  @MethodSource("params")
  public void doOne(String filename) throws Exception {
    String gcdmUrl = dataRoots.makeGcdmUrl(filename);
    try (CdmFile ncfile = CdmDatasets.openFile(filename, null);
         GcdmCdmFile gcdmFile = GcdmCdmFile.builder().setRemoteURI(gcdmUrl).build()) {

      boolean ok = new CompareCdmDataset().compare(ncfile, gcdmFile);
      assertThat(ok).isTrue();
    }
  }
}
