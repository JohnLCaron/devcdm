package cdm.dataset.coordsysbuild

import dev.cdm.test.util.TestFiles
import org.junit.jupiter.api.Test

class TestCoordsysProblems {

    @Test
    fun testProblem() {
        compareCoordinateSystems(TestFiles.coreLocalDir + "/netcdf4/testCFGridWriter.nc4")
    }

    @Test
    fun testHasStructure() {
        compareCoordinateSystems("../core/src/test/data/netcdf4/cdm_sea_soundings.nc4")
    }

    @Test
    fun testHdfEos() {
        compareCoordinateSystems("../core/src/test/data/hdfeos2/MCD43B2.A2007001.h00v08.005.2007043191624.hdf")
    }
}