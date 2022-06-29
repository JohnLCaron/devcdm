package dev.cdm.dataset.ncml;

import dev.cdm.dataset.api.CdmDataset;
import dev.cdm.dataset.api.CdmDatasets;
import dev.cdm.dataset.api.TestCdmDatasets;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static com.google.common.truth.Truth.assertThat;

/** Test getCdmFileTypeId */
public class TestNcmlFileId {
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
