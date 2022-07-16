/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.ucdm.core.write;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import dev.ucdm.array.Array;
import dev.ucdm.array.ArrayType;
import dev.ucdm.array.Arrays;
import dev.ucdm.array.Index;
import dev.ucdm.core.api.Attribute;
import dev.ucdm.core.api.Dimension;
import dev.ucdm.core.api.CdmFile;
import dev.ucdm.core.api.CdmFiles;
import dev.ucdm.core.api.Variable;
import dev.ucdm.core.netcdf3.NetcdfFormatUtils;

import java.io.File;

import static com.google.common.truth.Truth.assertThat;

/**
 * Test {@link Netcdf3FormatWriter} with fill values
 */
public class TestNetcdf3FormatWriterFill {

  @TempDir
  public static File tempFolder;

  @Test
  public void testCreateWithFill() throws Exception {
    String filename = File.createTempFile("testCreateWithFill", ".tmp", tempFolder).getAbsolutePath();

    Netcdf3FormatWriter.Builder<?> writerb = Netcdf3FormatWriter.createNewNetcdf3(filename);
    writerb.addDimension(Dimension.builder().setName("time").setIsUnlimited(true).build());
    Dimension latDim = writerb.addDimension("lat", 6);
    Dimension lonDim = writerb.addDimension("lon", 12);

    // define Variables
    writerb.addVariable("temperature", ArrayType.DOUBLE, "lat lon").addAttribute(new Attribute("units", "K"))
        .addAttribute(new Attribute("_FillValue", -999.9));

    writerb.addVariable("lat", ArrayType.DOUBLE, "lat");
    writerb.addVariable("lon", ArrayType.FLOAT, "lon");
    writerb.addVariable("shorty", ArrayType.SHORT, "lat");

    writerb.addVariable("rtemperature", ArrayType.INT, "time lat lon").addAttribute(new Attribute("units", "K"))
        .addAttribute(new Attribute("_FillValue", -9999));

    writerb.addVariable("rdefault", ArrayType.INT, "time lat lon");

    // add string-valued variables
    writerb.addVariable("svar", ArrayType.CHAR, "lat lon");
    writerb.addVariable("svar2", ArrayType.CHAR, "lat lon");

    // string array
    writerb.addDimension("names", 3);
    writerb.addDimension("svar_len", 80);
    writerb.addVariable("names", ArrayType.CHAR, "names svar_len");
    writerb.addVariable("names2", ArrayType.CHAR, "names svar_len");

    int[] shape1 = new int[] {1, latDim.getLength(), lonDim.getLength()};
    int n = lonDim.getLength();
    int[] parray = new int[(int) Arrays.computeSize(shape1)];
    // write to half of it
    for (int i = 0; i < latDim.getLength(); i++) {
      for (int j = 0; j < n / 2; j++) {
        parray[i * n + j] = (i * 1000000 + j * 1000);
      }
    }
    Array<Double> A = Arrays.factory(ArrayType.INT, shape1, parray);

    try (Netcdf3FormatWriter writer = writerb.build()) {
      Variable v = writer.findVariable("rtemperature");
      assertThat(v).isNotNull();
      writer.write(v, A.getIndex(), A);
    }

    //////////////////////////////////////////////////////////////////////
    // test reading, checking for fill values
    try (CdmFile ncfile = CdmFiles.open(filename)) {

      Variable temp = ncfile.findVariable("temperature");
      assertThat(temp).isNotNull();
      Array<Number> tA = (Array<Number>) temp.readArray();
      assertThat(tA.getRank()).isEqualTo(2);
      for (Number val : tA) {
        assertThat(val.doubleValue()).isEqualTo(-999.9);
      }

      Variable rtemp = ncfile.findVariable("rtemperature");
      assertThat(rtemp).isNotNull();

      Array<Number> rA = (Array<Number>) rtemp.readArray();
      assertThat(rA.getRank()).isEqualTo(3);

      Index ima = rA.getIndex();
      int[] rshape = rA.getShape();
      for (int i = 0; i < rshape[1]; i++) {
        for (int j = rshape[2]; j < rshape[2]; j++) {
          if (j > rshape[2] / 2) {
            assertThat(rA.get(ima.set(0, i, j)).doubleValue()).isEqualTo(-9999.0);
          } else {
            assertThat(rA.get(ima.set(0, i, j)).doubleValue()).isEqualTo((double) (i * 1000000 + j * 1000));
          }
        }
      }

      Variable v = ncfile.findVariable("lat");
      assertThat(v).isNotNull();
      assertThat(v.getArrayType()).isEqualTo(ArrayType.DOUBLE);

      Array<Number> data = (Array<Number>) v.readArray();
      for (Number val : data) {
        assertThat(val.doubleValue()).isEqualTo(NetcdfFormatUtils.NC_FILL_DOUBLE);
      }

      v = ncfile.findVariable("lon");
      assertThat(v).isNotNull();
      data = (Array<Number>) v.readArray();
      for (Number val : data) {
        assertThat(val.floatValue()).isEqualTo(NetcdfFormatUtils.NC_FILL_FLOAT);
      }

      v = ncfile.findVariable("shorty");
      assertThat(v).isNotNull();
      data = (Array<Number>) v.readArray();
      for (Number val : data) {
        assertThat(val.shortValue()).isEqualTo(NetcdfFormatUtils.NC_FILL_SHORT);
      }

      v = ncfile.findVariable("rdefault");
      assertThat(v).isNotNull();
      data = (Array<Number>) v.readArray();
      for (Number val : data) {
        assertThat(val.intValue()).isEqualTo(NetcdfFormatUtils.NC_FILL_INT);
      }
    }
  }
}
