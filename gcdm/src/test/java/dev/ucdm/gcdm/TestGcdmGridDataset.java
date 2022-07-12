/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.ucdm.gcdm;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import dev.ucdm.gcdm.client.GcdmGridDataset;
import dev.ucdm.gcdm.client.GcdmCdmFile;
import dev.ucdm.grid.api.*;

import java.io.File;
import java.util.Formatter;
import java.util.stream.Stream;

import static com.google.common.truth.Truth.assertThat;

/**
 * Test {@link GcdmCdmFile}
 */
public class TestGcdmGridDataset {
  public static Stream<Arguments> params() {
    return Stream.of(
            Arguments.of(TestGcdmDatasets.coreLocalDir + "permuteTest.nc"));

    //FileFilter ff = TestDir.FileFilterSkipSuffix(".cdl .gbx9 aggFmrc.xml cg.ncml");
    // TestDir.actOnAllParameterized(TestDir.cdmUnitTestDir + "/ft/grid", ff, result, true);
  }

  @ParameterizedTest
  @MethodSource("params")
  public void doOne(String filename) throws Exception {
    roundtrip(filename);
  }

  public static void roundtrip(String filename) throws Exception {

    filename = filename.replace("\\", "/");
    File file = new File(filename);
    // kludge for now. Also, need to auto start up CmdrServer
    String gcdmUrl = "gcdm://localhost:16111/" + file.getCanonicalPath();

    System.out.printf("TestGcdmCdmFile  %s%n", filename);

    Formatter info = new Formatter();
    try (GridDataset local = GridDatasetFactory.openGridDataset(filename, info)) {
      if (local == null) {
        System.out.printf("TestGcdmCdmFile %s NOT a grid %s%n", filename, info);
        return;
      }
      try (GridDataset remote = GridDatasetFactory.openGridDataset(gcdmUrl, info)) {
        if (remote == null) {
          System.out.printf(" Remote %s fails %s%n", filename, info);
          return;
        }
        assertThat(remote).isInstanceOf(GcdmGridDataset.class);
        new CompareGridDataset(remote, local, false).compare();
      }
    }
  }

}
