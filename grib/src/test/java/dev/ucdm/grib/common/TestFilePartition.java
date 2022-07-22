package dev.ucdm.grib.common;

import dev.ucdm.grib.collection.GribCollection;
import dev.ucdm.grib.inventory.FilePartition;
import dev.ucdm.grib.inventory.MPartition;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Formatter;

import static com.google.common.truth.Truth.assertThat;
import static dev.ucdm.grib.collection.GribPartitionIndexKt.createPartitionIndex;
import static dev.ucdm.test.util.TestFilesKt.oldTestDir;

public class TestFilePartition {

  @Test
  public void testCreatePartitionGrib1() throws IOException {
    //     topCollectionName: String,
    //    collectionDir: Path,
    //    val isTop: Boolean,
    //    glob: String?,
    //    val filter: DirectoryStream.Filter<Path>?,
    //    olderThanMillis: Long?

    MPartition dcm = new FilePartition("testCreatePartitionGrib1",
            Path.of(oldTestDir + "gribCollections/gfs_conus80/20141024"),
            true, "*.grib1", null, null);

    GribConfig config = new GribConfig();
    Formatter errlog = new Formatter();

    try (GribCollection gc = createPartitionIndex(true, dcm, config, errlog)) {
      assertThat(gc).isNotNull();
      assertThat(gc.center).isEqualTo(7);
      assertThat(gc.genProcessId).isEqualTo(81);
      assertThat(gc.datasets.get(0).groups.get(0).getDescription()).isEqualTo("LambertConformal_65X93 (Center 40.984, -100.08)");
    } catch (Throwable t) {
      System.out.printf("errlog = '%s'%n", errlog);
      t.printStackTrace();
    }

    MPartition dcm2 = new FilePartition("testCreatePartitionGrib1",
            Path.of(oldTestDir + "gribCollections/gfs_conus80/20141025"),
            true, "*.grib1", null, null);

    try (GribCollection gc = createPartitionIndex(true, dcm2, config, errlog)) {
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
  public void testCreatePartitionGrib2() throws IOException {

    MPartition dcm = new FilePartition("testCreatePartitionGrib2",
            Path.of(oldTestDir + "gribCollections/gfs_2p5deg"),
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
