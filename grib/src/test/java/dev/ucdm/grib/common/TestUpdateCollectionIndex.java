package dev.ucdm.grib.common;

import dev.ucdm.grib.collection.GribCollection;
import dev.ucdm.grib.inventory.CollectionUpdate;
import dev.ucdm.grib.inventory.DirectoryMCollection;
import dev.ucdm.grib.inventory.MCollection;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Formatter;

import static com.google.common.truth.Truth.assertThat;
import static dev.ucdm.test.util.TestFilesKt.oldTestDir;

public class TestUpdateCollectionIndex {

  @Test
  public void testUpdateCollectionIndex() throws IOException {

    String testfile = "/home/snake/tmp/rugley.pds15.grib2";
    var idxFile = new File(testfile + GribIndex.GBX_SUFFIX);
    if (idxFile.exists()) {
      idxFile.delete();
    }

    GribConfig config = new GribConfig();
    Formatter errlog = new Formatter();

    Path collectionDir = Path.of(oldTestDir + "gribCollections/hrrr/");
    MCollection dcm = new DirectoryMCollection("topdog", collectionDir, true, "*.grib2", null, null);

      try (GribCollection gc = GribCollectionIndex.updateCollectionIndex(false,
              dcm, CollectionUpdate.always, config, errlog)) {
        assertThat(gc).isNotNull();
      } catch (Throwable t) {
        System.out.printf("errlog = '%s'%n", errlog);
        t.printStackTrace();
      }
  }

  @Test
  public void testFromGbx9() throws IOException {
    GribConfig config = new GribConfig();
    Formatter errlog = new Formatter();

    Path collectionDir = Path.of(oldTestDir + "gribCollections/namAlaska22/");
    MCollection dcm = new DirectoryMCollection("topdog", collectionDir, true, "*.gbx9", null, null);

    try (GribCollection gc = GribCollectionIndex.updateCollectionIndex(false,
            dcm, CollectionUpdate.always, config, errlog)) {
      assertThat(gc).isNotNull();

      // assertThat(gc.fileSize)
    } catch (Throwable t) {
      System.out.printf("errlog = '%s'%n", errlog);
      t.printStackTrace();
    }
  }
}
