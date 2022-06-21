/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.cdm.dataset.ncml;

import dev.cdm.dataset.api.TestCdmDatasets;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import dev.cdm.array.Array;
import dev.cdm.array.ArrayType;
import dev.cdm.array.Arrays;
import dev.cdm.array.Section;
import dev.cdm.core.api.Attribute;
import dev.cdm.core.api.Dimension;
import dev.cdm.core.api.CdmFile;
import dev.cdm.core.api.Variable;

import java.io.IOException;
import java.util.Iterator;
import java.util.stream.Stream;

import static dev.cdm.array.NumericCompare.nearlyEquals;

public class TestNcmlRead {
  public static String topDir = TestCdmDatasets.datasetLocalNcmlDir;

  public static Stream<Arguments> params() {
    return Stream.of(
            Arguments.of("testRead.xml"),
            Arguments.of("readMetadata.xml"),
            Arguments.of("testReadHttps.xml"));
  }

  private String ncmlLocation;
  private CdmFile ncfile;

  @ParameterizedTest
  @MethodSource("params")
  public void testNcmlRead(String filename) throws Exception {
    this.ncmlLocation = "file:" + topDir + filename;
    try (CdmFile cdm = NcmlReader.readNcml(ncmlLocation, null, null).build()) {
      ncfile = cdm;
      testStructure();
      testReadCoordvar();
      testReadData();
      testReadSlice();
      testReadSlice2();
      testReadDataAlias();
    }
  }

  void testStructure() {
    System.out.println("ncfile opened = " + ncmlLocation + "\n" + ncfile);

    Attribute att = ncfile.findAttribute("title");
    assert null != att;
    assert !att.isArray();
    assert att.isString();
    assert att.getArrayType() == ArrayType.STRING;
    assert att.getStringValue().equals("Example Data");
    assert att.getNumericValue() == null;
    assert att.getNumericValue(3) == null;

    att = ncfile.findAttribute("testFloat");
    assert null != att;
    assert att.isArray();
    assert !att.isString();
    assert att.getArrayType() == ArrayType.FLOAT;
    assert att.getStringValue() == null;
    assert att.getNumericValue().equals(1.0f);
    assert att.getNumericValue(3).equals(4.0f);

    Dimension latDim = ncfile.findDimension("lat");
    assert null != latDim;
    assert latDim.getShortName().equals("lat");
    assert latDim.getLength() == 3;
    assert !latDim.isUnlimited();

    Dimension timeDim = ncfile.findDimension("time");
    assert null != timeDim;
    assert timeDim.getShortName().equals("time");
    assert timeDim.getLength() == 2;
    assert timeDim.isUnlimited();
  }

  void testReadCoordvar() {
    Variable lat = ncfile.findVariable("lat");
    assert null != lat;
    assert lat.getShortName().equals("lat");
    assert lat.getRank() == 1;
    assert lat.getSize() == 3;
    assert lat.getShape()[0] == 3;
    assert lat.getArrayType() == ArrayType.FLOAT;

    assert !lat.isUnlimited();

    assert lat.getDimension(0) == ncfile.findDimension("lat");

    Attribute att = lat.findAttribute("units");
    assert null != att;
    assert !att.isArray();
    assert att.isString();
    assert att.getArrayType() == ArrayType.STRING;
    assert att.getStringValue().equals("degrees_north");
    assert att.getNumericValue() == null;
    assert att.getNumericValue(3) == null;

    try {
      Array<Float> data = (Array<Float>) lat.readArray();
      assert data.getRank() == 1;
      assert data.getSize() == 3;
      assert data.getShape()[0] == 3;
      Iterator<Float> dataI = data.iterator();

      assert nearlyEquals(dataI.next(), 41.0);
      assert nearlyEquals(dataI.next(), 40.0);
      assert nearlyEquals(dataI.next(), 39.0);
    } catch (IOException io) {
    }

  }

