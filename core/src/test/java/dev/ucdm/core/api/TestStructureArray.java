/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.ucdm.core.api;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import dev.ucdm.array.Array;
import dev.ucdm.array.ArrayType;
import dev.ucdm.array.Section;

import static com.google.common.truth.Truth.assertThat;

/** Test reading record data */
public class TestStructureArray {

  private static CdmFile ncfile;

  @BeforeAll
  public static void setUp() throws Exception {
    // testStructures is 1 dimensional (nc2 record dimension)
    ncfile = CdmFiles.open(TestCdmFiles.coreLocalNetcdf3Dir + "testStructures.nc", -1, null,
        CdmFile.IOSP_MESSAGE_ADD_RECORD_STRUCTURE);
  }

  @AfterAll
  public static void tearDown() throws Exception {
    ncfile.close();
  }

  @Test
  public void testNames() {
    for (Variable v : ncfile.getVariables()) {
      System.out.println(" " + v.getShortName() + " =" + v.getFullName());
    }

    Structure record = (Structure) ncfile.findVariable("record");
    assertThat(record).isNotNull();

    for (Variable v : record.getVariables()) {
      assertThat("record." + v.getShortName()).isEqualTo(v.getFullName());
    }
  }

  @Test
  public void testReadTop() throws Exception {
    Variable v = ncfile.findVariable("record");
    assertThat(v).isNotNull();

    assertThat(v.getArrayType()).isEqualTo(ArrayType.STRUCTURE);
    assertThat(v instanceof Structure);
    assertThat(v.getRank()).isEqualTo(1);
    assertThat(v.getSize()).isEqualTo(1000);

    Array<?> data = v.readArray(new Section(new int[] {4}, new int[] {3}));
    assertThat(data.getArrayType()).isEqualTo(ArrayType.STRUCTURE);
    assertThat(data.getSize()).isEqualTo(3);
    assertThat(data.getRank()).isEqualTo(1);
  }
}
