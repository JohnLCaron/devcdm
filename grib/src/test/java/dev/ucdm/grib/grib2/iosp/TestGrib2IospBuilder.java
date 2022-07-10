package dev.ucdm.grib.grib2.iosp;

import dev.cdm.core.io.RandomAccessFile;
import dev.ucdm.grib.common.GribIndex;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static com.google.common.truth.Truth.assertThat;
import static dev.ucdm.grib.common.GribCollectionIndex.NCX_SUFFIX;

public class TestGrib2IospBuilder {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TestGrib2IospBuilder.class);
  private static final String testfile = "/home/snake/tmp/rugley.pds15.grib2";

  @Test
  public void testIsValidFile() throws IOException {
    try (RandomAccessFile raf = new RandomAccessFile(testfile, "r")) {
      assertThat(new Grib2Iosp().isValidFile(raf)).isTrue();
    }

    try (RandomAccessFile raf = new RandomAccessFile(testfile + NCX_SUFFIX, "r")) {
      assertThat(new Grib2Iosp().isValidFile(raf)).isFalse();
    }

    try (RandomAccessFile raf = new RandomAccessFile(testfile + GribIndex.GBX9_IDX, "r")) {
      assertThat(new Grib2Iosp().isValidFile(raf)).isFalse();
    }
  }


}