  void testReadData() throws Exception {
    Variable v = ncfile.findVariable("rh");
    assert null != v;
    assert v.getShortName().equals("rh");
    assert v.getRank() == 3;
    assert v.getSize() == 24;
    assert v.getShape()[0] == 2;
    assert v.getShape()[1] == 3;
    assert v.getShape()[2] == 4;
    assert v.getArrayType() == ArrayType.INT;

    assert !v.isCoordinateVariable();
    assert v.isUnlimited();

    assert v.getDimension(0) == ncfile.findDimension("time");
    assert v.getDimension(1) == ncfile.findDimension("lat");
    assert v.getDimension(2) == ncfile.findDimension("lon");

    Attribute att = v.findAttribute("units");
    assert null != att;
    assert !att.isArray();
    assert att.isString();
    assert att.getArrayType() == ArrayType.STRING;
    assert att.getStringValue().equals("percent");
    assert att.getNumericValue() == null;
    assert att.getNumericValue(3) == null;

    Array data = v.readArray();
    assert data.getRank() == 3;
    assert data.getSize() == 24;
    assert data.getShape()[0] == 2;
    assert data.getShape()[1] == 3;
    assert data.getShape()[2] == 4;
    Iterator<Integer> dataI = data.iterator();

    assert dataI.next() == 1;
    assert dataI.next() == 2;
    assert dataI.next() == 3;
    assert dataI.next() == 4;
    assert dataI.next() == 5;
  }

  void testReadSlice() throws Exception {
    Variable v = ncfile.findVariable("rh");
    int[] origin = new int[3];
    int[] shape = {2, 3, 1};

    Array data = v.readArray(new Section(origin, shape));
    assert data.getRank() == 3;
    assert data.getSize() == 6;
    assert data.getShape()[0] == 2;
    assert data.getShape()[1] == 3;
    assert data.getShape()[2] == 1;
    Iterator<Integer> dataI = data.iterator();

    assert dataI.next() == 1;
    assert dataI.next() == 5;
    assert dataI.next() == 9;
    assert dataI.next() == 21;
    assert dataI.next() == 25;
    assert dataI.next() == 29;
  }

  void testReadSlice2() throws Exception {
    Variable v = ncfile.findVariable("rh");
    int[] origin = new int[3];
    int[] shape = {2, 1, 3};

    Array data = Arrays.reduce(v.readArray(new Section(origin, shape)));
    assert data.getRank() == 2;
    assert data.getSize() == 6;
    assert data.getShape()[0] == 2;
    assert data.getShape()[1] == 3;
    Iterator<Integer> dataI = data.iterator();

    assert dataI.next() == 1;
    assert dataI.next() == 2;
    assert dataI.next() == 3;
    assert dataI.next() == 21;
    assert dataI.next() == 22;
    assert dataI.next() == 23;

  }

  void testReadDataAlias() throws Exception {
    Variable v = ncfile.findVariable("T");
    assert null != v;
    assert v.getShortName().equals("T");
    assert v.getRank() == 3;
    assert v.getSize() == 24;
    assert v.getShape()[0] == 2;
    assert v.getShape()[1] == 3;
    assert v.getShape()[2] == 4;
    assert v.getArrayType() == ArrayType.DOUBLE;

    assert !v.isCoordinateVariable();
    assert v.isUnlimited();

    assert v.getDimension(0) == ncfile.findDimension("time");
    assert v.getDimension(1) == ncfile.findDimension("lat");
    assert v.getDimension(2) == ncfile.findDimension("lon");

    Attribute att = v.findAttribute("units");
    assert null != att;
    assert !att.isArray();
    assert att.isString();
    assert att.getArrayType() == ArrayType.STRING : att.getArrayType();
    assert att.getStringValue().equals("degC");
    assert att.getNumericValue() == null;
    assert att.getNumericValue(3) == null;

    Array data = v.readArray();
    assert data.getRank() == 3;
    assert data.getSize() == 24;
    assert data.getShape()[0] == 2;
    assert data.getShape()[1] == 3;
    assert data.getShape()[2] == 4;
    Iterator<Double> dataI = data.iterator();

    assert nearlyEquals(dataI.next(), 1.0);
    assert nearlyEquals(dataI.next(), 2.0);
    assert nearlyEquals(dataI.next(), 3.0);
    assert nearlyEquals(dataI.next(), 4.0);
    assert nearlyEquals(dataI.next(), 2.0);
  }

}
