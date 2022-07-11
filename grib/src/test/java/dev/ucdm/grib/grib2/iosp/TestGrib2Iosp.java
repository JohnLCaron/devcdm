package dev.ucdm.grib.grib2.iosp;

import dev.cdm.core.api.CdmFile;
import dev.cdm.core.api.Group;
import dev.cdm.core.io.RandomAccessFile;
import dev.ucdm.grib.collection.GribCollection;
import dev.ucdm.grib.collection.CollectionUpdateType;
import dev.ucdm.grib.common.GribConfig;
import dev.ucdm.grib.common.GribIndex;
import dev.ucdm.grib.common.GribCollectionIndex;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Formatter;

import static com.google.common.truth.Truth.assertThat;
import static dev.ucdm.grib.common.GribCollectionIndex.NCX_SUFFIX;

public class TestGrib2Iosp {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TestGrib2Iosp.class);
  private static final String testfile = "/home/snake/tmp/rugley.pds15.grib2";

  @Test
  public void testIsValidFile() throws IOException {
    try (RandomAccessFile raf = new RandomAccessFile(testfile, "r")) {
      assertThat(new Grib2Iosp().isValidFile(raf)).isTrue();
    }

    try (RandomAccessFile raf = new RandomAccessFile(testfile + NCX_SUFFIX, "r")) {
      assertThat(new Grib2Iosp().isValidFile(raf)).isTrue();
    }

    try (RandomAccessFile raf = new RandomAccessFile(testfile + GribIndex.GBX9_IDX, "r")) {
      assertThat(new Grib2Iosp().isValidFile(raf)).isFalse();
    }
  }

  @Test
  public void testRafConstructor() throws IOException {

    try (RandomAccessFile raf = new RandomAccessFile(testfile, "r")) {
      Grib2Iosp spi = new Grib2Iosp();

      Group.Builder root = Group.builder().setName("");
      CdmFile.Builder<?> builder = CdmFile.builder().setIosp(spi).setLocation(testfile);
      spi.build(raf, root, null);
      builder.setRootGroup(root);
      CdmFile ncfile = builder.build();
      spi.buildFinish(ncfile);

      assertThat(ncfile).isNotNull();
      assertThat(ncfile.getCdmFileTypeId()).isEqualTo("GRIB-2");
      assertThat(ncfile.getCdmFileTypeVersion()).isEqualTo("N/A");
      assertThat(ncfile.getCdmFileTypeDescription()).isEqualTo("GRIB2 Collection");
    }
  }

  @Test
  public void testCollectionConstructor() throws IOException {

    GribConfig config = new GribConfig();
    Formatter errlog = new Formatter();

    try (RandomAccessFile raf = new RandomAccessFile(testfile + NCX_SUFFIX, "r")) {
      try (GribCollection gc = GribCollectionIndex.openGribCollectionFromRaf(
              raf, CollectionUpdateType.test, config, new Formatter())) {

        Grib2Iosp spi = new Grib2Iosp(gc);

        Group.Builder root = Group.builder().setName("");
        CdmFile.Builder<?> builder = CdmFile.builder().setIosp(spi).setLocation(testfile);
        spi.build(raf, root, null);
        builder.setRootGroup(root);
        CdmFile ncfile = builder.build();
        spi.buildFinish(ncfile);

        assertThat(ncfile).isNotNull();
        assertThat(ncfile.getCdmFileTypeId()).isEqualTo("GRIB-2");
        assertThat(ncfile.getCdmFileTypeVersion()).isEqualTo("N/A");
        assertThat(ncfile.getCdmFileTypeDescription()).isEqualTo("GRIB2 Collection");

      } catch (Throwable t) {
        System.out.printf("errlog = '%s'%n", errlog);
        t.printStackTrace();
      }
    }
  }

}
