/*
 * Copyright (c) 2020 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.cdm.core.hdf5;

import static com.google.common.truth.Truth.assertThat;

import java.io.IOException;
import org.junit.jupiter.api.Test;
import dev.cdm.array.Array;
import dev.cdm.array.ArrayType;
import dev.cdm.core.api.CdmFile;
import dev.cdm.core.api.CdmFiles;
import dev.cdm.core.api.Variable;

public class TestEosStructMetadata {

  @Test
  public void testStructMetadataEosString() throws IOException {
    // Test file source from https://github.com/Unidata/netcdf-java/issues/455
    // but reduced in size by creating a new file with only the StructureMetadata.0 dataset using h5copy:
    // h5copy -v -i VNP10A1_A2018001_h31v11_001_2019126193423_HEGOUT.nc -o structmetadata_eos.h5 -p
    // -s "/HDFEOS INFORMATION/StructMetadata.0" -d "/HDFEOS INFORMATION/StructMetadata.0"
    // Copying file <VNP10A1_A2018001_h31v11_001_2019126193423_HEGOUT.nc> and object </HDFEOS
    // INFORMATION/StructMetadata.0>
    // to file <structmetadata_eos.h5> and object </HDFEOS INFORMATION/StructMetadata.0>
    // h5copy: Creating parent groups
    String testFile = "src/test/data/hdfeos5/structmetadata_eos.h5";
    try (CdmFile ncf = CdmFiles.open(testFile)) {
      Variable testVar = ncf.findVariable("HDFEOS_INFORMATION/StructMetadata\\.0");
      Array<?> data = testVar.readArray();

      assertThat(data.getArrayType()).isEqualTo(ArrayType.STRING);
      assertThat(data.getSize()).isEqualTo(1);

      String strData = ((Array<String>) data).getScalar();
      assertThat(strData).hasLength(1664);
      assertThat(strData).startsWith("GROUP=SwathStructure\nEND_GROUP=SwathStructure\nGROUP=GridStructure");
      assertThat(strData).endsWith("END_GROUP=ZaStructure\nEND\n");
    }
  }
}
