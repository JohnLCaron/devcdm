package dev.ucdm.dataset.cdmdsl

import com.google.common.truth.Truth.assertThat
import dev.ucdm.dataset.api.CdmDatasetCS
import dev.ucdm.dataset.cdmdsl.CdmdslDataset
import dev.ucdm.dataset.cdmdsl.cdmdsl
import dev.ucdm.dataset.cdmdsl.writeDsl
import dev.ucdm.dataset.cdmdsl.build
import org.junit.jupiter.api.Test

class TestDsl {

    @Test
    fun testNoReferencedDataset() {
        val cdmdsl: CdmdslDataset = cdmdsl() {
            attribute("title").setValue("Example Data")
            attribute("_CoordSysBuilder").setValue("dev.ucdm.dataset.conv.DefaultConventions")

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