package dev.ucdm.dataset.cdmdsl

import com.google.common.truth.Truth.assertThat
import dev.ucdm.array.ArrayType
import dev.ucdm.dataset.cdmdsl.CdmdslDataset
import dev.ucdm.dataset.cdmdsl.cdmdsl
import org.junit.jupiter.api.Test

class TestCdmDsl {

    @Test
    fun testModifyAtts() {
        val cdmdsl: CdmdslDataset = cdmdsl("nc/example1.nc") {
            attribute("Conventions").setValue("Metapps")
            attribute("title").remove()
            variable("rh") {
                attribute("units").rename("UNITS")
                attribute("longer_name").setValue("Abe said what?")
                attribute("long_name").remove()
            }
        }
        assertThat(cdmdsl).isNotNull()
    }

    @Test
    fun testModifyVars() {
        val cdmdsl = cdmdsl("nc/example.nc") {
            attribute("Conventions", "added")
            attribute("title").remove()

            dimension("lat", 3)
            variable("deltaLat", ArrayType.DOUBLE, dimensions = "lat").setValues(.1, .1, .01)

            variable("T").rename("Temperature")
            variable("rh") {
                rename("ReletiveHumidity")
                attribute("long_name2").setValue("relatively humid")
                attribute("long_name").remove()
            }
        }
        assertThat(cdmdsl).isNotNull()
    }

    @Test
    fun testReadMetadata() {
        val cdmdsl = cdmdsl("nc/example1.nc") {
            attribute("title", "Example Data")
            attribute("testByte", "1 2 3 4", ArrayType.BYTE)
            attribute("testShort", "1 2 3 4", ArrayType.SHORT)
            attribute("testInt", "1 2 3 4", ArrayType.INT)
            attribute("testFloat", "1 2 3 4", ArrayType.FLOAT)
            attribute("testDouble", "1 2 3 4", ArrayType.DOUBLE)
        }
        assertThat(cdmdsl).isNotNull()
    }

    @Test
    fun testCoordSystem() {
        val cdmdsl = cdmdsl("nc/example.nc") {
            coordSystem("lat lon time")

            variable("rh") {
                rename("ReletiveHumidity")
                attribute("long_name", "relatively humidity")
                attribute("units", "percent")
            }
            variable("lat", ArrayType.FLOAT, "lat") {
                setValues(41.0, 40.0, 39.0)
                attribute("units", "degrees_north")
                attribute("AxisType", "Lat")
            }
            variable("lon", ArrayType.FLOAT, "lon") {
                setValues(-109.0, -107.0, -105.0, -103.0)
                attribute("units", "degrees_east")
                attribute("AxisType", "Lon")
            }
            variable("time", ArrayType.INT, "time") {
                setValues(6, 18, 24, 36)
                attribute("units", "hours since 2020/11/11T00:00:01")
                attribute("AxisType", "Time")
            }
        }
        assertThat(cdmdsl).isNotNull()
    }

    @Test
    fun testCoordSystem2() {
        val cdmdsl = cdmdsl("nc/something.nc", false) {
            coordSystem("x y time").setProjection("Mercator")
            transform("Mercator") {
                attribute("longitude of origin").setValue(23.0)
                attribute("standard parellel").setValue(60.0)
            }
            transform("UTM").useVariable("proj")

            variable("rh") {
                coordSystem("x y time")
            }
            variable("y", ArrayType.FLOAT, "y") {
                setValues(41.0, 40.0, 39.0)
                attribute("units", "km")
                attribute("AxisType", "GeoY")
            }
            variable("x", ArrayType.FLOAT, "x") {
                setValues(-109.0, -107.0, -105.0, -103.0)
                attribute("units", "km")
                attribute("AxisType", "GeoX")
            }
            variable("time", ArrayType.INT, "time") {
                setValues(6, 18, 24, 36)
                attribute("units", "hours since 2020/11/11T00:00:01")
                attribute("AxisType", "Time")
            }
        }
        assertThat(cdmdsl).isNotNull()
    }
}