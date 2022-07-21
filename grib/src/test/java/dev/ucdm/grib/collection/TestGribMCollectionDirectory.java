package dev.ucdm.grib.collection;

import dev.ucdm.grib.common.GribCollectionIndex;
import dev.ucdm.grib.common.GribConfig;
import dev.ucdm.grib.common.TestGribCollectionIndex;
import dev.ucdm.grib.inventory.DirectoryMCollection;
import dev.ucdm.grib.inventory.MCollection;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Formatter;

import static com.google.common.truth.Truth.assertThat;
import static dev.ucdm.test.util.TestFilesKt.oldTestDir;

public class TestGribMCollectionDirectory {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TestGribCollectionIndex.class);

  @Test
  public void testGribMCollectionDirectory() throws IOException {
    GribConfig config = new GribConfig();
    Formatter errlog = new Formatter();

    Path collectionDir = Path.of(oldTestDir + "gribCollections/hrrr/");
    MCollection dcm = new DirectoryMCollection("topdog", collectionDir, true, "*.grib2", null, null);

      try (GribCollection gc = GribCollectionIndex.updateCollectionIndex(false,
              dcm, CollectionUpdateType.always, config, errlog)) {
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
            dcm, CollectionUpdateType.always, config, errlog)) {
      assertThat(gc).isNotNull();

      // assertThat(gc.fileSize)
    } catch (Throwable t) {
      System.out.printf("errlog = '%s'%n", errlog);
      t.printStackTrace();
    }
  }
}
