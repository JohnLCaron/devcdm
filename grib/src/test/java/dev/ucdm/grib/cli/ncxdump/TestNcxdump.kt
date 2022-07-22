package dev.ucdm.grib.cli.ncxdump

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
    fun testHrrrPartition() {
        main(arrayOf("-in", oldTestDir + "gribCollections/hrrr/GSD_HRRR_CONUS_3km_surface.ncx4"))
    }

    @Test
    fun testHrrrCollectionOld() {
        main(arrayOf("-in", oldTestDir + "gribCollections/hrrr/DewpointTempFromGsdHrrrrConus3surface.ncx4"))
    }

    @Test
    fun testHrrrCollectionNew() {
        main(arrayOf("-in", oldTestDir + "gribCollections/hrrr/topdog-hrrr.ncx4"))
    }

    @Test
    fun testGbx9CollectionOld() {
        main(arrayOf("-in", oldTestDir + "gribCollections/namAlaska22/namAlaska22.ncx4"))
    }

    @Test
    fun testGbx9CollectionNew() {
        main(arrayOf("-in", oldTestDir + "gribCollections/namAlaska22/topdog-namAlaska22.ncx4", "-sparse"))
    }

    @Test
    fun testPartition1OldSub() {
        main(arrayOf("-in", oldTestDir + "gribCollections/gfs_conus80/20141025/GFS_CONUS_80km_20141025_0000.grib1.ncx4"))
    }

    @Test
    fun testPartition1Old() {
        main(arrayOf("-in", oldTestDir + "gribCollections/gfs_conus80/20141025/gfsConus80_file-20141025.ncx4"))
    }

    @Test
    fun testPartition1New() {
        main(arrayOf("-in", oldTestDir + "gribCollections/gfs_conus80/20141025/testCreatePartitionGrib1-20141025.ncx4"))
    }

    @Test
    fun testPartition2New() {
        main(arrayOf("-in", oldTestDir + "gribCollections/gfs_2p5deg/testCreatePartitionGrib2-gfs_2p5deg.ncx4"))
    }

}