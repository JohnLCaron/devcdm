package cdm.dataset.coordsysbuild

import org.junit.jupiter.api.Test

class TestCoordsysProblems {

    @Test
    fun testProblem() {
        compareCoordinateSystems(
            "/media/snake/0B681ADF0B681ADF/thredds-test-data/local/thredds-test-data/cdmUnitTest/conventions/wrf/global.nc")
    }

    @Test
    fun testAWIP() {
        compareCoordinateSystems(
            "/media/snake/0B681ADF0B681ADF/thredds-test-data/local/thredds-test-data/cdmUnitTest/conventions/awips/awips.nc")
    }


    @Test
    fun testAWIPSat() {
        compareCoordinateSystems(
            "/media/snake/0B681ADF0B681ADF/thredds-test-data/local/thredds-test-data/cdmUnitTest/conventions/awips/20150602_0830_sport_imerg_noHemis_rr.nc")
    }

    @Test
    fun testGDV() {
        compareCoordinateSystems("/media/snake/0B681ADF0B681ADF/thredds-test-data/local/thredds-test-data/cdmUnitTest/conventions/gdv/testGDV.nc")
    }

    @Test
    fun testZebra() {
        compareCoordinateSystems("/media/snake/0B681ADF0B681ADF/thredds-test-data/local/thredds-test-data/cdmUnitTest/conventions/zebra/SPOL_3Volumes.nc")
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