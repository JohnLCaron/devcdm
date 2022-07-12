package cdm.dataset.coordsysbuild

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import dev.ucdm.array.Indent
import dev.ucdm.core.constants.AxisType
import dev.ucdm.core.constants.CDM
import dev.ucdm.core.constants.CF
import dev.ucdm.dataset.api.CdmDatasetCS
import dev.ucdm.dataset.api.CdmDatasets.openDatasetWithCS
import dev.ucdm.dataset.api.VariableDS
import dev.ucdm.dataset.cdmdsl.writeCSDsl
import dev.ucdm.dataset.cdmdsl.writeDsl
import org.junit.jupiter.api.Test

class TestConventions {

    @Test
    fun testAWIP() {
        val location =
            "/media/snake/0B681ADF0B681ADF/thredds-test-data/local/thredds-test-data/cdmUnitTest/conventions/awips/awips.nc"
        // LOOK not right
        openDatasetWithCS(location, true).use { ncd ->
            assertThat(ncd).isNotNull()
            println("${ncd.writeCSDsl()}")
            assertThat(ncd.conventionName).isEqualTo("AWIPS")
            assertThat(ncd.coordinateAxes).hasSize(12)
            assertThat(ncd.coordinateSystems).hasSize(1)

            val temp = ncd.findVariable("staticTopo") as VariableDS
            assertThat(temp).isNotNull()
            val tempCss = ncd.makeCoordinateSystemsFor(temp)
            assertThat(tempCss).isNotNull()
            assertThat(tempCss).hasSize(1)
            val tempCs = tempCss[0]

            assertThat(tempCs.name).isEqualTo("y x")
            assertThat(tempCs.axesName).isEqualTo("y x")
            assertThat(tempCs.coordinateAxes).hasSize(2)
            assertThat(tempCs.findAxis(AxisType.GeoX)).isNotNull()
            assertThat(tempCs.findAxis(AxisType.GeoY)).isNotNull()

            assertThat(tempCs.coordinateTransforms).hasSize(1)
            assertThat(tempCs.projection).isNotNull()
            assertThat(tempCs.projection!!.name).isEqualTo(CF.LAMBERT_CONFORMAL_CONIC)
            assertThat(tempCs.verticalTransform).isNull()
        }
    }

    @Test
    fun testAWIPSat() {
        val location =
            "/media/snake/0B681ADF0B681ADF/thredds-test-data/local/thredds-test-data/cdmUnitTest/conventions/awips/20150602_0830_sport_imerg_noHemis_rr.nc"
        openDatasetWithCS(location, true).use { ncd ->
            assertThat(ncd).isNotNull()
            println("${ncd.writeCSDsl()}")

            assertThat(ncd.conventionName).isEqualTo("AWIPS-Sat")
            assertThat(ncd.coordinateAxes).hasSize(2)
            assertThat(ncd.coordinateSystems).hasSize(1)

            val temp = ncd.findVariable("image") as VariableDS
            assertThat(temp).isNotNull()
            val tempCss = ncd.makeCoordinateSystemsFor(temp)
            assertThat(tempCss).isNotNull()
            assertThat(tempCss).hasSize(1)
            val tempCs = tempCss[0]

            assertThat(tempCs.name).isEqualTo("y x")
            assertThat(tempCs.axesName).isEqualTo("y x")
            assertThat(tempCs.coordinateAxes).hasSize(2)
            assertThat(tempCs.findAxis(AxisType.GeoX)).isNotNull()
            assertThat(tempCs.findAxis(AxisType.GeoY)).isNotNull()

            assertThat(tempCs.coordinateTransforms).hasSize(1)
            assertThat(tempCs.projection).isNotNull()
            assertThat(tempCs.projection!!.name).isEqualTo(CDM.EquidistantCylindrical)
            assertThat(tempCs.verticalTransform).isNull()
        }
    }

    @Test
    fun testCedricRadar() {
        val location =
            " /media/snake/0B681ADF0B681ADF/thredds-test-data/local/thredds-test-data/cdmUnitTest/conventions/cedric/fort.54"
        openDatasetWithCS(location, true).use { ncd ->
            assertThat(ncd).isNotNull()
            assertThat(ncd.conventionName).isEqualTo("CEDRICRadar")
            assertThat(ncd.coordinateAxes).hasSize(4)
            assertThat(ncd.coordinateSystems).hasSize(1)

            val temp = ncd.findVariable("VVORT") as VariableDS
            assertThat(temp).isNotNull()
            val tempCss = ncd.makeCoordinateSystemsFor(temp)
            assertThat(tempCss).isNotNull()
            assertThat(tempCss).hasSize(1)
            val tempCs = tempCss[0]

            val out = StringBuilder()
            tempCs.writeDsl(out, Indent(2))
            println("$out")

            assertThat(tempCs.name).isEqualTo("time z y x")
            assertThat(tempCs.axesName).isEqualTo("time z y x")
            assertThat(tempCs.coordinateAxes).hasSize(4)
            assertThat(tempCs.findAxis(AxisType.Time)).isNotNull()
            assertThat(tempCs.findAxis(AxisType.GeoX)).isNotNull()
            assertThat(tempCs.findAxis(AxisType.GeoY)).isNotNull()
            assertThat(tempCs.findAxis(AxisType.GeoZ)).isNotNull()

            assertThat(tempCs.coordinateTransforms).hasSize(1)
            assertThat(tempCs.projection).isNotNull()
            assertThat(tempCs.projection!!.name).isEqualTo("FlatEarth")

            assertThat(tempCs.verticalTransform).isNull()
        }
    }

