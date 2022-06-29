/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package dev.cdm.gcdm;

import dev.cdm.gcdm.protogen.GcdmGridProto;
import dev.cdm.grid.api.GridDataset;
import dev.cdm.grid.api.GridDatasetFactory;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import dev.cdm.gcdm.client.GcdmGridDataset;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Formatter;
import java.util.stream.Stream;

import static com.google.common.truth.Truth.assertThat;

/**
 * Test {@link GcdmGridConverter} by roundtripping and comparing with original. Metadata only.
 */
@RunWith(Parameterized.class)
public class TestGcdmGridConverter {

  public static Stream<Arguments> params() {
    return Stream.of(
            Arguments.of(TestGcdmDatasets.coreLocalDir + "permuteTest.nc"),
            Arguments.of(TestGcdmDatasets.testDir + "gribCollections/gfs_2p5deg/gfs_2p5deg.ncx4"),
            Arguments.of(TestGcdmDatasets.testDir + "ft/grid/namExtract/20060926_0000.nc"),
            Arguments.of(TestGcdmDatasets.coreLocalDir + "ncml/fmrc/GFS_Puerto_Rico_191km_20090729_0000.nc"),
            Arguments.of(TestGcdmDatasets.testDir + "conventions/coards/inittest24.QRIDV07200.ncml"),
            Arguments.of(TestGcdmDatasets.testDir + "conventions/nuwg/avn-x.nc"),

            Arguments.of(
                    TestGcdmDatasets.testDir + "tds_index/NCEP/NAM/CONUS_80km/NAM_CONUS_80km_20201027_0000.grib1.ncx4"),
            Arguments.of(TestGcdmDatasets.testDir + "ft/grid/ensemble/jitka/ECME_RIZ_201201101200_00600_GB.ncx4"),
            Arguments.of(TestGcdmDatasets.testDir + "gribCollections/gfs_2p5deg/gfs_2p5deg.ncx4"),

            Arguments.of(TestGcdmDatasets.testDir + "tds_index/NCEP/MRMS/Radar/MRMS-Radar.ncx4"),
            Arguments.of(TestGcdmDatasets.testDir + "tds_index/NCEP/MRMS/Radar/MRMS_Radar_20201027_0000.grib2.ncx4"),

            // Offset (orthogonal)
            Arguments.of(TestGcdmDatasets.testDir + "gribCollections/gfs_2p5deg/gfs_2p5deg.ncx4"),

            // orth, reg
            Arguments.of(TestGcdmDatasets.testDir + "tds_index/NCEP/NBM/Alaska/NCEP_ALASKA_MODEL_BLEND.ncx4"),

            // OffsetRegular
            Arguments.of(TestGcdmDatasets.testDir + "tds_index/NCEP/NDFD/NWS/NDFD_NWS_CONUS_CONDUIT_ver7.ncx4"),
            Arguments.of(TestGcdmDatasets.testDir + "tds_index/NCEP/NBM/Ocean/NCEP_OCEAN_MODEL_BLEND.ncx4"),

            // OffsetIrregular
            Arguments.of(TestGcdmDatasets.testDir + "tds_index/NCEP/NDFD/CPC/NDFD_CPC_CONUS_CONDUIT.ncx4"),
            Arguments.of(TestGcdmDatasets.testDir + "tds_index/NCEP/NDFD/NWS/NDFD_NWS_CONUS_CONDUIT.ncx4")
    );
  }

  @ParameterizedTest
  @MethodSource("params")
  public void testExample(String filename) throws Exception {
    filename = filename.replace("\\", "/");
    File file = new File(filename);
    System.out.printf("getAbsolutePath %s%n", file.getAbsolutePath());
    System.out.printf("getCanonicalPath %s%n", file.getCanonicalPath());

    // kludge for now. Also, need to auto start up CmdrServer
    String gcdmUrl = "gcdm://localhost:16111/" + file.getCanonicalPath();
    Path path = Paths.get(filename);

    Formatter errlog = new Formatter();
    try (GridDataset gridDataset = GridDatasetFactory.openGridDataset(path.toString(), errlog)) {
      assertThat(gridDataset).isNotNull();

      GcdmGridProto.GridDataset proto = GcdmGridConverter.encodeGridDataset(gridDataset);
      GcdmGridDataset.Builder builder = GcdmGridDataset.builder();
      GcdmGridConverter.decodeGridDataset(proto, builder, errlog);
      GcdmGridDataset roundtrip = builder.build(false);

      new CompareGridDataset(roundtrip, gridDataset, false).compare();
    }
  }

}
