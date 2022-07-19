package dev.ucdm.grib.common;

import dev.ucdm.core.io.RandomAccessFile;
import dev.ucdm.grib.inventory.MCollectionSingleFile;
import dev.ucdm.grib.collection.GribCollection;
import dev.ucdm.grib.inventory.MCollection;
import dev.ucdm.grib.inventory.MFile;
import dev.ucdm.grib.inventory.MFileOS;
import dev.ucdm.grib.collection.CollectionUpdateType;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.Formatter;

import static com.google.common.truth.Truth.assertThat;
import static dev.ucdm.grib.common.GribCollectionIndex.NCX_SUFFIX;

public class TestGribCollectionIndex {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TestGribCollectionIndex.class);
  private static final String testfile = "/home/snake/tmp/rugley.pds15.grib2";

  @Test
  public void testReadOrCreateCollectionFromIndex() throws IOException {

    File dataFile = new File(testfile);
    MFile mfile = new MFileOS(dataFile);
    GribConfig config = new GribConfig();
    MCollection dcm = new MCollectionSingleFile(mfile).setAuxInfo(GribConfig.AUX_CONFIG, config);
    Formatter errlog = new Formatter();

    try (GribCollection gc = GribCollectionIndex.readOrCreateCollectionFromIndex(false, dcm,
            CollectionUpdateType.test, config, errlog)) {
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
              CollectionUpdateType.test, config, errlog)) {
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
              raf, CollectionUpdateType.test, config, errlog)) {
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
              raf, CollectionUpdateType.test, config, errlog)) {
        assertThat(gc).isNotNull();
        assertThat(gc.center).isEqualTo(7);
      } catch (Throwable t) {
        System.out.printf("errlog = '%s'%n", errlog);
        t.printStackTrace();
      }
    }
  }

}