    @Test
    fun testCF() {
        val location =
            "/media/snake/0B681ADF0B681ADF/thredds-test-data/local/thredds-test-data/cdmUnitTest/conventions/cf/ipcc/hfogo_O1.nc"
        openDatasetWithCS(location, true).use { ncd ->
            assertThat(ncd).isNotNull()
            assertThat(ncd.conventionName).isEqualTo("CFConventions")
            assertThat(ncd.coordinateAxes).hasSize(3)
            assertThat(ncd.coordinateSystems).hasSize(1)

            // ncd.testCss("PS", "lat y lon x")
            // ncd.testCss("Temperature", "level lat y lon x")

            val temp = ncd.findVariable("hfogo") as VariableDS
            assertThat(temp).isNotNull()
            val tempCss = ncd.makeCoordinateSystemsFor(temp)
            assertThat(tempCss).isNotNull()
            assertThat(tempCss).hasSize(1)
            val tempCs = tempCss[0]

            val out = StringBuilder()
            tempCs.writeDsl(out, Indent(2))
            println("$out")

            assertThat(tempCs.name).isEqualTo("geo_region time lat")
            assertThat(tempCs.axesName).isEqualTo("geo_region time lat")
            assertThat(tempCs.coordinateAxes).hasSize(3)
            assertThat(tempCs.findAxis(AxisType.Lat)).isNotNull()
            assertThat(tempCs.findAxis(AxisType.Time)).isNotNull()

            assertThat(tempCs.coordinateTransforms).hasSize(0)
            assertThat(tempCs.projection).isNull()
            assertThat(tempCs.verticalTransform).isNull()
        }
    }

    @Test
    fun testCFCurvilinear() {
        val location =
            "/media/snake/0B681ADF0B681ADF/thredds-test-data/local/thredds-test-data/cdmUnitTest/conventions/cf/signell/signell_bathy_fixed.nc"
        openDatasetWithCS(location, true).use { ncd ->
            assertThat(ncd).isNotNull()
            assertThat(ncd.conventionName).isEqualTo("CFConventions")
            assertThat(ncd.coordinateAxes).hasSize(2)
            assertThat(ncd.coordinateSystems).hasSize(1)

            val temp = ncd.findVariable("h") as VariableDS
            assertThat(temp).isNotNull()
            val tempCss = ncd.makeCoordinateSystemsFor(temp)
            assertThat(tempCss).isNotNull()
            assertThat(tempCss).hasSize(1)
            val tempCs = tempCss[0]

            val out = StringBuilder()
            tempCs.writeDsl(out, Indent(2))
            println("$out")

            assertThat(tempCs.name).isEqualTo("latitude longitude")
            assertThat(tempCs.axesName).isEqualTo("latitude longitude")
            assertThat(tempCs.coordinateAxes).hasSize(2)
            assertThat(tempCs.findAxis(AxisType.Lat)).isNotNull()
            assertThat(tempCs.findAxis(AxisType.Lon)).isNotNull()

            assertThat(tempCs.coordinateTransforms).hasSize(0)
            assertThat(tempCs.projection).isNull()
            assertThat(tempCs.verticalTransform).isNull()
        }
    }

