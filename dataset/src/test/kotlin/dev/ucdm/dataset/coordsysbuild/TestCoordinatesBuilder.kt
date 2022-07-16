package dev.ucdm.dataset.coordsysbuild

import com.google.common.truth.Truth.assertThat
import dev.ucdm.core.constants.AxisType
import dev.ucdm.dataset.api.CdmDatasetCS
import dev.ucdm.dataset.cdmdsl.*
import org.junit.jupiter.api.Test

class TestCoordinatesBuilder {

    fun createTestDataset() : CdmDatasetCS {
        val cdmdsl: CdmdslDataset = cdmdsl() {
            attribute("title").setValue("Example Data")
            attribute("Conventions").setValue("Default")

            dimension("time", 2)
            dimension("lat", 3)
            dimension("lon", 4)

            coordSystem("csys").setAxes("time rlat lon")

            variable("rh") {
                setType("INT")
                setDimensions("time lat lon")
                attribute("units").setValue("percent")
                attribute("long_name").setValue("relative humidity")
                coordSystemRef("csys")
            }
            variable("T") {
                setType("DOUBLE")
                setDimensions("time lat lon")
                attribute("units").setValue("degC")
                attribute("long_name").setValue("surface temperature")
                attribute("_CoordinateAxes").setValue("time height rlat lon")
            }

            variable("ps") {
                setType("INT")
                setDimensions("lat lon")
                attribute("units").setValue("mbar")
            }

            variable("csv") {
                attribute("_CoordinateSystemFor").setValue("rlat lon")
                attribute("_CoordinateTransforms").setValue("ctv ctv2")
            }

            variable("ctv") {
                attribute("_CoordinateTransformType").setValue("LatLonProjection")
            }

            variable("ctv2") {
                attribute("_CoordinateTransformType").setValue("Sigma")
            }

            axis("rlat") {
                setType("FLOAT")
                setDimensions("lat")
                setAxisType("Lat")
                attribute("units").setValue("degrees_north")
                attribute("_CoordinateAliasForDimension").setValue("lat")
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
                attribute("units").setValue("hours since 1970-01-01 12:00:00")
            }
            axis("height") {
                setType("DOUBLE")
                attribute("units").setValue("mbar")
            }
        }
        assertThat(cdmdsl).isNotNull()

        return cdmdsl.build()
    }

    @Test
    fun testCoordSysBuilder() {
        val testDataset = createTestDataset()
        println(testDataset.write())

        val convention = chooseCoordSysBuilder(testDataset)
        val coords = convention.buildCoordinateSystems(testDataset)
        assertThat(coords).isNotNull()
        println(convention.info)

        val withcs = testDataset.toBuilder()
            .setCoordsHelper(coords)
            .setConventionUsed(convention.conventionName)
            .build()
        assertThat(withcs).isNotNull()
        println(withcs.writeDsl())

        assertThat(withcs.conventionBuilder).isEqualTo("DefaultConventions")
        assertThat(withcs.coordinateSystems).hasSize(3)
        assertThat(withcs.coordinateAxes).hasSize(4)
        assertThat(withcs.coordinateTransforms).hasSize(2)

        assertThat(withcs.findCoordinateAxis(AxisType.Time)).isNotNull()
        assertThat(withcs.findVariable("csys")).isNotNull()

        assertThat(withcs.findCoordinateSystem("csys")).isNotNull()
        val csys = withcs.findCoordinateSystem("csys");
        assertThat(csys.projection).isNull()

        println("variables = ${withcs.variables.map {it.fullName}}")
        println("axes = ${withcs.coordinateAxes.map {it.fullName}}")
        println("systems = ${withcs.coordinateSystems.map {it.name}}")
        println("transforms = ${withcs.coordinateTransforms.map {it.name}}")
    }
}