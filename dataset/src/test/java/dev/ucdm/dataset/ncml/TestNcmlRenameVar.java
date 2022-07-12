/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.ucdm.dataset.ncml;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import dev.ucdm.array.Array;
import dev.ucdm.array.ArrayType;
import dev.ucdm.array.Arrays;
import dev.ucdm.array.InvalidRangeException;
import dev.ucdm.array.Section;
import dev.ucdm.core.api.Attribute;
import dev.ucdm.core.api.Dimension;
import dev.ucdm.core.api.CdmFile;
import dev.ucdm.core.api.Variable;

import java.io.IOException;
import java.util.Iterator;

import static com.google.common.truth.Truth.assertThat;
import static dev.ucdm.array.NumericCompare.nearlyEquals;

public class TestNcmlRenameVar {

  static CdmFile ncfile = null;
  static String filename = "file:./" + TestNcmlRead.topDir + "renameVar.xml";

  @BeforeAll
  public static void setUp() throws IOException {
    ncfile = NcmlReader.readNcml(filename, null, null).build();
    assertThat(ncfile).isNotNull();
  }

  @AfterAll
  public static void tearDown() throws IOException {
    if (ncfile != null) ncfile.close();
  }

  @Test
  public void testStructure() {
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
    assert timeDim.getLength() == 4;
    assert timeDim.isUnlimited();
  }

  @Test
  public void testReadCoordvar() {
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
      Array data = lat.readArray();
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

  @Test
  public void testReadData() {
    Variable v = ncfile.findVariable("ReletiveHumidity");
    assert null != v;
    assert v.getShortName().equals("ReletiveHumidity");
    assert v.getRank() == 3;
    assert v.getSize() == 48;
    assert v.getShape()[0] == 4;
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

    try {
      Array data = v.readArray();
      assert data.getRank() == 3;
      assert data.getSize() == 48;
      assert data.getShape()[0] == 4;
      assert data.getShape()[1] == 3;
      assert data.getShape()[2] == 4;
      Iterator<Integer> dataI = data.iterator();

      assert dataI.next() == 1;
      assert dataI.next() == 2;
      assert dataI.next() == 3;
      assert dataI.next() == 4;
      assert dataI.next() == 5;
    } catch (IOException io) {
    }
  }

  @Test
  public void testReadSlice() {
    Variable v = ncfile.findVariable("ReletiveHumidity");
    int[] origin = new int[3];
    int[] shape = {2, 3, 1};

    try {
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
    } catch (InvalidRangeException io) {
      assert false;
    } catch (IOException io) {
      io.printStackTrace();
      assert false;
    }
  }

  @Test
  public void testReadSlice2() {
    Variable v = ncfile.findVariable("ReletiveHumidity");
    int[] origin = new int[3];
    int[] shape = {2, 1, 3};

    try {
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
    } catch (InvalidRangeException io) {
      assert false;
    } catch (IOException io) {
      io.printStackTrace();
      assert false;
    }
  }

}