    @Test
    fun testCFCurvilinear2() {
        val location = "/home/snake/tmp/testData/coords/ukmo.nc"
        openDatasetWithCS(location, true).use { ncd ->
            assertThat(ncd).isNotNull()
            println(ncd.writeDsl())
            assertThat(ncd.conventionName).isEqualTo("CFConventions")
            assertThat(ncd.coordinateAxes).hasSize(5)
            assertThat(ncd.coordinateSystems).hasSize(1)

            val temp = ncd.findVariable("temperature_2m") as VariableDS
            assertThat(temp).isNotNull()
            val tempCss = ncd.makeCoordinateSystemsFor(temp)
            assertThat(tempCss).isNotNull()
            assertThat(tempCss).hasSize(1)
            val tempCs = tempCss[0]

            val out = StringBuilder()
            tempCs.writeDsl(out, Indent(2))
            println("$out")

            assertThat(tempCs.name).isEqualTo("basetime forecast level latitude longitude")
            assertThat(tempCs.axesName).isEqualTo("basetime forecast level latitude longitude")
            assertThat(tempCs.coordinateAxes).hasSize(5)
            assertThat(tempCs.findAxis(AxisType.Lat)).isNotNull()
            assertThat(tempCs.findAxis(AxisType.Lon)).isNotNull()
            assertThat(tempCs.findAxis(AxisType.Height)).isNotNull()
            assertThat(tempCs.findAxis(AxisType.RunTime)).isNotNull()
            assertThat(tempCs.findAxis(AxisType.TimeOffset)).isNotNull()

            assertThat(tempCs.coordinateTransforms).hasSize(0)
            assertThat(tempCs.projection).isNull()
            assertThat(tempCs.verticalTransform).isNull()
        }
    }

    //

    @Test
    fun testCoards() {
        val location =
            "/media/snake/0B681ADF0B681ADF/thredds-test-data/local/thredds-test-data/cdmUnitTest/conventions/coards/air.2001.ncml"
        openDatasetWithCS(location, true).use { ncd ->
            assertThat(ncd).isNotNull()
            assertThat(ncd.conventionName).isEqualTo("DefaultConventions")
            assertThat(ncd.coordinateAxes).hasSize(4)
            assertThat(ncd.coordinateSystems).hasSize(1)

            val temp = ncd.findVariable("air") as VariableDS
            assertThat(temp).isNotNull()
            val tempCss = ncd.makeCoordinateSystemsFor(temp)
            assertThat(tempCss).isNotNull()
            assertThat(tempCss).hasSize(1)
            val tempCs = tempCss[0]

            val out = StringBuilder()
            tempCs.writeDsl(out, Indent(2))
            println("$out")

            assertThat(tempCs.name).isEqualTo("time level lat lon")
            assertThat(tempCs.axesName).isEqualTo("time level lat lon")
            assertThat(tempCs.coordinateAxes).hasSize(4)
            assertThat(tempCs.findAxis(AxisType.Time)).isNotNull()
            assertThat(tempCs.findAxis(AxisType.Lat)).isNotNull()
            assertThat(tempCs.findAxis(AxisType.Lon)).isNotNull()
            assertThat(tempCs.findAxis(AxisType.Pressure)).isNotNull()

            assertThat(tempCs.coordinateTransforms).hasSize(0)
            assertThat(tempCs.projection).isNull()
            assertThat(tempCs.verticalTransform).isNull()
        }
    }

    @Test
    fun testCSM() {
        val location =
            "/media/snake/0B681ADF0B681ADF/thredds-test-data/local/thredds-test-data/cdmUnitTest/conventions/csm/atmos.tuv.monthly.nc"
        openDatasetWithCS(location, true).use { ncd ->
            assertThat(ncd).isNotNull()
            assertThat(ncd.conventionName).isEqualTo("DefaultConventions")
            assertThat(ncd.coordinateAxes).hasSize(4)
            assertThat(ncd.coordinateSystems).hasSize(1)

            val temp = ncd.findVariable("T") as VariableDS
            assertThat(temp).isNotNull()
            val tempCss = ncd.makeCoordinateSystemsFor(temp)
            assertThat(tempCss).isNotNull()
            assertThat(tempCss).hasSize(1)
            val tempCs = tempCss[0]

            val out = StringBuilder()
            tempCs.writeDsl(out, Indent(2))
            println("$out")

            assertThat(tempCs.name).isEqualTo("time lev lat lon")
            assertThat(tempCs.axesName).isEqualTo("time lev lat lon")
            assertThat(tempCs.coordinateAxes).hasSize(4)
            assertThat(tempCs.findAxis(AxisType.Time)).isNotNull()
            assertThat(tempCs.findAxis(AxisType.Lat)).isNotNull()
            assertThat(tempCs.findAxis(AxisType.Lon)).isNotNull()
            assertThat(tempCs.findAxis(AxisType.GeoZ)).isNotNull()

            assertThat(tempCs.coordinateTransforms).hasSize(1)
            assertThat(tempCs.projection).isNull()
            assertThat(tempCs.verticalTransform).isNotNull()
            assertThat(tempCs.verticalTransform?.name).isEqualTo("hybrid_sigma_pressure")
        }
    }

