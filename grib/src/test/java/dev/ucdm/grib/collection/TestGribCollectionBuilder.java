package dev.ucdm.grib.collection;

import dev.cdm.core.io.RandomAccessFile;
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
import static dev.cdm.test.util.TestFilesKt.testFilesIn;

public class TestGribCollectionBuilder {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TestGribCollectionIndex.class);

  public static Stream<Arguments> params() {
    return testFilesIn("src/test/data/")
            .addNameFilter(it -> it.endsWith("grib1") || it.endsWith("grib2"))
            .build();
  }

  @ParameterizedTest
  @MethodSource("params")
  public void testGribCollectionBuilder(String testfile) throws IOException {

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
