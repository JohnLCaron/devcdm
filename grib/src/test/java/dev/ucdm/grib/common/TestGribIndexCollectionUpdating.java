package dev.ucdm.grib.common;

import dev.ucdm.core.io.RandomAccessFile;
import dev.ucdm.grib.collection.GribCollection;
import dev.ucdm.grib.inventory.CollectionUpdate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import java.io.File;
import java.io.IOException;
import java.util.Formatter;

import static com.google.common.truth.Truth.assertThat;
import static dev.ucdm.grib.common.GribCollectionIndex.NCX_SUFFIX;

@ResourceLock("rugley.pds15.grib2")
public class TestGribIndexCollectionUpdating {

  @Test
  public void testUpdateNever() throws IOException {
    String testfile = "/home/snake/tmp/rugley.pds15.grib2";
    var idxFile = new File(testfile + NCX_SUFFIX);
    if (idxFile.exists()) {
      assertThat(idxFile.delete()).isTrue();
    }

    GribConfig config = new GribConfig();
    Formatter errlog = new Formatter();
    try (RandomAccessFile raf = new RandomAccessFile(testfile, "r")) {
      GribCollection gc = GribCollectionIndex.openGribCollectionFromRaf(
              raf, CollectionUpdate.never, config, errlog);
      // should not recreate the index file
      assertThat(gc).isNull();
    }

    assertThat(idxFile.exists()).isFalse();
  }

  @Test
  public void testUpdateAlways() throws IOException, InterruptedException {
    String testfile = "/home/snake/tmp/rugley.pds15.grib2";
    GribConfig config = new GribConfig();
    Formatter errlog = new Formatter();

    try (RandomAccessFile raf = new RandomAccessFile(testfile, "r")) {
      GribCollection gc = GribCollectionIndex.openGribCollectionFromRaf(
              raf, CollectionUpdate.always, config, errlog);
      // should recreate the index file
      assertThat(gc).isNotNull();
    }

    var idxFile = new File(testfile + NCX_SUFFIX);
    assertThat(idxFile.exists()).isTrue();
    long modified1 = idxFile.lastModified();
    Thread.sleep(100);

    // do it again
    try (RandomAccessFile raf = new RandomAccessFile(testfile, "r")) {
      GribCollection gc = GribCollectionIndex.openGribCollectionFromRaf(
              raf, CollectionUpdate.always, config, errlog);
      assertThat(gc).isNotNull();
    }

    assertThat(idxFile.exists()).isTrue();
    long modified2 = idxFile.lastModified();
    assertThat(modified2).isGreaterThan(modified1);
  }

  @Test
  public void testNoUpdateNeeded() throws IOException, InterruptedException {
    String testfile = "/home/snake/tmp/rugley.pds15.grib2";
    GribConfig config = new GribConfig();
    Formatter errlog = new Formatter();

    try (RandomAccessFile raf = new RandomAccessFile(testfile, "r")) {
      GribCollection gc = GribCollectionIndex.openGribCollectionFromRaf(
              raf, CollectionUpdate.test, config, errlog);
      // should recreate the index file
      assertThat(gc).isNotNull();
    }

    var idxFile = new File(testfile + NCX_SUFFIX);
    assertThat(idxFile.exists()).isTrue();
    long modified1 = idxFile.lastModified();
    Thread.sleep(100);

    // do it again
    try (RandomAccessFile raf = new RandomAccessFile(testfile, "r")) {
      GribCollection gc = GribCollectionIndex.openGribCollectionFromRaf(
              raf, CollectionUpdate.test, config, errlog);
      assertThat(gc).isNotNull();
    }

    assertThat(idxFile.exists()).isTrue();
    long modified2 = idxFile.lastModified();
    assertThat(modified2).isEqualTo(modified1);
  }

  @Test
  public void testUpdateNeeded() throws IOException, InterruptedException {
    String testfile = "/home/snake/tmp/rugley.pds15.grib2";
    GribConfig config = new GribConfig();
    Formatter errlog = new Formatter();

    try (RandomAccessFile raf = new RandomAccessFile(testfile, "r")) {
      GribCollection gc = GribCollectionIndex.openGribCollectionFromRaf(
              raf, CollectionUpdate.test, config, errlog);
      // should recreate the index file
      assertThat(gc).isNotNull();
    }

    var idxFile = new File(testfile + NCX_SUFFIX);
    assertThat(idxFile.exists()).isTrue();
    long modified1 = idxFile.lastModified();

    // touch the data file
    var dataFile = new File(testfile);
    assertThat(dataFile.setLastModified(System.currentTimeMillis())).isTrue();
    long dataTime = dataFile.lastModified();
    Thread.sleep(100);

    // do it again, should update
    try (RandomAccessFile raf = new RandomAccessFile(testfile, "r")) {
      GribCollection gc = GribCollectionIndex.openGribCollectionFromRaf(
              raf, CollectionUpdate.test, config, errlog);
      assertThat(gc).isNotNull();
    }

    long modified2 = new File(testfile + NCX_SUFFIX).lastModified();
    assertThat(modified2).isGreaterThan(modified1);
    System.out.printf("%d should be > %d%n", modified2, dataTime);
    assertThat(modified2).isAtLeast(dataTime);
  }

  @Test
  public void testNoCheck() throws IOException, InterruptedException {
    String testfile = "/home/snake/tmp/rugley.pds15.grib2";
    GribConfig config = new GribConfig();
    Formatter errlog = new Formatter();

    try (RandomAccessFile raf = new RandomAccessFile(testfile, "r")) {
      GribCollection gc = GribCollectionIndex.openGribCollectionFromRaf(
              raf, CollectionUpdate.test, config, errlog);
      // should recreate the index file
      assertThat(gc).isNotNull();
    }

    var idxFile = new File(testfile + NCX_SUFFIX);
    assertThat(idxFile.exists()).isTrue();
    long modified1 = idxFile.lastModified();

    // touch the data file
    var dataFile = new File(testfile);
    assertThat(dataFile.setLastModified(System.currentTimeMillis())).isTrue();
    long dataTime = dataFile.lastModified();
    Thread.sleep(100);

    // do it again, should not update
    try (RandomAccessFile raf = new RandomAccessFile(testfile, "r")) {
      GribCollection gc = GribCollectionIndex.openGribCollectionFromRaf(
              raf, CollectionUpdate.nocheck, config, errlog);
      assertThat(gc).isNotNull();
    }

    long modified2 = idxFile.lastModified();
    assertThat(modified2).isEqualTo(modified1); // no update
    System.out.printf("%d should be > %d%n", modified2, dataTime);
    assertThat(modified2).isLessThan(dataTime);
  }

}
