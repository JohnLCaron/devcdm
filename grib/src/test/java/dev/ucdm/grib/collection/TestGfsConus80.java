/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.ucdm.grib.collection;

import dev.ucdm.grib.common.GribCollectionIndex;
import dev.ucdm.grib.common.GribConfig;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static dev.ucdm.test.util.TestFilesKt.oldTestDir;

@RunWith(Parameterized.class)
public class TestGfsConus80 {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final String topdir = oldTestDir + "gribCollections/gfs_conus80";
  private static final String spec = topdir + "/**/.*grib1";

  @Parameterized.Parameters(name = "{0}")
  public static List<Object[]> getTestParameters() {
    List<Object[]> result = new ArrayList<>();

    result.add(new Object[] {"gfsConus80_dir", "directory", true});
    result.add(new Object[] {"gfsConus80_file", "file", true});
    result.add(new Object[] {"gfsConus80_none", "none", false});

    return result;
  }

  /*
  @org.junit.jupiter.api.Test
  public void testReadOrCreateCollectionFromIndex(String collectionName, String partitionType, Boolean isTwoD) throws IOException {

    GribConfig config = new GribConfig(collectionName, "test/" + collectionName,
            FeatureCollectionType.GRIB1, spec, null, null, null, partitionType, null);

    File dataFile = new File(testfile);
    MFile mfile = new MFileOS(dataFile);
    GribConfig config = new GribConfig();
    MCollection dcm = new CollectionSingleFile(mfile).setAuxInfo(GribConfig.AUX_CONFIG, config);
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
  public void testGFSconus80_dir() throws IOException {
    Indent indent = new Indent(2);

    // create it
    FeatureCollectionConfig config = new FeatureCollectionConfig(collectionName, "test/" + collectionName,
        FeatureCollectionType.GRIB1, spec, null, null, null, partitionType, null);

    System.out.printf("============== create %s %n", collectionName);
    try (GribCollection gc = GribCdmIndex.openGribCollection(config, CollectionUpdateType.always, logger)) {
      assertThat(gc).isNotNull();
      indent.incr();
      openGC(gc.getLocation(), config, indent);
      indent.decr();

      indent.incr();
      for (GribCollectionImmutable.Dataset dataset : gc.getDatasets()) {
        System.out.printf("%sdataset = %s %n", indent, dataset.getType());
      }

      for (MFile mfile : gc.getFiles()) {
        openGC(mfile.getPath(), config, indent);
      }
      indent.decr();
    }

    System.out.printf("done%n");
  }

  private void openGC(String indexFilename, FeatureCollectionConfig config, Indent indent) throws IOException {
    if (!indexFilename.endsWith(".ncx4"))
      return;

    try (GribCollection gc = GribCollectionIndex.readOrCreateCollectionFromIndex(false, dcm,
            CollectionUpdateType.test, config, errlog)) {
    try (GribCollection gc = GribCdmIndex.openCdmIndex(indexFilename, config, true, logger)) {
      assertThat(gc).isNotNull();
      System.out.printf("%sindex filename = %s %n", indent, gc.getLocation());

      indent.incr();
      for (GribCollectionImmutable.Dataset dataset : gc.getDatasets()) {
        System.out.printf("%sdataset = %s %n", indent, dataset.getType());
      }

      for (MFile mfile : gc.getFiles()) {
        openGC(mfile.getPath(), config, indent);
      }

      indent.decr();
    }
    System.out.printf("%n");
  }

   */

}
