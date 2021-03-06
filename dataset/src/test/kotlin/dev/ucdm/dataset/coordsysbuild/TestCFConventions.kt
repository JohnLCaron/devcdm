package dev.ucdm.dataset.coordsysbuild

import com.google.common.truth.Truth.assertThat
import dev.ucdm.core.constants.AxisType
import dev.ucdm.dataset.api.CdmDatasetCS
import dev.ucdm.dataset.api.CdmDatasets
import dev.ucdm.dataset.api.VariableDS
import dev.ucdm.dataset.cdmdsl.*
import dev.ucdm.test.util.datasetLocalDir
import dev.ucdm.test.util.testFilesIn
import org.junit.jupiter.api.Test

class TestCFConventions {
    val cfFile = "/media/snake/Elements/data/cdmUnitTest/conventions/cf/temperature.nc"

    @Test
    fun testCFConventions() {
        val orgDataset = CdmDatasets.openDataset(cfFile, false, null)
        // println(orgDataset.write())

        val convention = chooseCoordSysBuilder(orgDataset)
        val coords = convention.buildCoordinateSystems(orgDataset)
        assertThat(coords).isNotNull()
        println(convention.info)

        val withcs = CdmDatasetCS.builder().copyFrom(orgDataset)
            .setCoordsHelper(coords)
            .setConventionUsed(convention.conventionName)
            .build()
        assertThat(withcs).isNotNull()
        // println(withcs.write())

        assertThat(withcs.conventionBuilder).isEqualTo("CFConventions")
        assertThat(withcs.coordinateSystems).hasSize(3)
        assertThat(withcs.coordinateAxes).hasSize(5)
        assertThat(withcs.coordinateTransforms).hasSize(2)

        assertThat(withcs.findCoordinateAxis(AxisType.GeoZ)).isNotNull()
        assertThat(withcs.findVariable("level")).isNotNull()

        val csys = withcs.findCoordinateSystem("level lat y lon x");
        assertThat(csys).isNotNull()
        assertThat(csys.projection).isNotNull()
        assertThat(csys.projection!!.name).isEqualTo("lambert_conformal_conic")

        val temp = withcs.findVariable("Temperature") as VariableDS
        assertThat(temp).isNotNull()
        val tempCs = withcs.makeCoordinateSystemsFor(temp)
        assertThat(tempCs).isNotNull()
        assertThat(tempCs.size).isEqualTo(1)
        println("tempCs = ${tempCs}")
        assertThat(tempCs[0].name).isEqualTo("level lat y lon x")

        val lat = withcs.findVariable("lat") as VariableDS
        assertThat(lat).isNotNull()
        val latCs = withcs.makeCoordinateSystemsFor(lat)
        assertThat(latCs).isNotNull()
        assertThat(latCs.size).isEqualTo(1)
        assertThat(latCs[0].name).isEqualTo("y x")

        println("variables = ${withcs.variables.map {it.fullName}}")
        println("axes = ${withcs.coordinateAxes.map {it.fullName}}")
        println("systems = ${withcs.coordinateSystems.map {it.name}}")
        println("transforms = ${withcs.coordinateTransforms.map {it.name}}")
    }

    @Test
    fun testDir() {
        val files = testFilesIn(datasetLocalDir)
            .addNameFilter { !it.startsWith("WrfNoTimeVar") && it.endsWith(".nc")}
            .withRecursion()
            .build()
        files.forEach {
            println("${it.get()[0]}")
        }
    }
}