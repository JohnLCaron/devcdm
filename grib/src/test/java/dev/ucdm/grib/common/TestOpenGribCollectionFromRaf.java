package dev.ucdm.grib.common;

import dev.ucdm.core.io.RandomAccessFile;
import dev.ucdm.grib.inventory.CollectionUpdate;
import dev.ucdm.grib.collection.GribCollection;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Formatter;
import java.util.stream.Stream;

import static com.google.common.truth.Truth.assertThat;
import static dev.ucdm.test.util.TestFilesKt.oldTestDir;
import static org.junit.jupiter.api.Assertions.fail;

public class TestOpenGribCollectionFromRaf {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TestSingleFileMCollection.class);
  private static final String gridTestDir = "../grib/src/test/data/";

  public static Stream<Arguments> params() {
    return Stream.of(
            Arguments.of(oldTestDir + "tds_index/NCEP/NDFD/SPC/NDFD_SPC_CONUS_CONDUIT.ncx4", false, false),
            Arguments.of(gridTestDir + "index/grib1.proto2.gbx9", true, true),
            Arguments.of(gridTestDir + "afwa.grib1", true, false),
            Arguments.of(gridTestDir + "afwa.grib1.ncx4", true, false),
            Arguments.of(gridTestDir + "index/grib2.proto2.gbx9", false, true),
            Arguments.of(gridTestDir + "sref_eta.grib2", false, false),
            Arguments.of(gridTestDir + "sref_eta.grib2.ncx4", false, false)
    );
  }

  @ParameterizedTest
  @MethodSource("params")
  public void testOpen(String filename, boolean isGrib1, boolean expectFail) {
    System.out.printf("TestValidCollection %s%n", filename);
    try (RandomAccessFile raf = new RandomAccessFile(filename, "r")) {
      GribCollection gc = GribCollectionIndex.openGribCollectionFromRaf(
              raf, CollectionUpdate.never, new GribConfig(), new Formatter());
      if (gc == null) {
        if (!expectFail) fail();
        return;
      }
      if (expectFail) fail();

      assertThat(gc.isGrib1).isEqualTo(isGrib1);

    } catch (Exception e) {
      if (!expectFail) {
        fail();
      }
    }
  }

}
