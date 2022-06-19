package dev.cdm.core.hdf5;

import dev.cdm.core.api.CdmFile;
import dev.cdm.core.api.CdmFiles;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static com.google.common.truth.Truth.assertThat;

public class TestBasic {

  @Test
  public void testBasic() throws Exception {
    basic("src/test/data/hdf5/compound_complex.h5");
  }

  public void basic(String filename) throws IOException {
    assertThat(CdmFiles.canOpen(filename)).isTrue();
    try (CdmFile cdmFile = CdmFiles.open(filename)) {
      assertThat(cdmFile.getCdmFileTypeId()).isEqualTo("HDF5");
      assertThat(cdmFile.getCdmFileTypeDescription()).isEqualTo("Hierarchical Data Format 5");
      assertThat(cdmFile.getCdmFileTypeVersion()).isEqualTo("superblock version 0");
      assertThat(cdmFile.getDetailInfo()).contains("iosp= class dev.cdm.core.hdf5.H5iosp");
    }
  }
}
