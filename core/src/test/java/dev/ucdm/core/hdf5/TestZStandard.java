/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.ucdm.core.hdf5;

import dev.ucdm.array.Array;
import dev.ucdm.array.ArrayType;
import dev.ucdm.array.Index;
import dev.ucdm.core.api.CdmFile;
import dev.ucdm.core.api.CdmFiles;
import dev.ucdm.core.api.Variable;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

/**
 * Test zstandard compression in netcdf 4 files.
 */
public class TestZStandard {

  // @Test
  public void testZStandard() throws Exception {
    try (CdmFile ncfile = CdmFiles.open("src/test/data/netcdf4/tst_zstandard_zstandard.nc")) {
      Variable compressedVariable = ncfile.findVariable("Wacky_Woolies");
      assertThat((Object) compressedVariable).isNotNull();
      assertThat(compressedVariable.getArrayType()).isEqualTo(ArrayType.FLOAT);
      assertThat(compressedVariable.getShape()).isEqualTo(new int[] {100, 100});
      Array<Float> data = (Array<Float>) compressedVariable.readArray();
      Index ima = data.getIndex();
      for (int y = 0; y < 100; y++) {
        for (int x = 0; x < 100; x++) {
          assertWithMessage(String.format("y= %d x=%d", y, x)).that(data.get(ima.set(y, x)))
              .isEqualTo((float) (10000 * y + 101 * x));
        }
      }
    }
  }
}
