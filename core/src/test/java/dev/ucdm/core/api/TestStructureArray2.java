/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.ucdm.core.api;

import org.junit.jupiter.api.Test;
import dev.ucdm.array.Array;
import dev.ucdm.array.ArrayType;
import dev.ucdm.array.StructureDataArray;

import static com.google.common.truth.Truth.assertThat;

/** Test reading record data */

public class TestStructureArray2 {
  private CompareStructureArray test = new CompareStructureArray();

  @Test
  public void testBB() throws Exception {
    // testWriteRecord is 1 dimensional (nc2 record dimension)
    try (CdmFile ncfile = CdmFiles.open(TestCdmFiles.coreLocalNetcdf3Dir + "testWriteRecord.nc", -1, null,
        CdmFile.IOSP_MESSAGE_ADD_RECORD_STRUCTURE)) {

      Structure v = (Structure) ncfile.findVariable("record");
      assertThat(v).isNotNull();

      assertThat(v.getArrayType()).isEqualTo(ArrayType.STRUCTURE);

      Array<?> data = v.readArray();
      assertThat(data.getArrayType()).isEqualTo(ArrayType.STRUCTURE);

      test.testArrayStructure((StructureDataArray) data);
    }
  }

}
