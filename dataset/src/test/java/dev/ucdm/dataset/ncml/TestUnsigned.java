/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.ucdm.dataset.ncml;

import dev.ucdm.core.api.Attribute;
import dev.ucdm.core.api.CdmFile;
import dev.ucdm.core.api.Variable;
import dev.ucdm.dataset.api.CdmDataset;
import dev.ucdm.dataset.api.CdmDatasets;
import org.junit.jupiter.api.Test;
import dev.ucdm.array.Array;
import dev.ucdm.array.ArrayType;

import java.io.IOException;
import java.io.StringReader;

import static com.google.common.truth.Truth.assertThat;
import static dev.ucdm.test.util.TestFilesKt.coreLocalNetcdf3Dir;
import static dev.ucdm.test.util.TestFilesKt.datasetLocalNcmlDir;

/** Test Unsigned values and attributes in NcML works correctly */
public class TestUnsigned {

  @Test
  public void testSigned() throws IOException {
    try (CdmFile ncfile = CdmDatasets.openDataset(coreLocalNetcdf3Dir + "testWrite.nc")) {
      Variable v = ncfile.findVariable("bvar");
      assertThat(v).isNotNull();
      assertThat(v.getArrayType().isUnsigned()).isFalse();
      assertThat(v.getArrayType()).isEqualTo(ArrayType.BYTE);

      boolean hasSigned = false;
      Array<?> data = v.readArray();
      assertThat(data.getArrayType()).isEqualTo(ArrayType.BYTE);
      for (byte b : (Array<Byte>) data) {
        if (b < 0) {
          hasSigned = true;
        }
      }
      assertThat(hasSigned).isTrue();
    }
  }

  @Test
  public void testUnsigned() throws IOException {
    try (CdmFile ncfile = CdmDatasets.openDataset(datasetLocalNcmlDir + "testUnsignedByte.ncml")) {
      Variable v = ncfile.findVariable("bvar");
      assertThat(v).isNotNull();
      assertThat(v.getArrayType().isUnsigned()).isFalse();
      assertThat(v.getArrayType()).isEqualTo(ArrayType.FLOAT); // has float scale_factor

      boolean hasSigned = false;
      Array<?> data = v.readArray();
      assertThat(data.getArrayType()).isEqualTo(ArrayType.FLOAT);
      for (float b : (Array<Float>) data) {
        if (b < 0) {
          hasSigned = true;
        }
      }
      assertThat(hasSigned).isFalse();
    }
  }

  @Test
  public void testUnsignedWrap() throws IOException {
    String ncml = "<?xml version='1.0' encoding='UTF-8'?>\n"
        + "<netcdf xmlns='http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2' enhance='All' location='"
        + coreLocalNetcdf3Dir + "testWrite.nc'>\n" //
        + "  <variable name='bvar' shape='lat' type='byte'>\n"//
        + "    <attribute name='_Unsigned' value='true' />\n" //
        + "    <attribute name='scale_factor' type='float' value='2.0' />\n" //
        + "   </variable>\n" //
        + "</netcdf>";

    try (CdmDataset ncd = CdmDatasets.openNcmlDataset(new StringReader(ncml), null, null)) {
      Variable v = ncd.findVariable("bvar");
      assertThat(v).isNotNull();
      assertThat(v.getArrayType().isUnsigned()).isFalse();
      assertThat(v.getArrayType()).isEqualTo(ArrayType.FLOAT); // has float scale_factor

      boolean hasSigned = false;
      Array<?> data = v.readArray();
      assertThat(data.getArrayType()).isEqualTo(ArrayType.FLOAT);
      for (float b : (Array<Float>) data) {
        if (b < 0) {
          hasSigned = true;
        }
      }
      assertThat(hasSigned).isFalse();
    }
  }

