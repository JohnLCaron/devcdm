package dev.ucdm.grib.common;

import dev.ucdm.core.io.RandomAccessFile;
import dev.ucdm.grib.cli.ncxupdate.NcxUpdate;
import dev.ucdm.grib.collection.CollectionType;
import dev.ucdm.grib.collection.GribCollection;
import dev.ucdm.grib.inventory.CollectionUpdate;
import dev.ucdm.grib.inventory.FilePartition;
import dev.ucdm.grib.inventory.PartitionType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Formatter;
import java.util.stream.Stream;

import static com.google.common.truth.Truth.assertThat;
import static dev.ucdm.grib.collection.GribPartitionIndexKt.updatePartitionIndex;
import static dev.ucdm.test.util.TestFilesKt.oldTestDir;

public class TestGribPartitionCreation {
  public static Stream<Arguments> params() {
    return Stream.of(
            Arguments.of(oldTestDir + "ft/grid/ensemble/jitka/", "*.grib", false),
            Arguments.of(oldTestDir + "ft/grid/ensemble/jitka/", "*_GB", true),
            Arguments.of(oldTestDir + "tds_index/NCEP/NBM/Ocean/", "*.gbx9", false),
            Arguments.of(oldTestDir + "gribCollections/gfs_2p5deg/", "*.grib2", false),
            Arguments.of(oldTestDir + "gribCollections/gfs_conus80/", "*.grib1", true),
            Arguments.of(oldTestDir + "tds_index/NCEP/NDFD/NWS/", "*.gbx9", false),
            Arguments.of(oldTestDir + "gribCollections/anal/", "*.grib2", false),
            Arguments.of(oldTestDir + "tds_index/NCEP/MRMS/Radar/", "*.gbx9", false),
            Arguments.of(oldTestDir + "tds_index/NCEP/NAM/CONUS_80km/", "*.gbx9", true)
    );
  }

  @ParameterizedTest
  @MethodSource("params")
  public void testCreation(String topdir, String glob, boolean isGrib1) {
      NcxUpdate updater = new NcxUpdate("test-file", topdir, glob, isGrib1, PartitionType.file, CollectionUpdate.always);
      updater.update();

    NcxUpdate updater2 = new NcxUpdate("test-directory", topdir, glob, isGrib1, PartitionType.directory, CollectionUpdate.always);
    updater2.update();
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
