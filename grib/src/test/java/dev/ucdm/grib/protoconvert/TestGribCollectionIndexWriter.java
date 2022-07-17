package dev.ucdm.grib.protoconvert;

import dev.ucdm.core.io.RandomAccessFile;
import dev.ucdm.grib.collection.CollectionUpdateType;
import dev.ucdm.grib.collection.GribCollection;
import dev.ucdm.grib.common.GribCollectionIndex;
import dev.ucdm.grib.common.GribConfig;
import dev.ucdm.grib.common.TestGribCollectionIndex;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.util.Formatter;
import java.util.stream.Stream;

import static com.google.common.truth.Truth.assertThat;
import static dev.ucdm.test.util.TestFilesKt.gribLocalDir;
import static dev.ucdm.test.util.TestFilesKt.testFilesIn;

public class TestGribCollectionIndexWriter {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TestGribCollectionIndex.class);

  public static Stream<Arguments> params() {
    return Stream.of(
            Arguments.of(gribLocalDir + "Lannion.pds31.grib2"),
            Arguments.of(gribLocalDir + "afwa.grib1")
    );
  }

  @ParameterizedTest
  @MethodSource("params")
  public void testGribCollectionIndexWriter(String testfile) throws IOException {

    GribConfig config = new GribConfig();
    Formatter errlog = new Formatter();

    try (RandomAccessFile raf = new RandomAccessFile(testfile, "r")) {
      try (GribCollection gc = GribCollectionIndex.openGribCollectionFromRaf(raf,
              CollectionUpdateType.always, config, errlog)) {
        assertThat(gc).isNotNull();
      } catch (Throwable t) {
        System.out.printf("errlog = '%s'%n", errlog);
        t.printStackTrace();
      }
    }
  }
}
