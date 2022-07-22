package dev.ucdm.grib.collection;

import com.google.common.primitives.Ints;
import dev.ucdm.array.Array;
import dev.ucdm.core.api.CdmFile;
import dev.ucdm.core.api.CdmFiles;
import dev.ucdm.core.api.Variable;
import dev.ucdm.grib.common.TestSingleFileMCollection;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;

import static com.google.common.truth.Truth.assertThat;
import static dev.ucdm.test.util.TestFilesKt.oldTestDir;

public class TestGribDataReading {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TestSingleFileMCollection.class);

  public static Stream<Arguments> params() {
    return Stream.of(
            Arguments.of(oldTestDir + "ft/grid/ensemble/jitka/MOEASURGEENS20100709060002.grib", CollectionType.SRC,
                    "VAR10-3-192_FROM_74-0--1_surface_ens", List.of(477, 1, 207, 198)),
            Arguments.of(oldTestDir + "ft/grid/ensemble/jitka/ECME_RIZ_201201101200_00600_GB.ncx4", CollectionType.SRC,
                    "Total_precipitation_surface", List.of(1, 51, 21, 31)),

            Arguments.of(oldTestDir + "gribCollections/gfs_2p5deg/GFS_Global_2p5deg_20150301_1800.grib2",CollectionType.SRC,
                    "Ice_cover_surface", List.of(93, 73, 144)),
            Arguments.of(oldTestDir + "gribCollections/gfs_2p5deg/gfs_2p5deg.ncx4", CollectionType.TwoD,
                    "TwoD/Ice_cover_surface", List.of(4, 93, 73, 144)),

            // PofP ??
            Arguments.of(oldTestDir + "gribCollections/dgex/dgex_46.ncx4", CollectionType.TwoD,
                    "TwoD/Temperature_height_above_ground", List.of(4, 18, 1, 303, 491)),
            Arguments.of(oldTestDir + "gribCollections/gfs_conus80/gfsConus80_dir.ncx4", CollectionType.TwoD,
                    "TwoD/Temperature_tropopause", List.of(6,21,65,93)),
            Arguments.of(oldTestDir + "gribCollections/gfs_conus80/gfsConus80_file.ncx4", CollectionType.TwoD,
                    "TwoD/Temperature_tropopause", List.of(6,21,65,93)),
            Arguments.of(oldTestDir + "gribCollections/gfs_conus80/gfsConus80_none.ncx4", CollectionType.MRC,
                    "Temperature_tropopause", List.of(6,21,65,93)),

            Arguments.of(oldTestDir + "gribCollections/hrrr/DewpointTempFromGsdHrrrrConus3surface.grib2.ncx4", CollectionType.MRUTP,
                    "DPT_P0_L103_GLC0_height_above_ground", List.of(57,1,1059,1799)),
            Arguments.of(oldTestDir + "gribCollections/hrrr/GSD_HRRR_CONUS_3km_surface.ncx4", CollectionType.MRUTP,
                    "DPT_P0_L103_GLC0_height_above_ground", List.of(57,1,1059,1799))
    );
  }

  @ParameterizedTest
  @MethodSource("params")
  public void testGribDataReading(String testfile, CollectionType type, String varName, List<Integer> shape) throws IOException {
    System.out.printf("%s %s%n", testfile, varName);
    try (CdmFile nc = CdmFiles.open(testfile)) {
      Variable var = nc.findVariable(varName);
      if (var == null) {
        nc.findVariable(varName);
      }
      assertThat(var != null);
      Array<Float> data = (Array<Float>) var.readArray();
      assertThat(data != null);
      System.out.printf("   has shape %s%n", java.util.Arrays.toString(data.getShape()));
      var ll = Ints.asList(data.getShape());
      assertThat(Ints.asList(data.getShape())).isEqualTo(shape);
    }
  }
}
