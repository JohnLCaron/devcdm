/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.cdm.gcdm;

import static com.google.common.truth.Truth.assertThat;

import java.util.function.Predicate;
import java.util.stream.Stream;

import dev.cdm.core.api.CdmFile;
import dev.cdm.dataset.api.CdmDatasets;
import dev.cdm.dataset.util.CompareCdmDatasets;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import dev.cdm.gcdm.client.GcdmCdmFile;

/** Test {@link GcdmCdmFile} */
@RunWith(Parameterized.class)
public class TestGcdmNetcdf4 {

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
    return Stream.of(
            Arguments.of());
/*      FileFilter ff = new SuffixFileFilter(".nc4");
      TestDir.actOnAllParameterized(TestDir.cdmUnitTestDir + "formats/netcdf4", ff, result, false);
      result = result.stream().filter(filesToSkip).collect(Collectors.toList()); */
  }

  @ParameterizedTest
  @MethodSource("params")
  public void doOne(String filename) throws Exception {
    String gcdmUrl = "gcdm://localhost:16111/" + filename;
    try (CdmFile ncfile = CdmDatasets.openFile(filename, null);
         GcdmCdmFile gcdmFile = GcdmCdmFile.builder().setRemoteURI(gcdmUrl).build()) {

      boolean ok = new CompareCdmDatasets().compare(ncfile, gcdmFile);
      assertThat(ok).isTrue();
    }
  }
}
