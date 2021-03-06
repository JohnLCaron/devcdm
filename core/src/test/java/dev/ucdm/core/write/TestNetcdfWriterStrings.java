/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.ucdm.core.write;

import com.google.common.base.Charsets;
import dev.ucdm.array.PrintArray;
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

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.Formatter;

import static com.google.common.truth.Truth.assertThat;

/** Test {@link Netcdf3FormatWriter} using non ascii identifiers */
public class TestNetcdfWriterStrings {

  static int[] helloGreekCode =
      new int[] {0xce, 0x9a, 0xce, 0xb1, 0xce, 0xbb, 0xce, 0xb7, 0xce, 0xbc, 0xe1, 0xbd, 0xb3, 0xcf, 0x81, 0xce, 0xb1};
  static int helloGreekLen = 20;
  static int ngreeks = 3;
  static String geeks = "geeks";

  @TempDir
  public static File tempFolder;

  @Test
  public void writeNetCDFchar() throws Exception {
    String helloGreek = makeString(helloGreekCode, true);
    System.out.printf("writeNetCDFchar= %s%n", showBoth(helloGreek));
    String helloGreek2 = Normalizer.normalize(helloGreek, Normalizer.Form.NFC);
    System.out.printf(" normalized= %s%n", showBoth(helloGreek2));

    String filename = File.createTempFile("writeNetCDFchar", ".tmp", tempFolder).getAbsolutePath();

    Netcdf3FormatWriter.Builder<?> writerb = Netcdf3FormatWriter.createNewNetcdf3(filename);
    writerb.addDimension(new Dimension(helloGreek, helloGreekLen));
    writerb.addVariable(helloGreek, ArrayType.CHAR, helloGreek).addAttribute(new Attribute("units", helloGreek));

    try (Netcdf3FormatWriter writer = writerb.build()) {
      Variable v = writer.findVariable(helloGreek);
      byte[] helloBytes = helloGreek.getBytes(StandardCharsets.UTF_8);
      Array<Byte> data = Arrays.factory(ArrayType.CHAR, new int[] {helloBytes.length}, helloBytes);
      writer.write(v, data.getIndex(), data);
    }

    try (CdmFile ncout = CdmFiles.open(filename)) {
      Variable vr = ncout.findVariable(helloGreek);
      assertThat(vr).isNotNull();
      assertThat(vr.getShortName()).isEqualTo(helloGreek);

      Array<?> vrdata = vr.readArray();
      assertThat(vrdata.getArrayType()).isEqualTo(ArrayType.CHAR); // writing to netcdf3 turns it into a char
      assertThat(vrdata.getShape()).isEqualTo(new int[] {helloGreekLen});
      System.out.printf(" writeNetCDFchar printArray = %s%n", PrintArray.printArray(vrdata));
      System.out.printf(" writeNetCDFchar showBytes  = %s%n", showBytes((Array<Byte>) vrdata));

      Array<String> sdata = Arrays.makeStringsFromChar((Array<Byte>) vrdata);
      String strData = sdata.getScalar();
      System.out.printf(" writeNetCDFchar read = %s%n", showBoth(strData));
      assertThat(strData).isEqualTo(helloGreek);

      Attribute att = vr.findAttribute("units");
      assertThat(att).isNotNull();
      assertThat(att.isString()).isTrue();
      assertThat(att.getStringValue()).isEqualTo(helloGreek);
    }
  }

  @Test
  public void writeNetCDFcharArray() throws Exception {
    String helloGreek = makeString(helloGreekCode, true);
    // helloGreek = Normalizer.normalize(helloGreek, Normalizer.Form.NFC);
    System.out.printf("writeNetCDFcharArray=%s%n", showBoth(helloGreek));

    String filename = File.createTempFile("writeNetCDFcharArray", ".tmp", tempFolder).getAbsolutePath();
    Netcdf3FormatWriter.Builder<?> writerb = Netcdf3FormatWriter.createNewNetcdf3(filename);
    writerb.addDimension(new Dimension(geeks, ngreeks));
    writerb.addDimension(new Dimension(helloGreek, helloGreekLen));
    writerb.addVariable(helloGreek, ArrayType.CHAR, geeks + " " + helloGreek)
        .addAttribute(new Attribute("units", helloGreek));

    try (Netcdf3FormatWriter writer = writerb.build()) {
      Variable v = writer.findVariable(helloGreek);
      byte[] helloBytes = helloGreek.getBytes(StandardCharsets.UTF_8);
      Array<Byte> data = Arrays.factory(ArrayType.CHAR, new int[] {1, helloBytes.length}, helloBytes);
      Index index = data.getIndex();
      for (int i = 0; i < ngreeks; i++) {
        writer.write(v, index.set0(i), data);
      }
    }

    try (CdmFile ncout = CdmFiles.open(filename)) {
      Variable vr = ncout.findVariable(helloGreek);
      assertThat(vr).isNotNull();
      assertThat(vr.getShortName()).isEqualTo(helloGreek);

      Array<?> vrdata = vr.readArray();
      assertThat(vrdata.getArrayType()).isEqualTo(ArrayType.CHAR); // writing to netcdf3 turns it into a char
      assertThat(vrdata.getShape()).isEqualTo(new int[] {ngreeks, helloGreekLen});
      Array<String> sdata = Arrays.makeStringsFromChar((Array<Byte>) vrdata);
      for (int i = 0; i < ngreeks; i++) {
        String strData = sdata.get(i);
        System.out.printf(" writeNetCDFcharArray read = %s%n", showBoth(strData));
      }
      for (int i = 0; i < ngreeks; i++) {
        String strData = sdata.get(i);
        assertThat(strData).isEqualTo(helloGreek);
      }
    }
  }

