/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.cdm.dataset.conv;

import dev.cdm.core.api.Attribute;
import dev.cdm.core.constants._Coordinate;
import dev.cdm.dataset.api.CdmDataset;
import dev.cdm.dataset.api.CdmDatasetCS;
import dev.cdm.dataset.api.CdmDatasets;
import dev.cdm.dataset.util.CdmObjFilter;
import dev.cdm.dataset.util.CompareCdmDataset;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import static com.google.common.truth.Truth.assertThat;

public class TestOpenCdmDatasets {

  public static Stream<Arguments> params() throws IOException {
    return Stream.concat(
        Files.list(Paths.get(dev.cdm.dataset.api.TestCdmDatasets.coreLocalDir))
              .filter(file -> !Files.isDirectory(file))
              .filter(file -> !file.getFileName().toString().startsWith("Wrf"))
              .map(Path::toString)
              .map(Arguments::of),

        Stream.of(
            Arguments.of(dev.cdm.dataset.api.TestCdmDatasets.datasetLocalNcmlDir + "testRead.xml"),
            Arguments.of(dev.cdm.dataset.api.TestCdmDatasets.datasetLocalNcmlDir + "readMetadata.xml"),
            Arguments.of(dev.cdm.dataset.api.TestCdmDatasets.datasetLocalNcmlDir + "testReadHttps.xml"))
    );
  }

  @ParameterizedTest
  @MethodSource("params")
  public void compareNoEnhance(String filename) throws Exception {
    System.out.printf("TestCdmDatasets %s%n", filename);
    try (CdmDataset ncd = CdmDatasets.openDataset(filename, false, null);
         CdmDatasetCS ncdc = CdmDatasets.openDatasetCS(filename, false)) {

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
         CdmDatasetCS ncdc = CdmDatasets.openDatasetCS(filename, true)) {

      // does not compare coordinate systems
      boolean ok = new CompareCdmDataset().compare(ncd, ncdc, new LocalFilter());
      assertThat(ok).isTrue();
    }
  }

}
