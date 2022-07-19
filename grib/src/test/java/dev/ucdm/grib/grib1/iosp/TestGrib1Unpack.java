/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.ucdm.grib.grib1.iosp;

import dev.ucdm.array.*;
import dev.ucdm.core.api.*;
import dev.ucdm.core.io.RandomAccessFile;
import dev.ucdm.grib.collection.CollectionUpdateType;
import dev.ucdm.grib.inventory.MFile;
import dev.ucdm.grib.inventory.MFileOS;
import dev.ucdm.grib.common.GribIndex;
import dev.ucdm.grib.common.util.GribData;
import dev.ucdm.grib.grib1.record.Grib1Gds;
import dev.ucdm.grib.grib1.record.Grib1Record;
import dev.ucdm.grib.protoconvert.Grib1Index;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Formatter;

import static com.google.common.truth.Truth.assertThat;

/**
 * Test misc GRIB1 unpacking
 */
public class TestGrib1Unpack {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  // Tests reading data with Ecmwf extended complex packing
  @Test
  public void testEcmwfExtendedComplexData() throws IOException {
    final String testfile = "../grib/src/test/data/complex_packing.grib1";
    try (CdmFile nc = CdmFiles.open(testfile)) {
      Variable var = nc.findVariable("Minimum_temperature_at_2_metres_in_the_last_6_hours_surface_6_Hour");
      Array<Float> data = (Array<Float>) var.readArray();
      float first = data.get(0, 0, 0, 0);

      assertThat(first).isWithin(1e-6f).of(264.135559f);
    }
  }

  // Tests reading data with Ecmwf extended complex packing
  @Test
  public void testEcmwfExtendedComplexData2() throws IOException {
    final String testfile = "../grib/src/test/data/complex_packing2.grib1";
    try (CdmFile nc = CdmFiles.open(testfile)) {
      Variable var = nc.findVariable("Snowfall_surface");
      Array<Float> data = (Array<Float>) var.readArray();
      float first = data.get(0, 0, 0, 0);
      assertThat(first).isWithin(1e-6f).of(.326607f);
    }
  }

  // Tests reading a thin grid record thats at the the end of the file
  // sample file has single record
  @Test
  public void testThinGridAtEndofFile() throws IOException {
    final String testfile = "../grib/src/test/data/thinGrid.grib1";
    try (CdmFile nc = CdmFiles.open(testfile)) {

      Variable var = nc.findVariable("Temperature_isobaric");
      Array<Float> data = (Array<Float>) var.readArray();
      assertThat(data.getSize()).isEqualTo(73 * 73);

      float first = data.get(0, 0, 0, 0);
      float last = data.get(0, 0, 72, 71);
      assertThat(first).isWithin(1e-6f).of(291.0f);
      assertThat(last).isWithin(1e-6f).of(278.0f);
    }
  }

  // https://github.com/Unidata/thredds/issues/82
  // not sure what it should be
  // raw: line 22:
  // 96057.882813,96302.679688,96524.906250,96693.546875,96893.937500,97179.359375,97464.890625,97647.703125,97722.148438,97733.218750,97721.312500,97769.500000,98008.898438,98483.460938,99064.515625,99570.500000,99935.101563,100199.570313,100381.640625,100439.078125,100336.703125,100103.062500,99817.539063,99564.757813,99389.359375,99292.281250,99253.007813,99231.726563,99176.851563,99073.953125,98956.421875,98831.359375,98658.148438,98440.015625,98264.398438,
  // linear:
  // cubic:
  @Test
  public void testThisGridInterpolation() throws IOException, InvalidRangeException {
    final String testfile = "../grib/src/test/data/HPPI89_KWBC.grib1";

    MFile mfile = MFileOS.getExistingFile(testfile);
    Grib1Index index = GribIndex.readOrCreateIndex1(mfile, CollectionUpdateType.test, new Formatter());

    int lineno = 0;
    try (RandomAccessFile raf = new RandomAccessFile(testfile, "r")) {
      raf.order(RandomAccessFile.BIG_ENDIAN);

      for (Grib1Record gr : index.getRecords()) {
        getData(raf, gr, GribData.InterpolationMethod.none, lineno);
        getData(raf, gr, GribData.InterpolationMethod.linear, lineno);
        getData(raf, gr, GribData.InterpolationMethod.cubic, lineno);
      }
    }
  }

  private float[] getData(RandomAccessFile raf, Grib1Record gr, GribData.InterpolationMethod method,
      int lineno) throws IOException, InvalidRangeException {
    float[] data;
    data = gr.readData(raf, method);

    System.out.printf("%s%n", method);
    Grib1Gds gds = gr.getGDS();
    if (method == GribData.InterpolationMethod.none) {
      int[] lines = gds.getNptsInLine();
      int count = 0;
      for (int line = 0; line < lines.length; line++) {
        if (line != lineno)
          continue;
        System.out.printf(" %3d: ", line);
        for (int i = 0; i < lines[line]; i++)
          System.out.printf("%f,", data[count++]);
        System.out.printf("%n");
      }

    } else {
      int[] shape = new int[] {gds.getNy(), gds.getNx()};
      Array dataA = Arrays.factory(ArrayType.FLOAT, shape, data);
      Array lineA = Arrays.slice(dataA, 0, lineno);
      logger.debug("{}", PrintArray.printArray(lineA));
    }
    System.out.printf("%s%n", method);

    return data;
  }


  @Test
  public void testStuff() {
    float add_offset = 143.988f;
    float scale_factor = 0.000614654f;
    float fd = 339.029f;

    System.out.printf("res = %f%n", scale_factor / 2);

    int packed_data = Math.round((fd - add_offset) / scale_factor); // nint((unpacked_data_value - add_offset) /
                                                                    // scale_factor)
    float unpacked_data = packed_data * scale_factor + add_offset;
    float diff = Math.abs(fd - unpacked_data);
    System.out.printf("***   org=%f, packed_data=%d unpacked=%f diff = %f%n", fd, packed_data, unpacked_data, diff);

    packed_data++;
    unpacked_data = packed_data * scale_factor + add_offset;
    diff = Math.abs(fd - unpacked_data);
    System.out.printf("***   org=%f, packed_data+1=%d unpacked=%f diff = %f%n", fd, packed_data, unpacked_data, diff);

    packed_data -= 2;
    unpacked_data = packed_data * scale_factor + add_offset;
    diff = Math.abs(fd - unpacked_data);
    System.out.printf("***   org=%f, packed_data-1=%d unpacked=%f diff = %f%n", fd, packed_data, unpacked_data, diff);
  }
}
