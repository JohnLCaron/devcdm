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

public class TestDirectoryPartition {

  @Test
  public void testDirectoryPartitionGrib1() throws IOException {
    MPartition dcm = new DirectoryPartition("testCreatePartitionGrib1",
            Path.of(oldTestDir + "gribCollections/gfs_conus80"),
            true, "*.grib1", null, null);

    GribConfig config = new GribConfig();
    Formatter errlog = new Formatter();

    // boolean isGrib1, MPartition dcm, CollectionUpdateType update, GribConfig config, Formatter errlog) throws IOException {
    try (GribCollection gc = createPartitionIndex(true, dcm, config, errlog)) {
      assertThat(gc).isNotNull();
      assertThat(gc.center).isEqualTo(7);
      assertThat(gc.genProcessId).isEqualTo(81);
      assertThat(gc.datasets.get(0).groups.get(0).getDescription()).isEqualTo("LambertConformal_65X93 (Center 40.984, -100.08)");
    } catch (Throwable t) {
      System.out.printf("errlog = '%s'%n", errlog);
      t.printStackTrace();
    }
  }

  @Test
  public void testDirectoryPartitionGrib2() throws IOException {
     MPartition dcm = new DirectoryPartition("testDirectoryPartitionGrib2",
            Path.of(oldTestDir + "gribCollections/dgex"),
            true, "*.grib2", null, null);

    GribConfig config = new GribConfig();
    Formatter errlog = new Formatter();

    // boolean isGrib1, MPartition dcm, CollectionUpdateType update, GribConfig config, Formatter errlog) throws IOException {
    try (GribCollection gc = createPartitionIndex(false, dcm, config, errlog)) {
      assertThat(gc).isNotNull();
      assertThat(gc.center).isEqualTo(7);
    } catch (Throwable t) {
      System.out.printf("errlog = '%s'%n", errlog);
      t.printStackTrace();
    }
  }

}
