/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.ucdm.core.hdf5;

import dev.ucdm.core.api.CdmFile;
import dev.ucdm.core.api.CdmFiles;

import static com.google.common.truth.Truth.assertThat;

/** Test problems in hdf5 / netcdf 4 files. */
public class TestProblems {

  // @Test
  public void problem() throws Exception {
    String filename = "src/test/data/netcdf4/attributeStruct.nc";
    System.out.printf("TestProblems %s%n", filename);
    try (CdmFile ncfile = CdmFiles.open(filename)) {
      assertThat(ncfile).isNotNull();
      System.out.printf("result = %s%n", ncfile);
    }
  }

}
