package dev.ucdm.dataset.cli.ncdump

import dev.ucdm.test.util.datasetLocalNcmlDir
import org.junit.jupiter.api.Test


class TestNcdump {
    var filename: String = datasetLocalNcmlDir + "nc/time0.nc"

    @Test
    fun testNcdumpCli() {
        main(
            arrayOf(
                "-in",
                filename,
                "-type",
                "cdl",
            )
        )
    }

    @Test
    fun testNcdumpVarAll() {
        main(
            arrayOf(
                "-in", filename,
                "-vall"
            )
        )
    }

    @Test
    fun testNcdumpVarCoordinates() {
        main(
            arrayOf(
                "-in", filename,
                "-c"
            )
        )
    }

    @Test
    fun testNcdumpVar() {
        main(
            arrayOf(
                "-in", filename,
                "-v", "lat;lon"
            )
        )
    }

    @Test
    fun testNcdumpVarT() {
        main(
            arrayOf(
                "-in", filename,
                "-v", "T"
            )
        )
    }

    @Test
    fun testNcdumpVarSection() {
        main(
            arrayOf(
                "-in", filename,
                "-v", "T(1:2,0:3:2)"
            )
        )
    }
    @Test
    fun testNcdumpVarSection2() {
        main(
            arrayOf(
                "-in", filename,
                "-v", "T(1:2,0:3:2);lat(0)"
            )
        )
    }

    @Test
    fun testNcdumpNcml() {
        main(
            arrayOf(
                "-in", filename,
                "-type", "ncml",
            )
        )
    }

    @Test
    fun testNcdumpNcmlCoordinates() {
        main(
            arrayOf(
                "-in", filename,
                "-type", "ncml",
                "-c"
            )
        )
    }

    @Test
    fun testNcdumpNcmlAll() {
        main(
            arrayOf(
                "-in", filename,
                "-type", "ncml",
                "-vall"
            )
        )
    }

}