    @Test
    fun testGief() {
        val location =
            "/media/snake/0B681ADF0B681ADF/thredds-test-data/local/thredds-test-data/cdmUnitTest/conventions/gief/coamps.wind_uv.nc"
        openDatasetWithCS(location, true).use { ncd ->
            assertThat(ncd).isNotNull()
            assertThat(ncd.conventionName).isEqualTo("GIEF")
            assertThat(ncd.coordinateAxes).hasSize(4)
            assertThat(ncd.coordinateSystems).hasSize(1)

            val temp = ncd.findVariable("U-Component") as VariableDS
            assertThat(temp).isNotNull()
            val tempCss = ncd.makeCoordinateSystemsFor(temp)
            assertThat(tempCss).isNotNull()
            assertThat(tempCss).hasSize(1)
            val tempCs = tempCss[0]

            assertThat(tempCs.name).isEqualTo("time level latitude longitude")
            assertThat(tempCs.axesName).isEqualTo("time level latitude longitude")
            assertThat(tempCs.coordinateAxes).hasSize(4)
            assertThat(tempCs.findAxis(AxisType.Lat)).isNotNull()
            assertThat(tempCs.findAxis(AxisType.Lon)).isNotNull()
            assertThat(tempCs.findAxis(AxisType.Height)).isNotNull()
            assertThat(tempCs.findAxis(AxisType.Time)).isNotNull()

            assertThat(tempCs.coordinateTransforms).hasSize(0)
            assertThat(tempCs.projection).isNull()
            assertThat(tempCs.verticalTransform).isNull()
        }
    }


    @Test
    fun testGDV() {
        val location =
            "/media/snake/0B681ADF0B681ADF/thredds-test-data/local/thredds-test-data/cdmUnitTest/conventions/gdv/testGDV.nc"
        openDatasetWithCS(location, true).use { ncd ->
            assertThat(ncd).isNotNull()
            assertThat(ncd.conventionName).isEqualTo("GDV")
            assertThat(ncd.coordinateAxes).hasSize(4)
            assertThat(ncd.coordinateSystems).hasSize(1)

            val temp = ncd.findVariable("D2_O3_2D") as VariableDS
            assertThat(temp).isNotNull()
            val tempCss = ncd.makeCoordinateSystemsFor(temp)
            assertThat(tempCss).isNotNull()
            assertThat(tempCss).hasSize(1)
            val tempCs = tempCss[0]

            assertThat(tempCs.name).isEqualTo("time j i")
            assertThat(tempCs.axesName).isEqualTo("time j i")
            assertThat(tempCs.coordinateAxes).hasSize(3)
            assertThat(tempCs.findAxis(AxisType.GeoX)).isNotNull()
            assertThat(tempCs.findAxis(AxisType.GeoY)).isNotNull()
            assertThat(tempCs.findAxis(AxisType.Time)).isNotNull()

            assertThat(tempCs.coordinateTransforms).hasSize(1)
            assertThat(tempCs.projection).isNotNull()
            assertThat(tempCs.projection!!.name).isEqualTo(CF.STEREOGRAPHIC)
            assertThat(tempCs.verticalTransform).isNull()
        }
    }

    @Test
    fun testHasStructure() {
        val location ="../core/src/test/data/netcdf4/cdm_sea_soundings.nc4"
        openDatasetWithCS(location, true).use { ncd ->
            assertThat(ncd).isNotNull()
            assertThat(ncd.coordinateAxes).hasSize(0)
            assertThat(ncd.coordinateSystems).hasSize(0)

            val temp = ncd.findVariable("fun_soundings.sounding_no") as VariableDS
            assertThat(temp).isNotNull()
            val tempCss = ncd.makeCoordinateSystemsFor(temp)
            assertThat(tempCss).isEmpty()
        }
    }

    @Test
    fun testHdfAuraPointData() {
        val location ="/media/snake/0B681ADF0B681ADF/thredds-test-data/local/thredds-test-data/cdmUnitTest/formats/hdf5/aura/MLS-Aura_L2GP-BrO_v01-52-c01_2007d029.he5"
        openDatasetWithCS(location, true).use { ncd ->
            assertThat(ncd).isNotNull()
            assertThat(ncd.conventionName).isEqualTo("_Coordinates")
            assertThat(ncd.coordinateAxes).hasSize(7)
            assertThat(ncd.coordinateSystems).hasSize(2)

            val temp = ncd.findVariable("HDFEOS/SWATHS/BrO/Data_Fields/L2gpValue") as VariableDS
            assertThat(temp).isNotNull()
            val tempCss = ncd.makeCoordinateSystemsFor(temp)
            assertThat(tempCss).isNotNull()
            assertThat(tempCss).hasSize(1)
            val tempCs = tempCss[0]

            assertThat(tempCs.name).isEqualTo("Time Pressure Latitude Longitude")
            assertThat(tempCs.axesName).isEqualTo("Time Pressure Latitude Longitude")
            assertThat(tempCs.coordinateAxes).hasSize(4)
            assertThat(tempCs.findAxis(AxisType.Lat)).isNotNull()
            assertThat(tempCs.findAxis(AxisType.Lon)).isNotNull()
            assertThat(tempCs.findAxis(AxisType.Pressure)).isNotNull()
            assertThat(tempCs.findAxis(AxisType.Time)).isNotNull()

            assertThat(tempCs.coordinateTransforms).hasSize(0)
            assertThat(tempCs.projection).isNull()
            assertThat(tempCs.verticalTransform).isNull()
        }
    }

