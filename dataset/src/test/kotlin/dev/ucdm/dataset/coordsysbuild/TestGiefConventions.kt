package dev.ucdm.dataset.coordsysbuild

import com.google.common.truth.Truth.assertThat
import dev.ucdm.core.calendar.CalendarDate
import dev.ucdm.core.calendar.CalendarDateUnit
import dev.ucdm.core.constants.AxisType
import dev.ucdm.dataset.api.CdmDatasets
import dev.ucdm.dataset.api.VariableDS
import dev.ucdm.dataset.cdmdsl.*
import org.junit.jupiter.api.Test

class TestGiefConventions {
    val testFile = "/media/snake/0B681ADF0B681ADF/thredds-test-data/local/thredds-test-data/cdmUnitTest/conventions/gief/coamps.wind_uv.nc";

    @Test
    fun testGiefConventions() {
        val withcs = CdmDatasets.openDatasetWithCS(testFile, false)
        assertThat(withcs).isNotNull()
        println(withcs.write())

        assertThat(withcs.conventionBuilder).isEqualTo("GIEF")
        assertThat(withcs.findAttribute("Conventions")?.stringValue).isEqualTo("GIEF/GIEF-F")
        assertThat(withcs.coordinateSystems).hasSize(1)
        assertThat(withcs.coordinateAxes).hasSize(4)
        assertThat(withcs.coordinateTransforms).hasSize(0)

        assertThat(withcs.findCoordinateAxis(AxisType.Lat)).isNotNull()
        val csys = withcs.findCoordinateSystem("time level latitude longitude");
        assertThat(csys).isNotNull()
        assertThat(csys.projection).isNull()

        val u = withcs.findVariable("U-Component") as VariableDS
        assertThat(u).isNotNull()
        val ucs = withcs.makeCoordinateSystemsFor(u)
        assertThat(ucs).isNotNull()
        assertThat(ucs.size).isEqualTo(1)
        println("ucs = ${ucs}")
        assertThat(ucs[0].name).isEqualTo("time level latitude longitude")

        val lat = withcs.findVariable("latitude") as VariableDS
        assertThat(lat).isNotNull()
        val latCs = withcs.makeCoordinateSystemsFor(lat)
        assertThat(latCs).isNotNull()
        assertThat(latCs.size).isEqualTo(0)
        assertThat(lat.shape).isEqualTo( IntArray(1) {161})
        val latValues = lat.readArray()
        assertThat(latValues.size).isEqualTo( 161)
        assertThat(latValues.get(0)).isEqualTo( 0)
        assertThat(latValues.get(160)).isEqualTo( 160 * .2)

        val time = withcs.findVariable("time") as VariableDS
        assertThat(time).isNotNull()
        val timeVal = time.readScalarInt()
        val timUnit = time.findAttributeString("units", null)
        assertThat(CalendarDateUnit.isDateUnit(timUnit))
        val dateUnit = CalendarDateUnit.fromUdunitString(null, timUnit).orElseThrow()
        println("dateUnit = ${dateUnit}")
        println("calendarDate = ${dateUnit.makeCalendarDate(timeVal.toLong())}")
        println("reference_time att = ${withcs.findAttribute("reference_time")}")

        val dateUnit2 = CalendarDate.parse("2002-10-09T12:00:00-6")
        println("reference date = ${dateUnit2} in time zone 6")
    }
}