  @Test
  public void testVarWithUnsignedAttribute() throws IOException {
    String ncml = "<?xml version='1.0' encoding='UTF-8'?>\n"
        + "<netcdf xmlns='http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2' enhance='All' location='"
        + coreLocalNetcdf3Dir + "testWrite.nc'>\n" //
        + "  <variable name='bvar' shape='lat' type='byte'>\n" //
        + "    <attribute name='_Unsigned' value='true' />\n" //
        + "   </variable>\n" //
        + "</netcdf>";

    try (CdmDataset ncd = CdmDatasets.openNcmlDataset(new StringReader(ncml), null, null)) {
      Variable v = ncd.findVariable("bvar");
      assertThat(v).isNotNull();
      assertThat(v.getArrayType().isUnsigned()).isTrue();
      assertThat(v.getArrayType()).isEqualTo(ArrayType.USHORT);

      boolean hasSigned = false;
      Array<?> data = v.readArray();
      assertThat(data.getArrayType()).isEqualTo(ArrayType.USHORT);
      for (short b : (Array<Short>) data) {
        if (b < 0) {
          hasSigned = true;
        }
      }
      assertThat(hasSigned).isFalse();
    }
  }

  @Test
  // arbitrarily changes the datatype to ubyte, which doesnt affect the actual data
  public void testVarWithUnsignedTypeNotEnhanced() throws IOException {
    String ncml = "<?xml version='1.0' encoding='UTF-8'?>\n"
        + "<netcdf xmlns='http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2' location='"
        + coreLocalNetcdf3Dir + "testWrite.nc'>\n" //
        + "  <variable name='bvar' shape='lat' type='ubyte'/>" //
        + "</netcdf>";

    try (CdmDataset ncd = CdmDatasets.openNcmlDataset(new StringReader(ncml), null, null)) {
      Variable v = ncd.findVariable("bvar");
      assertThat(v).isNotNull();
      assertThat(ArrayType.UBYTE).isEqualTo(v.getArrayType());

      boolean hasSigned = false;
      Array<Byte> data = (Array<Byte>) v.readArray();
      assertThat(ArrayType.BYTE).isEqualTo(data.getArrayType());

      // but theres a tricky thing that when one gets as float, it seems to know its unsigned??
      for (byte b : data) {
        if (b < 0) {
          hasSigned = true;
        }
      }
      assertThat(hasSigned).isTrue();
    }
  }

  @Test
  // arbitrarily changes the datatype to ubyte, which doesnt affect the actual data
  public void testArrayWithUnsignedTypeNotEnhanced() throws IOException {
    String ncml = "<?xml version='1.0' encoding='UTF-8'?>\n"
        + "<netcdf xmlns='http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2' location='"
        + coreLocalNetcdf3Dir + "testWrite.nc'>\n" //
        + "  <variable name='bvar' shape='lat' type='ubyte'/>" //
        + "</netcdf>";

    try (CdmDataset ncd = CdmDatasets.openNcmlDataset(new StringReader(ncml), null, null)) {
      Variable v = ncd.findVariable("bvar");
      assertThat(v).isNotNull();
      assertThat(v.getArrayType().isUnsigned()).isTrue();
      assertThat(v.getArrayType()).isEqualTo(ArrayType.UBYTE);

      boolean hasSigned = false;
      Array<?> data = v.readArray();
      assertThat(data.getArrayType()).isEqualTo(ArrayType.BYTE);
      for (byte b : (Array<Byte>) data) {
        if (b < 0) {
          hasSigned = true;
        }
      }
      assertThat(hasSigned).isTrue();

      hasSigned = false;
      Array<Number> ndata = (Array<Number>) data;
      for (Number val : ndata) {
        float b = val.floatValue();
        if (b < 0) {
          hasSigned = true;
        }
      }
      assertThat(hasSigned).isTrue();
    }
  }

  @Test
  public void testVarWithUnsignedTypeEnhanced() throws IOException {
    String ncml = "<?xml version='1.0' encoding='UTF-8'?>\n"
        + "<netcdf xmlns='http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2' enhance='true' location='"
        + coreLocalNetcdf3Dir + "testWrite.nc'>\n" //
        + "  <variable name='bvar' shape='lat' type='ubyte'/>" //
        + "</netcdf>";

    try (CdmDataset ncd = CdmDatasets.openNcmlDataset(new StringReader(ncml), null, null)) {
      Variable v = ncd.findVariable("bvar");
      assertThat(v).isNotNull();
      assertThat(v.getArrayType().isUnsigned()).isTrue();
      assertThat(v.getArrayType()).isEqualTo(ArrayType.USHORT);

      boolean hasSigned = false;
      Array<?> data = v.readArray();
      assertThat(data.getArrayType()).isEqualTo(ArrayType.USHORT);
      for (short b : (Array<Short>) data) {
        if (b < 0) {
          hasSigned = true;
        }
      }
      assertThat(hasSigned).isFalse();
    }
  }

