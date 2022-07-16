/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.ucdm.core.write;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import dev.ucdm.array.Array;
import dev.ucdm.array.ArrayType;
import dev.ucdm.array.Arrays;
import dev.ucdm.array.CompareArrayToArray;
import dev.ucdm.array.Index;
import dev.ucdm.core.api.Attribute;
import dev.ucdm.core.api.Dimension;
import dev.ucdm.core.api.CdmFile;
import dev.ucdm.core.api.CdmFiles;
import dev.ucdm.core.api.Variable;
import dev.ucdm.core.constants.CDM;

import java.io.File;

import static com.google.common.truth.Truth.assertThat;

/** Test {@link Netcdf3FormatWriter} */
public class TestNetcdf3FormatWriter {
  @TempDir
  public static File tempFolder;

  /*
   * byte Band1(y, x);
   * > Band1:_Unsigned = "true";
   * > Band1:_FillValue = -1b; // byte
   * >
   * > byte Band2(y, x);
   * > Band2:_Unsigned = "true";
   * > Band2:valid_range = 0s, 254s; // short
   */
  @Test
  public void testUnsignedAttribute() throws Exception {
    String filename = File.createTempFile("testUnsignedAttribute", ".tmp", tempFolder).getAbsolutePath();

    Netcdf3FormatWriter.Builder<?> writerb = Netcdf3FormatWriter.createNewNetcdf3(filename);
    writerb.addUnlimitedDimension("time");

    writerb.addVariable("time", ArrayType.BYTE, "time").addAttribute(new Attribute(CDM.UNSIGNED, "true"))
        .addAttribute(new Attribute(CDM.SCALE_FACTOR, 10.0))
        .addAttribute(Attribute.builder(CDM.VALID_RANGE).setValues(ImmutableList.of(10, 240), false).build());

    /*
     * byte Band1(y, x);
     * > Band1:_Unsigned = "true";
     * > Band1:_FillValue = -1b; // byte
     */
    writerb.addVariable("Band1", ArrayType.BYTE, "time").addAttribute(new Attribute(CDM.UNSIGNED, "true"))
        .addAttribute(new Attribute(CDM.FILL_VALUE, (byte) -1)).addAttribute(new Attribute(CDM.SCALE_FACTOR, 1.0));

    /*
     * byte Band2(y, x);
     * > Band2:_Unsigned = "true";
     * > Band2:valid_range = 0s, 254s; // short
     */
    writerb.addVariable("Band2", ArrayType.BYTE, "time").addAttribute(new Attribute(CDM.UNSIGNED, "true"))
        .addAttribute(new Attribute(CDM.SCALE_FACTOR, 1.0)).addAttribute(
            Attribute.builder(CDM.VALID_RANGE).setValues(ImmutableList.of((short) 0, (short) 254), false).build());

    try (Netcdf3FormatWriter writer = writerb.build()) {
      Index index = Index.ofRank(1);
      byte[] data = new byte[1];
      for (int time = 0; time < 256; time++) {
        data[0] = (byte) time;
        writer.config().forVariable("time").withOrigin(index).withPrimitiveArray(data).withShape(1).write();
        writer.config().forVariable("Band1").withOrigin(index).withPrimitiveArray(data).withShape(1).write();
        writer.config().forVariable("Band2").withOrigin(index).withPrimitiveArray(data).withShape(1).write();
        index.incr(0);
      }
    }

    Array<Byte> expected = Arrays.makeArray(ArrayType.BYTE, 256, 0, 1);
    try (CdmFile ncFile = CdmFiles.open(filename)) {
      Array<?> time = ncFile.readSectionArray("time");
      assertThat(CompareArrayToArray.compareData("time", time, expected)).isTrue();
      Array<?> Band1 = ncFile.readSectionArray("Band1");
      assertThat(CompareArrayToArray.compareData("time", Band1, expected)).isTrue();
      Array<?> Band2 = ncFile.readSectionArray("Band2");
      assertThat(CompareArrayToArray.compareData("time", Band2, expected)).isTrue();
    }
  }

