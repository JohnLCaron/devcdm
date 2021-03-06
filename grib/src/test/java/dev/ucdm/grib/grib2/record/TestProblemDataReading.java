/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.ucdm.grib.grib2.record;

import dev.ucdm.core.io.RandomAccessFile;
import org.junit.jupiter.api.Test;

import java.io.IOException;

/** Test problems reading data */
public class TestProblemDataReading {

  @Test
  public void compareProblemFile() throws IOException {
    // String filename = TestDir.cdmUnitTestDir + "formats/grib2/gfs_4_20130830_1800_144.grb2";
    String filename = "/home/snake/Downloads/ECMWF_CAMS_total_column_nitric_acid2019010100.grib2";
    testRead(filename);
  }

  private void testRead(String filename) throws IOException {
    readFile(filename, (raf, gr) -> {
      Grib2SectionData ds = gr.getDataSection();
      System.out.printf("Grib2SectionData %s end %d%n", ds, ds.getEndingPosition());
      float[] data = gr.readData(raf);
    });
  }

  private void readFile(String path, TestGrib2Records.Callback callback) throws IOException {
    try (RandomAccessFile raf = new RandomAccessFile(path, "r")) {
      raf.order(RandomAccessFile.BIG_ENDIAN);
      raf.seek(0);
      System.out.printf("Filename %s len = %s%n", path, raf.length());

      Grib2RecordScanner reader = new Grib2RecordScanner(raf);
      while (reader.hasNext()) {
        Grib2Record gr = reader.next();
        if (gr == null)
          break;
        callback.call(raf, gr);
      }
    }
  }

}
