/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.ucdm.dataset.ncml;

import org.junit.jupiter.api.Test;
import dev.ucdm.array.ArrayType;
import dev.ucdm.core.api.CdmFile;
import dev.ucdm.core.api.Variable;
import dev.ucdm.dataset.api.CdmDatasets;

import java.io.IOException;

import static com.google.common.truth.Truth.assertThat;
import static dev.ucdm.test.util.TestFilesKt.datasetLocalDir;

/** Test NcmlNew enhancement */
public class TestEnhance {
  private static String dataDir = datasetLocalDir + "ncml/enhance/";

  @Test
  public void testStandaloneNoEnhance() throws IOException {
    try (CdmFile ncfile = CdmDatasets.openFile(dataDir + "testStandaloneNoEnhance.ncml", null)) {
      Variable unvar = ncfile.findVariable("unvar");
      assertThat((Object) unvar).isNotNull();
      assertThat(unvar.getArrayType()).isEqualTo(ArrayType.SHORT);
      assertThat(unvar.attributes().hasAttribute("_Unsigned")).isTrue();
      assertThat(unvar.attributes().findAttributeString("_Unsigned", "")).isEqualTo("true");
      assertThat(unvar.readArray().getScalar()).isEqualTo(-9981);

      Variable scaledvar = ncfile.findVariable("scaledvar");
      assertThat((Object) scaledvar).isNotNull();
      assertThat(scaledvar.getArrayType()).isEqualTo(ArrayType.SHORT);
      assertThat(scaledvar.attributes().hasAttribute("scale_factor")).isTrue();
      assertThat(scaledvar.attributes().findAttributeDouble("scale_factor", 1.0)).isEqualTo(2.0);
      assertThat(scaledvar.readArray().getScalar()).isEqualTo(1);
    }
  }

  @Test
  public void testStandaloneNoEnhanceDataset() throws IOException {
    try (CdmFile ncfile = CdmDatasets.openDataset(dataDir + "testStandaloneNoEnhance.ncml", false, null)) {
      Variable unvar = ncfile.findVariable("unvar");
      assertThat((Object) unvar).isNotNull();
      assertThat(unvar.getArrayType()).isEqualTo(ArrayType.SHORT);
      assertThat(unvar.attributes().hasAttribute("_Unsigned")).isTrue();
      assertThat(unvar.attributes().findAttributeString("_Unsigned", "")).isEqualTo("true");
      assertThat(unvar.readArray().getScalar()).isEqualTo(-9981);

      Variable scaledvar = ncfile.findVariable("scaledvar");
      assertThat((Object) scaledvar).isNotNull();
      assertThat(scaledvar.getArrayType()).isEqualTo(ArrayType.SHORT);
      assertThat(scaledvar.attributes().hasAttribute("scale_factor")).isTrue();
      assertThat(scaledvar.attributes().findAttributeDouble("scale_factor", 1.0)).isEqualTo(2.0);
      assertThat(scaledvar.readArray().getScalar()).isEqualTo(1);
    }
  }

  @Test
  public void testStandaloneEnhance() throws IOException {
    try (CdmFile ncfile = CdmDatasets.openFile(dataDir + "testStandaloneEnhance.ncml", null)) {
      Variable unvar = ncfile.findVariable("unvar");
      assertThat((Object) unvar).isNotNull();
      assertThat(unvar.getArrayType()).isEqualTo(ArrayType.UINT);
      assertThat(unvar.attributes().hasAttribute("_Unsigned")).isTrue();
      assertThat(unvar.attributes().findAttributeString("_Unsigned", "")).isEqualTo("true");
      assertThat(unvar.readArray().getScalar()).isEqualTo(55555);

      Variable scaledvar = ncfile.findVariable("scaledvar");
      assertThat((Object) scaledvar).isNotNull();
      assertThat(scaledvar.getArrayType()).isEqualTo(ArrayType.FLOAT);
      assertThat(scaledvar.attributes().hasAttribute("scale_factor")).isTrue();
      assertThat(scaledvar.attributes().findAttributeDouble("scale_factor", 1.0)).isEqualTo(2.0);
      assertThat(scaledvar.readArray().getScalar()).isEqualTo(12.0f);
    }
  }

  @Test
  public void testStandaloneEnhanceDataset() throws IOException {
    try (CdmFile ncfile = CdmDatasets.openDataset(dataDir + "testStandaloneNoEnhance.ncml", true, null)) {
      Variable unvar = ncfile.findVariable("unvar");
      assertThat((Object) unvar).isNotNull();
      assertThat(unvar.getArrayType()).isEqualTo(ArrayType.UINT);
      assertThat(unvar.attributes().hasAttribute("_Unsigned")).isTrue();
      assertThat(unvar.attributes().findAttributeString("_Unsigned", "")).isEqualTo("true");
      assertThat(unvar.readArray().getScalar()).isEqualTo(55555);

      Variable scaledvar = ncfile.findVariable("scaledvar");
      assertThat((Object) scaledvar).isNotNull();
      assertThat(scaledvar.getArrayType()).isEqualTo(ArrayType.FLOAT);
      assertThat(scaledvar.attributes().hasAttribute("scale_factor")).isTrue();
      assertThat(scaledvar.attributes().findAttributeDouble("scale_factor", 1.0)).isEqualTo(2.0);
      assertThat(scaledvar.readArray().getScalar()).isEqualTo(12.0f);
    }
  }

  @Test
  public void testStandaloneDoubleEnhanceDataset() throws IOException {
    try (CdmFile ncfile = CdmDatasets.openDataset(dataDir + "testStandaloneEnhance.ncml", true, null)) {
      Variable unvar = ncfile.findVariable("unvar");
      assertThat((Object) unvar).isNotNull();
      assertThat(unvar.getArrayType()).isEqualTo(ArrayType.UINT);
      assertThat(unvar.attributes().hasAttribute("_Unsigned")).isTrue();
      assertThat(unvar.attributes().findAttributeString("_Unsigned", "")).isEqualTo("true");
      assertThat(unvar.readArray().getScalar()).isEqualTo(55555);

      Variable scaledvar = ncfile.findVariable("scaledvar");
      assertThat((Object) scaledvar).isNotNull();
      assertThat(scaledvar.getArrayType()).isEqualTo(ArrayType.FLOAT);
      assertThat(scaledvar.attributes().hasAttribute("scale_factor")).isTrue();
      assertThat(scaledvar.attributes().findAttributeDouble("scale_factor", 1.0)).isEqualTo(2.0);
      assertThat(scaledvar.readArray().getScalar()).isEqualTo(12.0f);
    }
  }

}
