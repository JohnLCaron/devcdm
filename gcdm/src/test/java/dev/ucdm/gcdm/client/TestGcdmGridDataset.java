/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.ucdm.gcdm.client;

import dev.ucdm.array.Array;
import dev.ucdm.dataset.transform.vertical.VerticalTransform;
import dev.ucdm.gcdm.CompareGridDataset;
import dev.ucdm.gcdm.server.DataRoots;
import dev.ucdm.test.util.FileFilterSkipSuffixes;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import dev.ucdm.grid.api.*;

import java.util.Formatter;
import java.util.Optional;
import java.util.stream.Stream;

import static com.google.common.truth.Truth.assertThat;
import static dev.ucdm.test.util.TestFilesKt.oldTestDir;
import static dev.ucdm.test.util.TestFilesKt.testFilesIn;

/**
 * Test {@link GcdmCdmFile}
 */
public class TestGcdmGridDataset {

  public static Stream<Arguments> params() {
    return testFilesIn(oldTestDir + "/ft/grid")
            .addNameFilter(new FileFilterSkipSuffixes(".cdl .gbx9 aggFmrc.xml cg.ncml"))
            .addNameFilter(it -> !it.contains("RTMA_CONUS_2p5km"))
            .withRecursion()
            .build();
  }

  @ParameterizedTest
  @MethodSource("params")
  public void doOne(String filename) throws Exception {
    roundtrip(filename);
  }

  private static DataRoots dataRoots = new DataRoots();

  public static void roundtrip(String filename) throws Exception {
    String gcdmUrl = dataRoots.makeGcdmUrl(filename);

    System.out.printf("openGridDataset  %s%n", gcdmUrl);
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

  public static void readData(String filename, String gridName) throws Exception {
    String gcdmUrl = dataRoots.makeGcdmUrl(filename);

    System.out.printf("openGridDataset  %s%n", gcdmUrl);
    Formatter info = new Formatter();
    try (GridDataset local = GridDatasetFactory.openGridDataset(filename, info)) {
      assertThat(local).isNotNull();
      Grid localGrid = local.findGrid(gridName).orElseThrow();

      try (GridDataset remote = GridDatasetFactory.openGridDataset(gcdmUrl, info)) {
        assertThat(remote).isNotNull();
        assertThat(remote).isInstanceOf(GcdmGridDataset.class);
        Grid remoteGrid = remote.findGrid(gridName).orElseThrow();
        GridReferencedArray remoteArray = remoteGrid.readData(GridSubset.create());
        GridReferencedArray localArray = localGrid.readData(GridSubset.create());

        assertThat(remoteArray.name()).isEqualTo(localArray.name());
        assertThat(remoteArray.arrayType()).isEqualTo(localArray.arrayType());
        assertThat(remoteArray.data()).isEqualTo(localArray.data());
        assertThat(remoteArray.materializedCoordinateSystem()).isEqualTo(localArray.materializedCoordinateSystem());
      }
    }
  }

  public static void readVerticalTransform(String filename, String transformName) throws Exception {
    String gcdmUrl = dataRoots.makeGcdmUrl(filename);

    System.out.printf("openGridDataset  %s%n", gcdmUrl);
    Formatter info = new Formatter();
    try (GridDataset local = GridDatasetFactory.openGridDataset(filename, info)) {
      assertThat(local).isNotNull();
      VerticalTransform localVt = local.findVerticalTransformByName(transformName).orElseThrow();

      try (GridDataset remote = GridDatasetFactory.openGridDataset(gcdmUrl, info)) {
        assertThat(remote).isNotNull();
        assertThat(remote).isInstanceOf(GcdmGridDataset.class);
        VerticalTransform remoteVt = remote.findVerticalTransformByName(transformName).orElseThrow();

        Array<?> remoteArray = remoteVt.getCoordinateArray3D(0);
        Array<?> localArray = localVt.getCoordinateArray3D(0);
        assertThat(remoteArray).isEqualTo(localArray);
      }
    }
  }

}
