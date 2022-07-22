package dev.ucdm.grib.collection;

import dev.ucdm.core.io.RandomAccessFile;
import dev.ucdm.grib.common.GribCollectionIndex;
import dev.ucdm.grib.common.GribConfig;
import dev.ucdm.grib.common.TestSingleFileMCollection;
import dev.ucdm.grib.inventory.CollectionUpdate;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.util.Formatter;
import java.util.stream.Stream;

import static com.google.common.truth.Truth.assertThat;
import static dev.ucdm.test.util.TestFilesKt.testFilesIn;

public class TestGribCollectionBuilder {

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
              CollectionUpdate.always, config, errlog)) {
        assertThat(gc).isNotNull();
      } catch (Throwable t) {
        System.out.printf("errlog = '%s'%n", errlog);
        t.printStackTrace();
      }
    }
  }

  @BeforeAll
  public static void startup() {
    RandomAccessFile.setDebugLeaks(true);
  }

  @AfterAll
  public static void shutdown() {
    RandomAccessFile.setDebugLeaks(false);
    System.out.printf("%s%n", RandomAccessFile.showDebugLeaks(new Formatter(), false));
    assertThat(RandomAccessFile.leftOpen()).isEqualTo(0);
  }
}
