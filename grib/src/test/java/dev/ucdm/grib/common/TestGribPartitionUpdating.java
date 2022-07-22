package dev.ucdm.grib.common;

import dev.ucdm.grib.collection.GribCollection;
import dev.ucdm.grib.inventory.CollectionUpdate;
import dev.ucdm.grib.inventory.FilePartition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Formatter;

import static com.google.common.truth.Truth.assertThat;
import static dev.ucdm.grib.collection.GribPartitionIndexKt.updatePartitionIndex;
import static dev.ucdm.test.util.TestFilesKt.oldTestDir;

@ResourceLock("testUpdate.anal.ncx4")
public class TestGribPartitionUpdating {
  private String exampleDataFile = oldTestDir + "gribCollections/anal/HRRR_CONUS_2p5km_ana_20150706_2000.grib2";

  @Test
  public void testUpdateAlways() throws IOException, InterruptedException {
    var filePartition = new FilePartition("testUpdate", Path.of(oldTestDir + "gribCollections/anal/"), false, "*.grib2", null, null);
    GribConfig config = new GribConfig();
    Formatter errlog = new Formatter();

    try (GribCollection gc = updatePartitionIndex(false, filePartition, CollectionUpdate.test, config, errlog)) {
      // should not recreate the index file
      assertThat(gc).isNotNull();
    }

    var idxFile = new File(filePartition.getIndexFilename());
    assertThat(idxFile.exists()).isTrue();
    long modified1 = idxFile.lastModified();
    Thread.sleep(100);

    try (GribCollection gc = updatePartitionIndex(false, filePartition, CollectionUpdate.always, config, errlog)) {
      // should not recreate the index file
      assertThat(gc).isNotNull();
    }
    assertThat(idxFile.exists()).isTrue();
    long modified2 = idxFile.lastModified();
    assertThat(modified2).isGreaterThan(modified1);
  }

  @Test
  public void testNoUpdateNeeded() throws IOException, InterruptedException {
    var filePartition = new FilePartition("testUpdate", Path.of(oldTestDir + "gribCollections/anal/"), false, "*.grib2", null, null);
    GribConfig config = new GribConfig();
    Formatter errlog = new Formatter();

    try (GribCollection gc = updatePartitionIndex(false, filePartition, CollectionUpdate.test, config, errlog)) {
      // should not recreate the index file
      assertThat(gc).isNotNull();
    }

    var idxFile = new File(filePartition.getIndexFilename());
    assertThat(idxFile.exists()).isTrue();
    long modified1 = idxFile.lastModified();
    Thread.sleep(100);

    try (GribCollection gc = updatePartitionIndex(false, filePartition, CollectionUpdate.test, config, errlog)) {
      // should not recreate the index file
      assertThat(gc).isNotNull();
    }
    assertThat(idxFile.exists()).isTrue();
    long modified2 = idxFile.lastModified();
    assertThat(modified2).isEqualTo(modified1);
  }

  /*
      // touch the data file
    var dataFile = new File(testfile);
    dataFile.setLastModified(System.currentTimeMillis());
    long dataTime = dataFile.lastModified();

    // do it again, should update
    try (RandomAccessFile raf = new RandomAccessFile(testfile, "r")) {
      GribCollection gc = GribCollectionIndex.openGribCollectionFromRaf(
              raf, CollectionUpdate.test, config, errlog);
      assertThat(gc).isNotNull();
    }

    long modified2 = new File(testfile + NCX_SUFFIX).lastModified();
    assertThat(modified2).isGreaterThan(modified1);
    System.out.printf("%d should be > %d%n", modified2, dataTime);
    // assertThat(modified2).isAtLeast(dataTime);
   */

  @Test
  public void testUpdateNeeded() throws IOException, InterruptedException {
    var filePartition = new FilePartition("testUpdate", Path.of(oldTestDir + "gribCollections/anal/"), false, "*.grib2", null, null);
    GribConfig config = new GribConfig();
    Formatter errlog = new Formatter();

    try (GribCollection gc = updatePartitionIndex(false, filePartition, CollectionUpdate.test, config, errlog)) {
      // should not recreate the index file
      assertThat(gc).isNotNull();
    }

    var idxFile = new File(filePartition.getIndexFilename());
    assertThat(idxFile.exists()).isTrue();
    long modified1 = idxFile.lastModified();

    // touch a data file
    var dataFile = new File(exampleDataFile);
    assertThat(dataFile.setLastModified(System.currentTimeMillis())).isTrue();
    long dataTime = dataFile.lastModified();
    Thread.sleep(100);

    // now force lastModified to be recalculated
    filePartition.iterateOverMCollections(it -> {});

    try (GribCollection gc = updatePartitionIndex(false, filePartition, CollectionUpdate.test, config, errlog)) {
      // should recreate the index file
      assertThat(gc).isNotNull();
    }

    long modified2 = idxFile.lastModified();
    assertThat(modified2).isGreaterThan(modified1);
    System.out.printf("%d should be > %d%n", modified2, dataTime);
  }

  @Test
  public void testNocheck() throws IOException, InterruptedException {
    var filePartition = new FilePartition("testUpdate", Path.of(oldTestDir + "gribCollections/anal/"), false, "*.grib2", null, null);
    GribConfig config = new GribConfig();
    Formatter errlog = new Formatter();

    try (GribCollection gc = updatePartitionIndex(false, filePartition, CollectionUpdate.test, config, errlog)) {
      // should not recreate the index file
      assertThat(gc).isNotNull();
    }

    var idxFile = new File(filePartition.getIndexFilename());
    assertThat(idxFile.exists()).isTrue();
    long modified1 = idxFile.lastModified();

    // touch a data file
    var dataFile = new File(exampleDataFile);
    assertThat(dataFile.setLastModified(System.currentTimeMillis())).isTrue();
    long dataTime = dataFile.lastModified();
    Thread.sleep(100);

    // now force lastModified to be recalculated
    filePartition.iterateOverMCollections(it -> {});

    // should not update
    try (GribCollection gc = updatePartitionIndex(false, filePartition, CollectionUpdate.nocheck, config, errlog)) {
      assertThat(gc).isNotNull();
    }

    long modified2 = idxFile.lastModified();
    assertThat(modified2).isEqualTo(modified1); // no update
    System.out.printf("%d should be > %d%n", modified2, dataTime);
    assertThat(modified2).isLessThan(dataTime);
  }

  @Test
  public void testUpdateNever() throws IOException {
    var filePartition = new FilePartition("testUpdate", Path.of(oldTestDir + "gribCollections/anal/"), false, "*.grib2", null, null);

    var idxFile = new File(filePartition.getIndexFilename());
    if (idxFile.exists()) {
      assertThat(idxFile.delete()).isTrue();
    }

    GribConfig config = new GribConfig();
    Formatter errlog = new Formatter();
    try (GribCollection gc = updatePartitionIndex(false, filePartition, CollectionUpdate.never, config, errlog)) {
      // should not recreate the index file
      assertThat(gc).isNull();
    }

    var idxFile2 = new File(filePartition.getIndexFilename());
    assertThat(idxFile2.exists()).isFalse();
  }

}
