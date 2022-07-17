package dev.ucdm.dataset.cli.nccopy

import dev.ucdm.dataset.cli.TestNetcdfCopier
import dev.ucdm.test.util.datasetLocalNcmlDir
import dev.ucdm.test.util.gribLocalDir
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.io.File
import java.util.stream.Stream


class TestNccopy {

    companion object {
        @TempDir
        @JvmStatic
        var tempFolder: File? = null

        @JvmStatic
        fun params(): Stream<Arguments?>? {
            return Stream.of(
                Arguments.of(datasetLocalNcmlDir + "nc/time0.nc"),
                Arguments.of(datasetLocalNcmlDir + "modifyVars.xml"),
                Arguments.of(datasetLocalNcmlDir + "testReadOverride.xml"),
                Arguments.of(gribLocalDir + "Lannion.pds31.grib2"),
                Arguments.of(gribLocalDir + "afwa.grib1")
            )
        }
    }

    @ParameterizedTest
    @MethodSource("params")
    fun testNccopy(filein : String) {
        val fileout = File.createTempFile("TestNccopy", ".nc", tempFolder).absolutePath
        main(
            arrayOf(
                "-in", filein,
                "-out", fileout,
            )
        )
    }

}