  @Test
  public void testWriteUnlimited() throws Exception {
    String filename = File.createTempFile("testWriteUnlimited", ".tmp", tempFolder).getAbsolutePath();

    Netcdf3FormatWriter.Builder<?> writerb = Netcdf3FormatWriter.createNewNetcdf3(filename);
    writerb.addUnlimitedDimension("time");
    writerb.addAttribute(new Attribute("name", "value"));

    // public Variable addVariable(Group g, String shortName, ArrayType dataType, String dims) {
    writerb.addVariable("time", ArrayType.DOUBLE, "time");

    // write
    try (Netcdf3FormatWriter writer = writerb.build()) {
      Array<?> data = Arrays.makeArray(ArrayType.DOUBLE, 4, 0, 1);
      Variable time = writer.findVariable("time"); // ?? immutable ??
      assertThat(time).isNotNull();
      writer.config().forVariable(time).withArray(data).write();
    }

    // read it back
    try (CdmFile ncfile = CdmFiles.open(filename)) {
      Variable vv = ncfile.findVariable("time");
      assertThat(vv).isNotNull();
      assertThat(vv.getSize()).isEqualTo(4);

      Array<?> expected = Arrays.makeArray(ArrayType.DOUBLE, 4, 0, 1);
      Array<?> data = vv.readArray();
      assertThat(CompareArrayToArray.compareData("time", data, expected)).isTrue();
    }
  }

  @Test
  public void testWriteRecordOneAtaTime() throws Exception {
    String filename = File.createTempFile("testWriteRecordOneAtaTime", ".tmp", tempFolder).getAbsolutePath();

    Netcdf3FormatWriter.Builder<?> writerb = Netcdf3FormatWriter.createNewNetcdf3(filename);
    // define dimensions, including unlimited
    Dimension latDim = writerb.addDimension("lat", 3);
    Dimension lonDim = writerb.addDimension("lon", 4);
    writerb.addDimension(Dimension.builder().setName("time").setIsUnlimited(true).build());

    // define Variables
    writerb.addVariable("lat", ArrayType.FLOAT, "lat").addAttribute(new Attribute("units", "degrees_north"));
    writerb.addVariable("lon", ArrayType.FLOAT, "lon").addAttribute(new Attribute("units", "degrees_east"));
    writerb.addVariable("rh", ArrayType.INT, "time lat lon")
        .addAttribute(new Attribute("long_name", "relative humidity")).addAttribute(new Attribute("units", "percent"));
    writerb.addVariable("T", ArrayType.DOUBLE, "time lat lon")
        .addAttribute(new Attribute("long_name", "surface temperature")).addAttribute(new Attribute("units", "degC"));
    writerb.addVariable("time", ArrayType.INT, "time").addAttribute(new Attribute("units", "hours since 1990-01-01"));

    try (Netcdf3FormatWriter writer = writerb.build()) {
      // write out the non-record variables
      writer.config().forVariable("lat").withPrimitiveArray(new float[] {41, 40, 39}).write();
      writer.config().forVariable("lon").withPrimitiveArray(new float[] {-109, -107, -105, -103}).write();

      Variable timeVar = writer.findVariable("time");
      Preconditions.checkNotNull(timeVar);

      Index timeOrigin = Index.ofRank(1);
      Index recordOrigin = Index.ofRank(3);

      // write 10 records
      int[] timeValue = new int[1];
      for (int timeIdx = 0; timeIdx < 10; timeIdx++) {
        timeValue[0] = timeIdx;

        // 12 values in one record
        Array<?> rhData = Arrays.makeArray(ArrayType.INT, 12, 0, 1 + timeIdx, 1, 3, 4);
        Array<?> tData = Arrays.makeArray(ArrayType.DOUBLE, 12, 99 * timeIdx, (1 + timeIdx) / 3.14159, 1, 3, 4);

        // write the data
        writer.config().forVariable("T").withOrigin(recordOrigin).withArray(tData).write();
        writer.config().forVariable("rh").withOrigin(recordOrigin).withArray(rhData).write();
        writer.config().forVariable(timeVar).withOrigin(timeOrigin).withPrimitiveArray(timeValue).withShape(1).write();
        timeOrigin.incr(0);
        recordOrigin.incr(0);
      } // loop over record
    }

    // read it back
    try (CdmFile ncfile = CdmFiles.open(filename)) {
      Variable time = ncfile.getRootGroup().findVariableLocal("time");
      assertThat(time).isNotNull();
      assertThat(time.getSize()).isEqualTo(10);

      Array<?> expected = Arrays.makeArray(ArrayType.INT, 12, 0, 1);
      Array<?> data = time.readArray();
      assertThat(CompareArrayToArray.compareData("time", data, expected)).isTrue();
    }
  }

