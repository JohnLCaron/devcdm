/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package dev.cdm.dataset.api;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static com.google.common.truth.Truth.assertThat;

/** Test {@link CdmDataset} */
public class TestCdmDataset {
  @Test
  public void testNcmlFileId() throws IOException {
    String filename = TestCdmDatasets.datasetLocalNcmlDir + "modifyAtts.ncml";
    try (CdmDataset ds = CdmDatasets.openDataset(filename)) {
      assertThat(ds.getCdmFileTypeId()).isEqualTo("NcML/NetCDF");
    }
  }

  @Test
  public void testN3FileId() throws IOException {
    String filename = TestCdmDatasets.coreLocalDir + "example1.nc";
    try (CdmDataset ds = CdmDatasets.openDataset(filename)) {
      assertThat(ds.getCdmFileTypeId()).isEqualTo("NetCDF");
      assertThat(ds.getCdmFileTypeDescription()).isEqualTo("NetCDF-3/CDM");
    }
  }

}
