/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.ucdm.dataset.internal;

import dev.ucdm.core.api.Attribute;
import dev.ucdm.core.constants._Coordinate;
import dev.ucdm.dataset.api.CdmDataset;
import dev.ucdm.dataset.api.CdmDatasetCS;
import dev.ucdm.dataset.api.CdmDatasets;
import dev.ucdm.test.util.CdmObjFilter;
import dev.ucdm.test.util.CompareCdmDataset;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import static com.google.common.truth.Truth.assertThat;
import static dev.ucdm.test.util.TestFilesKt.coreLocalNetcdf3Dir;
import static dev.ucdm.test.util.TestFilesKt.datasetLocalNcmlDir;

public class TestOpenCdmDatasets {

  public static Stream<Arguments> params() throws IOException {
    return Stream.concat(
        Files.list(Paths.get(coreLocalNetcdf3Dir))
              .filter(file -> !Files.isDirectory(file))
              .filter(file -> !file.getFileName().toString().startsWith("Wrf"))
              .map(Path::toString)
              .map(Arguments::of),

        Stream.of(
            Arguments.of(datasetLocalNcmlDir + "testRead.xml"),
            Arguments.of(datasetLocalNcmlDir + "readMetadata.xml"),
            Arguments.of(datasetLocalNcmlDir + "testReadHttps.xml"))
    );
  }

  @ParameterizedTest
  @MethodSource("params")
  public void compareNoEnhance(String filename) throws Exception {
    System.out.printf("TestCdmDatasets %s%n", filename);
    try (CdmDataset ncd = CdmDatasets.openDataset(filename, false, null);
         CdmDatasetCS ncdc = CdmDatasets.openDatasetWithCS(filename, false)) {

      boolean ok = new CompareCdmDataset().compare(ncd, ncdc, new LocalFilter());
      assertThat(ok).isTrue();
    }
  }

  public static class LocalFilter extends CdmObjFilter {

    @Override
    public boolean attCheckOk(Attribute att) {
      String name = att.getShortName();

      return
              !name.startsWith("_Coordinate") &&
              !name.equals("calendar") &&
              !name.equals(_Coordinate._CoordSysBuilder);
    }
  }

  @ParameterizedTest
  @MethodSource("params")
  public void compareEnhance(String filename) throws Exception {
    System.out.printf("TestCdmDatasets enhance %s%n", filename);
    try (CdmDataset ncd = CdmDatasets.openDataset(filename, true, null);
         CdmDatasetCS ncdc = CdmDatasets.openDatasetWithCS(filename, true)) {

      // does not compare coordinate systems
      boolean ok = new CompareCdmDataset().compare(ncd, ncdc, new LocalFilter());
      assertThat(ok).isTrue();
    }
  }

}