  // fix for bug introduced 2/9/10, reported by Christian Ward-Garrison cwardgar@usgs.gov
  @Test
  public void testRecordSizeBug() throws Exception {
    String filename = File.createTempFile("testRecordSizeBug", ".tmp", tempFolder).getAbsolutePath();
    int size = 10;

    Netcdf3FormatWriter.Builder<?> writerb = Netcdf3FormatWriter.createNewNetcdf3(filename).setFill(false);
    writerb.addUnlimitedDimension("time");
    writerb.addVariable("time", ArrayType.INT, "time").addAttribute(new Attribute("units", "hours since 1990-01-01"));

    try (Netcdf3FormatWriter writer = writerb.build()) {
      Index timeOrigin = Index.ofRank(1);

      for (int time = 0; time < size; time++) {
        Array<?> timeData = Arrays.factory(ArrayType.INT, new int[] {1}, new int[] {time * 12});
        writer.config().forVariable("time").withOrigin(timeOrigin).withArray(timeData).write();
        timeOrigin.incr(0);
      }
    }

    try (CdmFile ncFile = CdmFiles.open(filename)) {
      Array<?> result = ncFile.readSectionArray("time");
      assertThat(result.show()).isEqualTo("0, 12, 24, 36, 48, 60, 72, 84, 96, 108");
    }
  }

  @Test
  public void testStringWriting() throws Exception {
    String filename = File.createTempFile("testStringWriting", ".tmp", tempFolder).getAbsolutePath();
    int strlen = 25;

    Netcdf3FormatWriter.Builder<?> writerb = Netcdf3FormatWriter.createNewNetcdf3(filename).setFill(false);
    writerb.addDimension("len", strlen);
    writerb.addUnlimitedDimension("time");
    writerb.addVariable("time", ArrayType.CHAR, "time len");

    try (Netcdf3FormatWriter writer = writerb.build()) {
      Variable time = writer.findVariable("time");
      assertThat(time).isNotNull();

      // Note that origin is incremented.
      Index index = Index.ofRank(time.getRank());
      writer.config().forVariable(time).withOrigin(index).withString("This is the first string.").write();
      writer.config().forVariable(time).withOrigin(index.incr(0)).withString("Shorty").write();
      writer.config().forVariable(time).withOrigin(index.incr(0))
          .withString("This is too long so it will get truncated").write();
    }

    try (CdmFile ncfile = CdmFiles.open(filename)) {
      Variable time = ncfile.findVariable("time");
      assertThat(time).isNotNull();
      Array<?> timeData = time.readArray();

      assertThat(timeData.getArrayType()).isEqualTo(ArrayType.CHAR);
      Array<String> achar3Data = Arrays.makeStringsFromChar((Array<Byte>) timeData);

      assertThat(achar3Data.get(0)).isEqualTo("This is the first string.");
      assertThat(achar3Data.get(1)).isEqualTo("Shorty");
      assertThat(achar3Data.get(2)).isEqualTo("This is too long so it wi");
      assertThat(achar3Data.get(2)).hasLength(strlen);
    }
  }
}