  @Test
  public void testAttWithUnsignedType() throws IOException {
    String ncml = "<?xml version='1.0' encoding='UTF-8'?>\n"
        + "<netcdf xmlns='http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2' enhance='All' location='"
        + coreLocalNetcdf3Dir + "testWrite.nc'>\n" //
        + "  <attribute name='gatt' type='ubyte'>1 0 -1</attribute>" //
        + "</netcdf>";

    try (CdmDataset ncd = CdmDatasets.openNcmlDataset(new StringReader(ncml), null, null)) {
      Attribute att = ncd.findAttribute("gatt");
      assertThat(att).isNotNull();
      assertThat(att.getArrayType().isUnsigned()).isTrue();
      assertThat(att.getArrayType()).isEqualTo(ArrayType.UBYTE);
      assertThat(att.getLength()).isEqualTo(3);

      Array<?> gattValues = att.getArrayValues();

      boolean hasSigned = false;
      assertThat(gattValues.getArrayType()).isEqualTo(ArrayType.UBYTE);
      for (byte b : (Array<Byte>) gattValues) {
        if (b < 0) {
          hasSigned = true;
        }
      }
      assertThat(hasSigned).isTrue();
    }
  }

  @Test
  public void testAttWithUnsignedAtt() throws IOException {
    String ncml = "<?xml version='1.0' encoding='UTF-8'?>\n"
        + "<netcdf xmlns='http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2' enhance='All' location='"
        + coreLocalNetcdf3Dir + "testWrite.nc'>\n"
        + "  <attribute name='gatt' type='byte' isUnsigned='true'>1 0 -1</attribute>" + "</netcdf>";

    try (CdmDataset ncd = CdmDatasets.openNcmlDataset(new StringReader(ncml), null, null)) {
      Attribute att = ncd.findAttribute("gatt");
      assertThat(att).isNotNull();
      assertThat(att.getArrayType().isUnsigned()).isTrue();
      assertThat(att.getArrayType()).isEqualTo(ArrayType.UBYTE);
      assertThat(att.getLength()).isEqualTo(3);

      Array<?> gattValues = att.getArrayValues();

      boolean hasSigned = false;
      assertThat(gattValues.getArrayType()).isEqualTo(ArrayType.UBYTE);
      for (byte b : (Array<Byte>) gattValues) {
        if (b < 0) {
          hasSigned = true;
        }
      }
      assertThat(hasSigned).isTrue();
    }
  }

  @Test
  public void testAttWithUnsignedType2() throws IOException {
    String ncml = "<?xml version='1.0' encoding='UTF-8'?>\n"
        + "<netcdf xmlns='http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2' enhance='All' location='"
        + coreLocalNetcdf3Dir + "testWrite.nc'>\n" + "  <attribute name='gatt' type='ubyte' value='1 0 -1' />"
        + "</netcdf>";

    try (CdmDataset ncd = CdmDatasets.openNcmlDataset(new StringReader(ncml), null, null)) {
      Attribute att = ncd.findAttribute("gatt");
      assertThat(att).isNotNull();
      assertThat(att.getArrayType().isUnsigned()).isTrue();
      assertThat(att.getArrayType()).isEqualTo(ArrayType.UBYTE);
      assertThat(att.getLength()).isEqualTo(3);

      Array<?> gattValues = att.getArrayValues();

      boolean hasSigned = false;
      assertThat(gattValues.getArrayType()).isEqualTo(ArrayType.UBYTE);
      for (byte b : (Array<Byte>) gattValues) {
        if (b < 0) {
          hasSigned = true;
        }
      }
      assertThat(hasSigned).isTrue();
    }
  }
}
