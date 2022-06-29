package cdm.dataset.internal

import com.google.common.truth.Truth.assertThat
import dev.cdm.core.constants.AxisType
import dev.cdm.dataset.api.CdmDataset
import dev.cdm.dataset.api.CdmDatasetCS
import dev.cdm.dataset.api.CdmDatasets
import dev.cdm.dataset.cdmdsl.*
import dev.cdm.dataset.coordsysbuild.findConvention
import org.junit.jupiter.api.Test

class TestCFConventions {
    val cfFile = "/media/snake/Elements/data/cdmUnitTest/conventions/cf/temperature.nc"

    @Test
    fun testCFConventions() {
        val orgDataset = CdmDatasets.openDataset(cfFile, false, null)
        println(orgDataset.write())

        val convention = findConvention(orgDataset)
        val coords = convention.buildCoordinateSystems()
        assertThat(coords).isNotNull()
        println(convention.info)

        val withcs = CdmDatasetCS.builder().copyFrom(orgDataset)
            .setCoordsHelper(coords)
            .setConventionUsed(convention.conventionName)
            .build()
        assertThat(withcs).isNotNull()
        println(withcs.write())

        assertThat(withcs.conventionBuilder).isEqualTo("CFConventions")
        assertThat(withcs.coordinateSystems).hasSize(2)
        assertThat(withcs.coordinateAxes).hasSize(4)
        assertThat(withcs.coordinateTransforms).hasSize(2)

        assertThat(withcs.findCoordinateAxis(AxisType.Time)).isNotNull()
        assertThat(withcs.findVariable("csys")).isNotNull()

        assertThat(withcs.findCoordinateSystem("csys")).isNotNull()
        val csys = withcs.findCoordinateSystem("csys");
        assertThat(csys.projection).isNull()

        println("${withcs.variables.map {it.shortName}}")
        assertThat(withcs.variables).hasSize(7)

    }
}