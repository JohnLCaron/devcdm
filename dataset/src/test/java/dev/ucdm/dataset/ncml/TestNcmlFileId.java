package dev.ucdm.dataset.ncml;

import dev.ucdm.dataset.api.CdmDataset;
import dev.ucdm.dataset.api.CdmDatasets;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static com.google.common.truth.Truth.assertThat;
import static dev.ucdm.test.util.TestFilesKt.coreLocalNetcdf3Dir;
import static dev.ucdm.test.util.TestFilesKt.datasetLocalNcmlDir;

/** Test getCdmFileTypeId */
public class TestNcmlFileId {
  @Test
  public void testNcmlFileId() throws IOException {
    String filename = datasetLocalNcmlDir + "modifyAtts.ncml";
    try (CdmDataset ds = CdmDatasets.openDataset(filename)) {
      assertThat(ds.getCdmFileTypeId()).isEqualTo("NcML/NetCDF");
    }
  }

  @Test
  public void testN3FileId() throws IOException {
    String filename = coreLocalNetcdf3Dir + "example1.nc";
    try (CdmDataset ds = CdmDatasets.openDataset(filename)) {
      assertThat(ds.getCdmFileTypeId()).isEqualTo("NetCDF");
      assertThat(ds.getCdmFileTypeDescription()).isEqualTo("NetCDF-3/CDM");
    }
  }

}
