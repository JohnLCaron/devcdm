package dev.ucdm.core.api;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static com.google.common.truth.Truth.assertThat;

public class TestCdmFilesRemote {

  // random remote file
  String testFile =
  "https://thredds.ucar.edu/thredds/fileServer/casestudies/2019blizzard/goes/GOES16/Mesoscale-2/Channel14/20190313/GOES16_Mesoscale-2_20190313_234156_11.20_2km_43.5N_96.9W.nc4";

  @Test
  public void testReadRemote() throws IOException {
    try (CdmFile ncfile = CdmFiles.open(testFile)) {
      System.out.printf("remoteFile = %s%n", ncfile);
      // global attributes
      assertThat(ncfile.getRootGroup().findAttributeString("title", null))
              .isEqualTo("Sectorized Cloud and Moisture Imagery for the EMESO region.");

      Variable temp = ncfile.findVariable("fixedgrid_projection");
      assertThat(temp).isNotNull();
      assertThat(temp.findAttributeString("grid_mapping_name", "barf")).isEqualTo("geostationary");
    }
  }

  // TODO we need a compressed one, ask Unidata to host
}
