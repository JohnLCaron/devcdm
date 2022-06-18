/*
 * Copyright (c) 2021 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.cdm.core.hdf5;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import dev.cdm.core.api.Attribute;
import dev.cdm.core.api.CdmFile;
import dev.cdm.core.api.CdmFiles;
import dev.cdm.core.api.Variable;

import java.io.IOException;

import static com.google.common.truth.Truth.assertThat;

/** Test String Attributes */
public class TestNullStringAttr {

  private static CdmFile ncf;
  private static final String testFile = "src/test/data/netcdf4/string_attrs.nc4";

  @BeforeAll
  public static void openFile() throws IOException {
    ncf = CdmFiles.open(testFile);
    assertThat(ncf).isNotNull();
  }

  @Test
  public void TestNullStringGlobalAttr() {
    Attribute attr = ncf.findAttribute("NULL_STR_GATTR");
    String value = attr.getStringValue();
    assertThat(value).isNotNull();
    assertThat(value).isNotEmpty();
    assertThat(value).isNotEqualTo("");
    assertThat(value).isEqualTo("NIL");
  }

  @Test
  public void TestEmptyStringGlobalAttr() {
    Attribute attr = ncf.findAttribute("EMPTY_STR_GATTR");
    String value = attr.getStringValue();
    assertThat(value).isNotNull();
    assertThat(value).isEmpty();
    assertThat(value).isEqualTo("");
  }

  @Test
  public void TestNullStringAttr() {
    Variable testVar = ncf.findVariable("/var");
    Attribute attr = testVar.findAttribute("NULL_STR_ATTR");
    String value = attr.getStringValue();
    assertThat(value).isNotNull();
    assertThat(value).isNotEmpty();
    assertThat(value).isNotEqualTo("");
    assertThat(value).isEqualTo("NIL");
  }

  @Test
  public void TestEmptyStringAttr() {
    Variable testVar = ncf.findVariable("/var");
    Attribute attr = testVar.findAttribute("EMPTY_STR_ATTR");
    String value = attr.getStringValue();
    assertThat(value).isNotNull();
    assertThat(value).isEmpty();
    assertThat(value).isEqualTo("");
  }

  @AfterAll
  public static void closeFile() throws IOException {
    ncf.close();
  }
}
