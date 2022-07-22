package dev.ucdm.grib.collection;

import dev.ucdm.core.io.RandomAccessFile;
import dev.ucdm.grib.common.GribCollectionIndex;
import dev.ucdm.grib.common.GribConfig;
import dev.ucdm.grib.common.TestSingleFileMCollection;
import dev.ucdm.grib.inventory.CollectionUpdate;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.util.Formatter;
import java.util.List;
import java.util.stream.Stream;

import static com.google.common.truth.Truth.assertThat;
import static dev.ucdm.test.util.TestFilesKt.oldTestDir;

public class TestGribPartitionReading {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TestSingleFileMCollection.class);

  public static Stream<Arguments> params() {
    return Stream.of(
            Arguments.of(oldTestDir + "tds_index/NCEP/MRMS/Radar/MRMS_Radar_20201027_0000.grib2.ncx4", CollectionType.MRUTC),
            Arguments.of(oldTestDir + "tds_index/NCEP/MRMS/Radar/MRMS-Radar.ncx4", CollectionType.MRUTP),
            Arguments.of(oldTestDir + "tds_index/NCEP/NAM/CONUS_80km/NAM_CONUS_80km_20201027_0000.grib1.ncx4", CollectionType.SRC),
            Arguments.of(oldTestDir + "ft/grid/ensemble/jitka/MOEASURGEENS20100709060002.grib", CollectionType.SRC),
            Arguments.of(oldTestDir + "ft/grid/ensemble/jitka/ECME_RIZ_201201101200_00600_GB.ncx4", CollectionType.SRC),
            Arguments.of(oldTestDir + "tds_index/NCEP/NBM/Ocean/NCEP_OCEAN_MODEL_BLEND.ncx4", CollectionType.TwoD),
            Arguments.of(oldTestDir + "gribCollections/gfs_2p5deg/gfs_2p5deg.ncx4", CollectionType.TwoD),
            Arguments.of(oldTestDir + "tds_index/NCEP/NDFD/NWS/NDFD_NWS_CONUS_CONDUIT.ncx4", CollectionType.TwoD),
            Arguments.of(oldTestDir + "gribCollections/anal/HRRRanalysis.ncx4", CollectionType.MRUTC),
            Arguments.of(oldTestDir + "gribCollections/anal/test-anal.ncx4", CollectionType.MRUTC)
    );
  }

  @ParameterizedTest
  @MethodSource("params")
  public void testGribPartitionReading(String testfile, CollectionType type) throws IOException {

    GribConfig config = new GribConfig();
    Formatter errlog = new Formatter();

    try (RandomAccessFile raf = new RandomAccessFile(testfile, "r")) {
      try (GribCollection gc = GribCollectionIndex.openGribCollectionFromRaf(raf,
              CollectionUpdate.never, config, errlog)) {
        assertThat(gc).isNotNull();
        List<CollectionType> types = gc.datasets.stream().map(ds -> ds.gctype).toList();
        System.out.printf("%s has types %s%n", testfile, types);
        assertThat(types).contains(type);

      } catch (Throwable t) {
        System.out.printf("errlog = '%s'%n", errlog);
        t.printStackTrace();
      }
    }
  }
}
