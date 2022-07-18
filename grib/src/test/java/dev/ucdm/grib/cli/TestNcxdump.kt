package dev.ucdm.grib.cli

import dev.ucdm.test.util.extraTestDir
import dev.ucdm.test.util.gribLocalDir
import dev.ucdm.test.util.oldTestDir
import org.junit.jupiter.api.Test


class TestNcxdump {

    @Test
    fun testGrib1() {
        main(arrayOf("-in", oldTestDir + "formats/grib1/CCCma_SRES_A2_HGT500_1-10.grb.ncx4"))
    }

    @Test
    fun testGrib2() {
        main(arrayOf("-in", oldTestDir + "formats/grib2/berkes.grb2.ncx4"))
    }

    @Test
    fun testCollection() {
        main(arrayOf("-in", oldTestDir + "gribCollections/rdavm/ds083.2/PofP/2004/200406/ds083.2-pofp-200406.ncx4"))
    }

    @Test
    fun testPartition() {
        main(arrayOf("-in", oldTestDir + "gribCollections/rdavm/ds083.2/PofP/2004/ds083.2-pofp-2004.ncx4"))
    }

    @Test
    fun testPofP() {
        main(arrayOf("-in", oldTestDir + "gribCollections/rdavm/ds083.2/PofP/ds083.2-pofp.ncx4"))
    }

    @Test
    fun testEcmwf() {
        main(arrayOf("-in", oldTestDir + "gribCollections/ecmwf/mwp/ECMWFmwp.ncx4"))
    }

    @Test
    fun testHrrr() {
        main(arrayOf("-in", oldTestDir + "gribCollections/hrrr/GSD_HRRR_CONUS_3km_surface.ncx4"))
    }

}