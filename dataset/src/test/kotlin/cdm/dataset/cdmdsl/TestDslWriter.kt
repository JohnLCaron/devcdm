package cdm.dataset.cdmdsl

import com.google.common.truth.Truth.assertThat
import dev.cdm.dataset.api.CdmDataset
import dev.cdm.dataset.api.CdmDatasetCS
import dev.cdm.dataset.api.TestCdmDatasets
import dev.cdm.dataset.cdmdsl.CdmdslDataset
import dev.cdm.dataset.cdmdsl.cdmdsl
import dev.cdm.dataset.cdmdsl.writeDsl
import dev.cdm.dataset.cdmdsl.build
import dev.cdm.dataset.coordsysbuild.openDatasetWithCoordSys
import dev.cdm.dataset.ncml.NcmlReader
import org.junit.jupiter.api.Test

class TestDslWriter {

    @Test
    fun testWriteDsl() {
        val cdmdsl: CdmdslDataset = cdmdsl( TestCdmDatasets.datasetLocalNcmlDir + "nc/example1.nc") {
        }
        assertThat(cdmdsl).isNotNull()

        val cd: CdmDatasetCS = cdmdsl.build()
        println(cd.writeDsl())
        val withcs = openDatasetWithCoordSys(cd)
        assertThat(withcs).isNotNull()

        println(withcs.writeDsl())
    }

    @Test
    fun writeDslFromNcml() {
        val location = "/home/snake/tmp/GIEF.ncml"
        val emptyDataset : CdmDataset.Builder<*> = CdmDataset.builder()
        NcmlReader.wrapNcml(emptyDataset, location, null)

        val ncml = emptyDataset.build()
        println(ncml.writeDsl())
    }
}