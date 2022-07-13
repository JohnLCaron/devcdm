package dev.ucdm.core.hdf4;

import dev.ucdm.core.api.CdmFile;
import dev.ucdm.core.api.CdmFiles;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static com.google.common.truth.Truth.assertThat;

public class TestBasic {

  @Test
  public void testBasic() throws Exception {
    basic("src/test/data/hdf4/MAC07S0.A2008230.1250.002.2008233222357.hdf");
  }

  public void basic(String filename) throws IOException {
    assertThat(CdmFiles.canOpen(filename)).isTrue();
    try (CdmFile cdmFile = CdmFiles.open(filename)) {
      assertThat(cdmFile.getCdmFileTypeId()).isEqualTo("HDF4");
      assertThat(cdmFile.getCdmFileTypeDescription()).isEqualTo("Hierarchical Data Format, version 4");
      assertThat(cdmFile.getCdmFileTypeVersion()).isEqualTo("4.2.1 (NCSA HDF Version 4.2 Release 1, February 17, 2005)");
      assertThat(cdmFile.getDetailInfo()).contains("iosp= class dev.ucdm.core.hdf4.H4iosp");
    }
  }
}
