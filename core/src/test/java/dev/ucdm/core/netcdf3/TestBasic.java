package dev.ucdm.core.netcdf3;

import dev.ucdm.core.api.CdmFile;
import dev.ucdm.core.api.CdmFiles;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static com.google.common.truth.Truth.assertThat;

public class TestBasic {

  @Test
  public void testBasic() throws Exception {
    basic("src/test/data/netcdf3/WMI_Lear-2003-05-28-212817.nc");
  }

  public void basic(String filename) throws IOException {
    assertThat(CdmFiles.canOpen(filename)).isTrue();
    try (CdmFile cdmFile = CdmFiles.open(filename)) {
      assertThat(cdmFile.getCdmFileTypeId()).isEqualTo("NetCDF");
      assertThat(cdmFile.getCdmFileTypeDescription()).isEqualTo("NetCDF-3/CDM");
      assertThat(cdmFile.getCdmFileTypeVersion()).isEqualTo("1");
      assertThat(cdmFile.getDetailInfo()).contains("iosp= class dev.cdm.core.netcdf3.N3iosp");
      assertThat(cdmFile.getDetailInfo()).contains("raf length= 30408");
    }
  }
}