  @Test
  public void writeNetCDFstring() throws Exception {
    String helloGreek = makeString(helloGreekCode, true);
    helloGreek = Normalizer.normalize(helloGreek, Normalizer.Form.NFC);
    System.out.printf("writeNetCDFstring=%s%n", showBoth(helloGreek));

    String filename = File.createTempFile("writeNetCDFstring", ".tmp", tempFolder).getAbsolutePath();
    Netcdf3FormatWriter.Builder<?> writerb = Netcdf3FormatWriter.createNewNetcdf3(filename);
    writerb.addDimension(new Dimension("nstr", 1));
    writerb.addDimension(new Dimension(helloGreek, helloGreekLen));
    writerb.addVariable(helloGreek, ArrayType.CHAR, "nstr " + helloGreek)
        .addAttribute(new Attribute("units", helloGreek));

    try (Netcdf3FormatWriter writer = writerb.build()) {
      Variable v = writer.findVariable(helloGreek);
      assertThat(v).isNotNull();
      Array<String> data = Arrays.factory(ArrayType.STRING, new int[] {1}, new String[] {helloGreek});
      writer.writeStringData(v, Index.ofRank(2), data);
    }

    try (CdmFile ncout = CdmFiles.open(filename)) {
      Variable vr = ncout.findVariable(helloGreek);
      assertThat(vr).isNotNull();
      assertThat(vr.getShortName()).isEqualTo(helloGreek);

      Array<?> vrdata = vr.readArray();
      assertThat(vrdata.getArrayType()).isEqualTo(ArrayType.CHAR); // writing to netcdf3 turns it into a char
      assertThat(vrdata.getShape()).isEqualTo(new int[] {1, helloGreekLen});
      Array<String> sdata = Arrays.makeStringsFromChar((Array<Byte>) vrdata);
      String strData = sdata.getScalar();
      System.out.printf(" writeNetCDFstring read = %s%n", showBoth(strData));
      assertThat(strData).isEqualTo(helloGreek);
    }
  }

  @Test
  public void testWriteStringData() throws Exception {
    String helloGreek = makeString(helloGreekCode, false);
    helloGreek = Normalizer.normalize(helloGreek, Normalizer.Form.NFC);
    System.out.printf("testWriteStringData=%s%n", showBoth(helloGreek));

    String filename = File.createTempFile("testWriteStringData", ".tmp", tempFolder).getAbsolutePath();
    Netcdf3FormatWriter.Builder<?> writerb = Netcdf3FormatWriter.createNewNetcdf3(filename);
    writerb.addDimension(new Dimension(geeks, ngreeks));
    writerb.addDimension(new Dimension(helloGreek, helloGreekLen));
    writerb.addVariable(helloGreek, ArrayType.CHAR, geeks + " " + helloGreek)
        .addAttribute(new Attribute("units", helloGreek));

    try (Netcdf3FormatWriter writer = writerb.build()) {
      Variable v = writer.findVariable(helloGreek);
      assertThat(v).isNotNull();
      Index index = Index.ofRank(v.getRank());
      for (int i = 0; i < ngreeks; i++) {
        writer.writeStringData(v, index.set0(i), helloGreek);
      }
    }

    try (CdmFile ncout = CdmFiles.open(filename)) {
      Variable vr = ncout.findVariable(helloGreek);
      assertThat(vr).isNotNull();
      assertThat(vr.getShortName()).isEqualTo(helloGreek);

      Array<?> vrdata = vr.readArray();
      assertThat(vrdata.getArrayType()).isEqualTo(ArrayType.CHAR); // writing to netcdf3 turns it into a char
      assertThat(vrdata.getShape()).isEqualTo(new int[] {ngreeks, helloGreekLen});
      Array<String> sdata = Arrays.makeStringsFromChar((Array<Byte>) vrdata);
      for (int i = 0; i < ngreeks; i++) {
        String strData = sdata.get(i);
        System.out.printf(" testWriteStringData read = %s%n", showBoth(strData));
        assertThat(strData).isEqualTo(helloGreek);
      }
    }
  }

  ///////////////////////////////////////////
  private String makeString(int[] codes, boolean debug) {
    byte[] b = new byte[codes.length];
    for (int i = 0; i < codes.length; i++)
      b[i] = (byte) codes[i];
    if (debug)
      System.out.println(" orgBytes= " + showBytes(b));
    String s = new String(b, StandardCharsets.UTF_8);
    if (debug)
      System.out.println("convBytes= " + showString(s));
    return s;
  }

  private String showBytes(byte[] buff) {
    StringBuilder sbuff = new StringBuilder();
    for (int i = 0; i < buff.length; i++) {
      byte b = buff[i];
      int ub = (b < 0) ? b + 256 : b;
      if (i > 0)
        sbuff.append(" ");
      sbuff.append(Integer.toHexString(ub));
    }
    return sbuff.toString();
  }

  private String showBytes(Array<Byte> buff) {
    StringBuilder sbuff = new StringBuilder();
    for (byte b : buff) {
      int ub = (b < 0) ? b + 256 : b;
      sbuff.append(" ");
      sbuff.append(Integer.toHexString(ub));
    }
    return sbuff.toString();
  }

  private String showString(String s) {
    StringBuilder sbuff = new StringBuilder();
    for (int i = 0; i < s.length(); i++) {
      int c = s.charAt(i);
      if (i > 0)
        sbuff.append(" ");
      sbuff.append(Integer.toHexString(c));
    }
    return sbuff.toString();
  }

  private String showBoth(String s) {
    Formatter sbuff = new Formatter();
    sbuff.format(" %s == %s", s, showBytes(s.getBytes(Charsets.UTF_8)));
    return sbuff.toString();
  }

}
