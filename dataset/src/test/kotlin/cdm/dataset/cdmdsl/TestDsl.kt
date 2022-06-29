package cdm.dataset.cdmdsl

import com.google.common.truth.Truth.assertThat
import dev.cdm.dataset.api.CdmDatasetCS
import dev.cdm.dataset.api.TestCdmDatasets
import dev.cdm.dataset.cdmdsl.CdmdslDataset
import dev.cdm.dataset.cdmdsl.cdmdsl
import dev.cdm.dataset.cdmdsl.writeDsl
import dev.cdm.dataset.internal.CoordinatesHelper
import dev.cdm.dataset.cdmdsl.build
import dev.cdm.dataset.coordsysbuild.findConvention
import org.junit.jupiter.api.Test

class TestDsl {

    @Test
    fun testWriteDsl() {
        val cdmdsl: CdmdslDataset = cdmdsl( TestCdmDatasets.datasetLocalNcmlDir + "nc/example1.nc") {
        }
        assertThat(cdmdsl).isNotNull()

        val cd: CdmDatasetCS = cdmdsl.build()
        println(cd.writeDsl())

        val builder = findConvention(cd)
        val cshb = builder.buildCoordinateSystems()
        assertThat(cshb).isNotNull()

        val coords = CoordinatesHelper.builder()
        coords.addCoordinateAxes(cshb.coordAxes)
        coords.addCoordinateSystems(cshb.coordSys)
        coords.addCoordinateTransforms(cshb.coordTransforms)

        val withcs = cd.toBuilder().setCoordsHelper(cshb).build()
        assertThat(withcs).isNotNull()
        println(withcs.writeDsl())
    }

    @Test
    fun testNoReferencedDataset() {
        val cdmdsl: CdmdslDataset = cdmdsl() {
            attribute("title").setValue("Example Data")
            attribute("_CoordSysBuilder").setValue("dev.cdm.dataset.conv.DefaultConventions")

            dimension("time", 2)
            dimension("lat", 3)
            dimension("lon", 4)

            variable("rh") {
                setType("INT")
                setDimensions("time lat lon")
                attribute("units").setValue("percent")
                attribute("long_name").setValue("relative humidity")
                coordSystem("time lat lon")
            }
            variable("T") {
                setType("DOUBLE")
                setDimensions("time lat lon")
                attribute("units").setValue("degC")
                attribute("long_name").setValue("surface temperature")
                coordSystem("time lat lon")
            }

            axis("lat") {
                setType("FLOAT")
                setDimensions("lat")
                setAxisType("Lat")
                attribute("units").setValue("degrees_north")
                attribute("_CoordinateAxisType").setValue("Lat")
            }
            axis("lon") {
                setType("FLOAT")
                setDimensions("lon")
                setAxisType("Lon")
                attribute("units").setValue("degrees_east")
                attribute("_CoordinateAxisType").setValue("Lon")
            }
            axis("time") {
                setType("INT")
                setDimensions("time")
                setAxisType("null")
                attribute("units").setValue("hours")
            }
        }
        assertThat(cdmdsl).isNotNull()

        val cd: CdmDatasetCS = cdmdsl.build()
        println(cd.writeDsl())
    }
}