package dev.ucdm.grib.common;

import dev.ucdm.grib.collection.GribCollection;
import dev.ucdm.grib.inventory.DirectoryPartition;
import dev.ucdm.grib.inventory.MPartition;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Formatter;

import static com.google.common.truth.Truth.assertThat;
import static dev.ucdm.grib.collection.GribPartitionIndexKt.createPartitionIndex;
import static dev.ucdm.test.util.TestFilesKt.oldTestDir;

public class TestGribPofP {

  @Test
  public void testCreatePofPGrib1() throws IOException {
     MPartition dcm = new DirectoryPartition("testCreatePartitionGrib1",
            Path.of(oldTestDir + "gribCollections/gfs_conus80"),
            true, "*.grib1", null, null);

    GribConfig config = new GribConfig();
    Formatter errlog = new Formatter();

    try (GribCollection gc = createPartitionIndex(true, dcm, config, errlog)) {
      assertThat(gc).isNotNull();
      assertThat(gc.center).isEqualTo(7);
    } catch (Throwable t) {
      System.out.printf("errlog = '%s'%n", errlog);
      t.printStackTrace();
    }
  }

}