    @Test
    fun testHdfEosModis2() {
        val location ="/media/snake/0B681ADF0B681ADF/thredds-test-data/local/thredds-test-data/cdmUnitTest/formats/hdf4/ncidc/MOD10A1.A2008001.h23v15.005.2008003161138.hdf"
        openDatasetWithCS(location, true).use { ncd ->
            assertThat(ncd).isNotNull()
            assertThat(ncd.conventionName).isEqualTo("HDF4-EOS-MODIS")
            assertThat(ncd.coordinateAxes).hasSize(2)
            assertThat(ncd.coordinateSystems).hasSize(1)

            val temp = ncd.findVariable("MOD_Grid_Snow_500m/Data_Fields/Snow_Cover_Daily_Tile") as VariableDS
            assertThat(temp).isNotNull()
            val tempCss = ncd.makeCoordinateSystemsFor(temp)
            assertThat(tempCss).isNotNull()
            assertThat(tempCss).hasSize(1)
            val tempCs = tempCss[0]

            assertThat(tempCs.name).isEqualTo("YDim XDim")
            assertThat(tempCs.axesName).isEqualTo("YDim XDim")
            assertThat(tempCs.coordinateAxes).hasSize(2)
            assertThat(tempCs.findAxis(AxisType.GeoX)).isNotNull()
            assertThat(tempCs.findAxis(AxisType.GeoY)).isNotNull()

            assertThat(tempCs.coordinateTransforms).hasSize(1)
            assertThat(tempCs.projection).isNotNull()
            assertThat(tempCs.projection!!.name).isEqualTo("sinusoidal")
            assertThat(tempCs.verticalTransform).isNull()
        }
    }

    @Test
    fun testHdfEosModis() {
        val location ="../core/src/test/data/hdfeos2/MCD43B2.A2007001.h00v08.005.2007043191624.hdf"
        openDatasetWithCS(location, true).use { ncd ->
            assertThat(ncd).isNotNull()
            assertThat(ncd.conventionName).isEqualTo("HDF4-EOS-MODIS")
            assertThat(ncd.coordinateAxes).hasSize(2)
            assertThat(ncd.coordinateSystems).hasSize(1)

            val temp = ncd.findVariable("MOD_Grid_BRDF/Data_Fields/BRDF_Albedo_Quality") as VariableDS
            assertThat(temp).isNotNull()
            val tempCss = ncd.makeCoordinateSystemsFor(temp)
            assertThat(tempCss).isNotNull()
            assertThat(tempCss).hasSize(1)
            val tempCs = tempCss[0]

            assertThat(tempCs.name).isEqualTo("YDim XDim")
            assertThat(tempCs.axesName).isEqualTo("YDim XDim")
            assertThat(tempCs.coordinateAxes).hasSize(2)
            assertThat(tempCs.findAxis(AxisType.GeoX)).isNotNull()
            assertThat(tempCs.findAxis(AxisType.GeoY)).isNotNull()

            assertThat(tempCs.coordinateTransforms).hasSize(1)
            assertThat(tempCs.projection).isNotNull()
            assertThat(tempCs.projection!!.name).isEqualTo("sinusoidal")
            assertThat(tempCs.verticalTransform).isNull()
        }
    }

    @Test
    fun testHdfEos() {
        val location ="/media/snake/0B681ADF0B681ADF/thredds-test-data/local/thredds-test-data/cdmUnitTest/formats/hdf4/eos/amsua/amsua16_2008.001_37506_0431_0625_WI.eos"
        openDatasetWithCS(location, true).use { ncd ->
            assertThat(ncd).isNotNull()
            assertThat(ncd.conventionName).isEqualTo("HdfEos")
            assertThat(ncd.coordinateAxes).hasSize(3)
            assertThat(ncd.coordinateSystems).hasSize(1)

            val temp = ncd.findVariable("Orbit_37506/Data_Fields/31400\\.42_MHz") as VariableDS
            assertThat(temp).isNotNull()
            val tempCss = ncd.makeCoordinateSystemsFor(temp)
            assertThat(tempCss).isNotNull()
            assertThat(tempCss).hasSize(1)
            val tempCs = tempCss[0]

            assertThat(tempCs.name).isEqualTo("Time Latitude Longitude")
            assertThat(tempCs.axesName).isEqualTo("Time Latitude Longitude")
            assertThat(tempCs.coordinateAxes).hasSize(3)
            assertThat(tempCs.findAxis(AxisType.Lat)).isNotNull()
            assertThat(tempCs.findAxis(AxisType.Lon)).isNotNull()
            assertThat(tempCs.findAxis(AxisType.Time)).isNotNull()

            assertThat(tempCs.coordinateTransforms).hasSize(0)
            assertThat(tempCs.projection).isNull()
            assertThat(tempCs.verticalTransform).isNull()
        }
    }


