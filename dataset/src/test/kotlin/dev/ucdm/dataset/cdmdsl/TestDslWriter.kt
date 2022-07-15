package dev.ucdm.dataset.cdmdsl

import com.google.common.truth.Truth.assertThat
import dev.ucdm.dataset.api.CdmDataset
import dev.ucdm.dataset.api.CdmDatasetCS
import dev.ucdm.dataset.cdmdsl.CdmdslDataset
import dev.ucdm.dataset.cdmdsl.cdmdsl
import dev.ucdm.dataset.cdmdsl.writeDsl
import dev.ucdm.dataset.cdmdsl.build
import dev.ucdm.dataset.coordsysbuild.openDatasetWithCoordSys
import dev.ucdm.dataset.ncml.NcmlReader
import dev.ucdm.test.util.datasetLocalNcmlDir
import org.junit.jupiter.api.Test

class TestDslWriter {

    @Test
    fun testWriteDsl() {
        val cdmdsl: CdmdslDataset = cdmdsl( datasetLocalNcmlDir + "nc/example1.nc") {
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