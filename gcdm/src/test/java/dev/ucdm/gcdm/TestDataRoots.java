package dev.ucdm.gcdm;


import dev.ucdm.gcdm.server.DataRoots;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static dev.ucdm.test.util.TestFilesKt.coreLocalNetcdf3Dir;
import static dev.ucdm.test.util.TestFilesKt.extraTestDir;

public class TestDataRoots {
  DataRoots roots = new DataRoots();

  @Test
  public void testDataRoots() {
    String localFilename = extraTestDir + "formats/hdf4/MOD021KM.A2004328.1735.004.2004329164007.hdf";
    String dataRoot = roots.findDataRoot(localFilename);
    assertThat(dataRoot).isEqualTo("extraTestDir/");

    String reqPath = "extraTestDir/formats/hdf4/MOD021KM.A2004328.1735.004.2004329164007.hdf";
    String dataPath = roots.findDataPath(reqPath);
    assertThat(dataPath).isEqualTo(extraTestDir);

    String urlName = roots.convertPathToRoot(localFilename);
    assertThat(urlName).isEqualTo("extraTestDir/formats/hdf4/MOD021KM.A2004328.1735.004.2004329164007.hdf");

    String roundtrip = roots.convertRootToPath(urlName);
    assertThat(roundtrip).isEqualTo(localFilename);
  }

  @Test
  public void testDataRootsLongestPath() {
    String localFilename = coreLocalNetcdf3Dir + "pathPart";
    String dataRoot = roots.findDataRoot(localFilename);
    assertThat(dataRoot).isEqualTo("coreLocalNetcdf3Dir/");

    String reqPath = "coreLocalNetcdf3Dir/pathPart";
    String dataPath = roots.findDataPath(reqPath);
    assertThat(dataPath).isEqualTo("/home/snake/dev/github/devcdm/core/src/test/data/netcdf3/");

    String urlName = roots.convertPathToRoot(localFilename);
    assertThat(urlName).isEqualTo("coreLocalNetcdf3Dir/pathPart");

    String roundtrip = roots.convertRootToPath(urlName);
    assertThat(roundtrip).isEqualTo("/home/snake/dev/github/devcdm/core/src/test/data/netcdf3/pathPart");
  }


}