    @Test
    fun testIfps() {
        val location ="/media/snake/0B681ADF0B681ADF/thredds-test-data/local/thredds-test-data/cdmUnitTest/conventions/ifps/HUNGrids.netcdf"
        openDatasetWithCS(location, true).use { ncd ->
            assertThat(ncd).isNotNull()
            assertThat(ncd.conventionName).isEqualTo("IFPS")
            assertThat(ncd.coordinateAxes).hasSize(29)
            assertThat(ncd.coordinateSystems).hasSize(26)

            ncd.variables.filter { it -> ncd.makeCoordinateSystemsFor(it as VariableDS).isNotEmpty()}.forEach {
                ncd.testCss(it.shortName, "${it.shortName}_timeCoord yCoord xCoord")
            }

            val temp = ncd.findVariable("QPF_SFC") as VariableDS
            assertThat(temp).isNotNull()
            val tempCss = ncd.makeCoordinateSystemsFor(temp)
            assertThat(tempCss).isNotNull()
            assertThat(tempCss).hasSize(1)
            val tempCs = tempCss[0]

            assertThat(tempCs.name).isEqualTo("QPF_SFC_timeCoord yCoord xCoord")
            assertThat(tempCs.axesName).isEqualTo("QPF_SFC_timeCoord yCoord xCoord")
            assertThat(tempCs.coordinateAxes).hasSize(3)
            assertThat(tempCs.findAxis(AxisType.GeoX)).isNotNull()
            assertThat(tempCs.findAxis(AxisType.GeoY)).isNotNull()
            assertThat(tempCs.findAxis(AxisType.Time)).isNotNull()

            assertThat(tempCs.coordinateTransforms).hasSize(1)
            assertThat(tempCs.projection).isNotNull()
            assertThat(tempCs.projection!!.name).isEqualTo("LambertConformal")
            assertThat(tempCs.verticalTransform).isNull()
        }
    }

    @Test
    fun testM3io() {
        val location ="/media/snake/0B681ADF0B681ADF/thredds-test-data/local/thredds-test-data/cdmUnitTest/conventions/m3io/19L.nc"
        openDatasetWithCS(location, true).use { ncd ->
            assertThat(ncd).isNotNull()
            assertThat(ncd.conventionName).isEqualTo("M3IO")
            assertThat(ncd.coordinateAxes).hasSize(5)
            assertThat(ncd.coordinateSystems).hasSize(1)

            val temp = ncd.findVariable("CO") as VariableDS
            assertThat(temp).isNotNull()
            val tempCss = ncd.makeCoordinateSystemsFor(temp)
            assertThat(tempCss).isNotNull()
            assertThat(tempCss).hasSize(1)
            val tempCs = tempCss[0]

            assertThat(tempCs.name).isEqualTo("time level y x")
            assertThat(tempCs.axesName).isEqualTo("time level y x")
            assertThat(tempCs.coordinateAxes).hasSize(4)
            assertThat(tempCs.findAxis(AxisType.GeoX)).isNotNull()
            assertThat(tempCs.findAxis(AxisType.GeoY)).isNotNull()
            assertThat(tempCs.findAxis(AxisType.GeoZ)).isNotNull()
            assertThat(tempCs.findAxis(AxisType.Time)).isNotNull()

            assertThat(tempCs.coordinateTransforms).hasSize(1)
            assertThat(tempCs.projection).isNotNull()
            assertThat(tempCs.projection!!.name).isEqualTo(CF.LAMBERT_CONFORMAL_CONIC)
            assertThat(tempCs.verticalTransform).isNull()
        }
    }

