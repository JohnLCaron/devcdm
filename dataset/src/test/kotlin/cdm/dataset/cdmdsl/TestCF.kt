package cdm.dataset.cdmdsl

import com.google.common.truth.Truth.assertThat
import dev.cdm.array.Array
import dev.cdm.core.api.CdmFile
import dev.cdm.core.constants.AxisType
import dev.cdm.dataset.api.CdmDatasetCS
import dev.cdm.dataset.api.CdmDatasets
import dev.cdm.dataset.api.VariableDS
import dev.cdm.dataset.cdmdsl.CdmdslDataset
import dev.cdm.dataset.cdmdsl.cdmdsl
import dev.cdm.dataset.cdmdsl.write
import dev.cdm.dataset.cdmdsl.writeDsl
import dev.cdm.dataset.cdmdsl.build
import org.junit.jupiter.api.Test
import java.io.IOException

class TestCF {
    val cfFile = "/media/snake/Elements/data/cdmUnitTest/conventions/cf/temperature.nc"

    @Test
    @Throws(IOException::class)
    fun testCF() {
        println(cfFile)
        CdmDatasets.openDatasetWithCS(cfFile, true).use { ncd ->
            println(ncd.write())
            assertThat(ncd.conventionBuilder).startsWith("CF")
            val cs = ncd.findCoordinateSystem("level lat y lon x")
            assertThat(cs).isNotNull()
            val temp = ncd.findVariable("Temperature") as VariableDS
            assertThat(temp).isNotNull()
            val tempcss = ncd.makeCoordinateSystemsFor(temp)
            assertThat(tempcss.size).isEqualTo(1)
            assertThat(tempcss.get(0)).isEqualTo(cs)

            val tca = ncd.findCoordinateAxis(AxisType.GeoY)
            assertThat(tca).isNotNull()
            val times = tca.readArray() as Array<Number>
            assertThat(times[0].toInt()).isEqualTo(-2286000)

            assertThat(tempcss.get(0).findAxis(AxisType.GeoY)).isEqualTo(tca)
        }
    }

    @Test
    fun testCFdsl() {
        val cdmdsl: CdmdslDataset = cdmdsl(cfFile, false) {
            coordSystem("level y x").setProjection("Lambert_Conformal")
            transform("level").useVariable("level")

            variable("Temperature") {
                coordSystem("level y x")
            }
        }
        assertThat(cdmdsl).isNotNull()

        val ncd = cdmdsl.build()
        val withcs = CdmDatasets.openDatasetCS(ncd, true)

        // println(ncd.write())
        println(withcs.writeDsl())

        val temp = withcs.findVariable("Temperature") as VariableDS
        assertThat(temp).isNotNull()
        assertThat(withcs.makeCoordinateSystemsFor(temp).size).isEqualTo(1)

        val cs = withcs.findCoordinateSystem("y x")
        assertThat(cs).isNotNull()
    }

}