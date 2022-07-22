package dev.ucdm.grib.common;

import dev.ucdm.core.io.RandomAccessFile;
import dev.ucdm.grib.collection.GribCollection;
import dev.ucdm.grib.inventory.MCollection;
import dev.ucdm.grib.inventory.MFile;
import dev.ucdm.grib.inventory.MFileOS;
import dev.ucdm.grib.inventory.CollectionUpdate;
import dev.ucdm.grib.inventory.SingleFileMCollection;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.Formatter;

import static com.google.common.truth.Truth.assertThat;
import static dev.ucdm.grib.common.GribCollectionIndex.NCX_SUFFIX;
import static dev.ucdm.test.util.TestFilesKt.gribLocalDir;

public class TestSingleFileMCollection {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TestSingleFileMCollection.class);
  private static final String testfile = gribLocalDir + "rugley.pds15.grib2";

  @Test
  public void testSingleFileMCollection() {

    File dataFile = new File(testfile);
    MFile mfile = new MFileOS(dataFile);
    GribConfig config = new GribConfig();
    MCollection dcm = new SingleFileMCollection(mfile);
    Formatter errlog = new Formatter();

    try (GribCollection gc = GribCollectionIndex.updateCollectionIndex(false, dcm,
            CollectionUpdate.test, config, errlog)) {
      assertThat(gc).isNotNull();
      assertThat(gc.center).isEqualTo(7);
    } catch (Throwable t) {
      System.out.printf("errlog = '%s'%n", errlog);
      t.printStackTrace();
    }
  }

  @Test
  public void testOpenGribCollectionFromDataFile() throws IOException {

    GribConfig config = new GribConfig();
    Formatter errlog = new Formatter();

    try (RandomAccessFile raf = new RandomAccessFile(testfile, "r")) {
      try (GribCollection gc = GribCollectionIndex.openGribCollectionFromDataFile(false, raf,
              CollectionUpdate.test, config, errlog)) {
        assertThat(gc).isNotNull();
        assertThat(gc.center).isEqualTo(7);
      } catch (Throwable t) {
        System.out.printf("errlog = '%s'%n", errlog);
        t.printStackTrace();
      }
    }
  }

  @Test
  public void testOpenGribCollectionFromDataRaf() throws IOException {

    GribConfig config = new GribConfig();
    Formatter errlog = new Formatter();

    try (RandomAccessFile raf = new RandomAccessFile(testfile, "r")) {
      try (GribCollection gc = GribCollectionIndex.openGribCollectionFromRaf(
              raf, CollectionUpdate.test, config, errlog)) {
        assertThat(gc).isNotNull();
        assertThat(gc.center).isEqualTo(7);
      } catch (Throwable t) {
        System.out.printf("errlog = '%s'%n", errlog);
        t.printStackTrace();
      }
    }
  }

  @Test
  public void testOpenGribCollectionFromIndexRaf() throws IOException {

    GribConfig config = new GribConfig();
    Formatter errlog = new Formatter();

    try (RandomAccessFile raf = new RandomAccessFile(testfile + NCX_SUFFIX, "r")) {
      try (GribCollection gc = GribCollectionIndex.openGribCollectionFromRaf(
              raf, CollectionUpdate.test, config, errlog)) {
        assertThat(gc).isNotNull();
        assertThat(gc.center).isEqualTo(7);
      } catch (Throwable t) {
        System.out.printf("errlog = '%s'%n", errlog);
        t.printStackTrace();
      }
    }
  }

}