    @Test
    fun testNuwg() {
        val location ="/media/snake/0B681ADF0B681ADF/thredds-test-data/local/thredds-test-data/cdmUnitTest/conventions/nuwg/ruc.nc"
        openDatasetWithCS(location, true).use { ncd ->
            assertThat(ncd).isNotNull()
            assertThat(ncd.conventionName).isEqualTo("NUWGConventions")
            assertThat(ncd.coordinateAxes).hasSize(6)
            assertThat(ncd.coordinateSystems).hasSize(5)

            ncd.testCss("RH", "valtime level y x")
            ncd.testCss("RH_fhg", "valtime fhg y x")
            ncd.testCss("RH_frzlvl", "valtime y x")
            ncd.testCss("RH_lpdg", "valtime lpdg y x")
            ncd.testCss("P_sfc", "valtime y x")
            ncd.testCss("Z_sfc", "y x")

            val temp = ncd.findVariable("omega") as VariableDS
            assertThat(temp).isNotNull()
            val tempCss = ncd.makeCoordinateSystemsFor(temp)
            assertThat(tempCss).isNotNull()
            assertThat(tempCss).hasSize(1)
            val tempCs = tempCss[0]

            assertThat(tempCs.name).isEqualTo("valtime level y x")
            assertThat(tempCs.axesName).isEqualTo("valtime level y x")
            assertThat(tempCs.coordinateAxes).hasSize(4)
            assertThat(tempCs.findAxis(AxisType.GeoX)).isNotNull()
            assertThat(tempCs.findAxis(AxisType.GeoY)).isNotNull()
            assertThat(tempCs.findAxis(AxisType.Pressure)).isNotNull()
            assertThat(tempCs.findAxis(AxisType.Time)).isNotNull()

            assertThat(tempCs.coordinateTransforms).hasSize(1)
            assertThat(tempCs.projection).isNotNull()
            assertThat(tempCs.projection!!.name).isEqualTo(CF.LAMBERT_CONFORMAL_CONIC)
            assertThat(tempCs.verticalTransform).isNull()
        }
    }

    @Test
    fun testWrf2() {
        val location ="/home/snake/tmp/testData/transforms/wrfout_v2_Lambert.nc"
        openDatasetWithCS(location, true).use { ncd ->
            assertThat(ncd).isNotNull()
            assertThat(ncd.conventionName).isEqualTo("WrfConventions")
            assertThat(ncd.coordinateAxes).hasSize(8)

            ncd.testCss("W", "Time z_stag y x")
            ncd.testCss("U", "Time z y x_stag")
            ncd.testCss("V", "Time z y_stag x")
            ncd.testCss("T", "Time z y x")

            val temp = ncd.findVariable("U") as VariableDS
            assertThat(temp).isNotNull()
            val tempCss = ncd.makeCoordinateSystemsFor(temp)
            assertThat(tempCss).isNotNull()
            assertThat(tempCss).hasSize(1)
            val tempCs = tempCss[0]

            assertThat(tempCs.name).isEqualTo("Time z y x_stag")
            assertThat(tempCs.axesName).isEqualTo("Time z y x_stag")
            assertThat(tempCs.coordinateAxes).hasSize(4)
            assertThat(tempCs.findAxis(AxisType.GeoX)).isNotNull()
            assertThat(tempCs.findAxis(AxisType.GeoY)).isNotNull()
            assertThat(tempCs.findAxis(AxisType.Time)).isNotNull()
            assertThat(tempCs.findAxis(AxisType.GeoZ)).isNotNull()

            assertThat(tempCs.coordinateTransforms).hasSize(2)
            assertThat(tempCs.projection).isNotNull()
            assertThat(tempCs.projection!!.name).isEqualTo(CF.LAMBERT_CONFORMAL_CONIC)
            assertThat(tempCs.verticalTransform).isNotNull()
            assertThat(tempCs.verticalTransform!!.name).isEqualTo("wrf_eta_coordinate")
        }
    }

    @Test
    fun testWrf() {
        val location ="/media/snake/0B681ADF0B681ADF/thredds-test-data/local/thredds-test-data/cdmUnitTest/conventions/wrf/global.nc"
        openDatasetWithCS(location, true).use { ncd ->
            assertThat(ncd).isNotNull()
            assertThat(ncd.conventionName).isEqualTo("WrfConventions")
            assertThat(ncd.coordinateAxes).hasSize(10)
            assertThat(ncd.coordinateSystems).hasSize(13)

            ncd.testCss("ZS", "Time soilDepth")
            ncd.testCss("FNM", "Time z")
            ncd.testCss("ZNW", "Time z_stag")

            ncd.testCss("W", "Time z_stag XLAT XLONG")
            ncd.testCss("U", "Time z XLAT_U XLONG_U")
            ncd.testCss("V", "Time z XLAT_V XLONG_V")
            ncd.testCss("T", "Time z XLAT XLONG")

            ncd.testCss("SNOWC", "Time XLAT XLONG")
            ncd.testCss("MAPFAC_U", "Time XLAT_U XLONG_U")
            ncd.testCss("MAPFAC_V", "Time XLAT_V XLONG_V")
            ncd.testCss("SH2O", "Time soilDepth XLAT XLONG")

            val temp = ncd.findVariable("QVAPOR") as VariableDS
            assertThat(temp).isNotNull()
            val tempCss = ncd.makeCoordinateSystemsFor(temp)
            assertThat(tempCss).isNotNull()
            assertThat(tempCss).hasSize(1)
            val tempCs = tempCss[0]

            assertThat(tempCs.name).isEqualTo("Time z XLAT XLONG")
            assertThat(tempCs.axesName).isEqualTo("Time z XLAT XLONG")
            assertThat(tempCs.coordinateAxes).hasSize(4)
            assertThat(tempCs.findAxis(AxisType.Lat)).isNotNull()
            assertThat(tempCs.findAxis(AxisType.Lon)).isNotNull()
            assertThat(tempCs.findAxis(AxisType.Time)).isNotNull()
            assertThat(tempCs.findAxis(AxisType.GeoZ)).isNotNull()

            assertThat(tempCs.coordinateTransforms).hasSize(1)
            assertThat(tempCs.projection).isNull()
            assertThat(tempCs.verticalTransform).isNotNull()
            assertThat(tempCs.verticalTransform!!.name).isEqualTo("wrf_eta_coordinate")
        }
    }

    @Test
    fun testWrfNoTime() {
        val location = "/home/snake/dev/github/devcdm/dataset/src/test/data/dataset/WrfNoTimeVar.nc"

        openDatasetWithCS(location, true).use { ncd ->
            assertThat(ncd).isNotNull()
            assertThat(ncd.conventionName).isEqualTo("WrfConventions")
            assertThat(ncd.coordinateAxes).hasSize(2)
            assertThat(ncd.coordinateSystems).hasSize(1)

            val temp = ncd.findVariable("T2") as VariableDS
            assertThat(temp).isNotNull()
            val tempCss = ncd.makeCoordinateSystemsFor(temp)
            assertThat(tempCss).isNotNull()
            assertThat(tempCss).hasSize(1)
            val tempCs = tempCss[0]

            assertThat(tempCs.name).isEqualTo("y x")
            assertThat(tempCs.axesName).isEqualTo("y x")
            assertThat(tempCs.coordinateAxes).hasSize(2)
            assertThat(tempCs.findAxis(AxisType.GeoX)).isNotNull()
            assertThat(tempCs.findAxis(AxisType.GeoY)).isNotNull()

            assertThat(tempCs.coordinateTransforms).hasSize(1)
            assertThat(tempCs.projection).isNotNull()
            assertThat(tempCs.verticalTransform).isNull()
            assertThat(tempCs.projection!!.name).isEqualTo(CF.LAMBERT_CONFORMAL_CONIC)
        }
    }

    @Test
    fun testZebra() {
        val location =
            "/media/snake/0B681ADF0B681ADF/thredds-test-data/local/thredds-test-data/cdmUnitTest/conventions/zebra/SPOL_3Volumes.nc"
        openDatasetWithCS(location, true).use { ncd ->
            assertThat(ncd).isNotNull()
            assertThat(ncd.conventionName).isEqualTo("Zebra")
            assertThat(ncd.coordinateAxes).hasSize(4)
            assertThat(ncd.coordinateSystems).hasSize(1)

            val temp = ncd.findVariable("VE") as VariableDS
            assertThat(temp).isNotNull()
            val tempCss = ncd.makeCoordinateSystemsFor(temp)
            assertThat(tempCss).isNotNull()
            assertThat(tempCss).hasSize(1)
            val tempCs = tempCss[0]

            assertThat(tempCs.name).isEqualTo("time_offset altitude latitude longitude")
            assertThat(tempCs.axesName).isEqualTo("time_offset altitude latitude longitude")
            assertThat(tempCs.coordinateAxes).hasSize(4)
            assertThat(tempCs.findAxis(AxisType.Lat)).isNotNull()
            assertThat(tempCs.findAxis(AxisType.Lon)).isNotNull()
            assertThat(tempCs.findAxis(AxisType.Time)).isNotNull()
            assertThat(tempCs.findAxis(AxisType.Height)).isNotNull()

            assertThat(tempCs.coordinateTransforms).hasSize(0)
            assertThat(tempCs.projection).isNull()
            assertThat(tempCs.verticalTransform).isNull()
        }
    }

    fun CdmDatasetCS.testCss(varname : String, expectCss : String) {
        val v = this.findVariable(varname) as VariableDS
        assertWithMessage("variable $varname").that(v).isNotNull()
        val css = this.makeCoordinateSystemsFor(v)
        assertWithMessage("variable $varname").that(css).hasSize(1)
        val cs = css[0]
        assertWithMessage("variable $varname").that(cs.axesName).isEqualTo(expectCss)
    }

}