package dev.ucdm.dataset.coordsysbuild

import com.google.common.truth.Truth.assertThat
import dev.ucdm.core.api.Attribute
import dev.ucdm.core.api.Variable
import dev.ucdm.dataset.api.CdmDatasetCS
import dev.ucdm.dataset.api.CdmDatasets
import dev.ucdm.dataset.cdmdsl.write
import dev.ucdm.dataset.cdmdsl.writeDsl
import dev.ucdm.test.util.CdmObjFilter
import dev.ucdm.test.util.CompareCdmDataset
import dev.ucdm.test.util.extraTestDir
import dev.ucdm.test.util.testFilesIn
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.io.IOException
import java.util.stream.Stream


var showOrg = false
var showAug = false
var showInfo = false
var showResult = false

// dataset testing with grib loaded
class TestCompareCoordsysBuilding {
    companion object {
        @Throws(IOException::class)
        @JvmStatic
        fun params(): Stream<Arguments> {
            // return Stream.of(Arguments.of("/home/snake/tmp/testData/transforms/HIRLAMhybrid.ncml"))
            /* return testFilesIn("/media/snake/0B681ADF0B681ADF/thredds-test-data/local/thredds-test-data/cdmUnitTest/conventions")
                .addNameFilter { !it.startsWith("WrfNoTimeVar") && !it.startsWith("0150602_0830_sport_imerg_noHemis_rr.nc")}
                .addNameFilter(FileFilterSkipSuffixes("cdl pdf txt"))
                .withRecursion()
                .build()

             */

            return testFilesIn(extraTestDir)
                .addNameFilter { !it.startsWith("WrfNoTimeVar") &&
                        !it.contains("grib")}
                .withPathFilter{ it ->
                    val pname = it.toString()
                    !pname.contains("exclude") && !pname.contains("npoess")}
                .withRecursion()
                .build()


            /* return testFilesIn(TestCdmDatasets.gridTestDir)
                .addNameFilter { !it.startsWith("WrfNoTimeVar") }
                .withRecursion()
                .build()

             */
        }
    }

    @ParameterizedTest
    @MethodSource("params")
    @Throws(Exception::class)
    fun testCoordinateSystems(filename: String) {
        System.out.printf("TestCdmDatasets  %s%n", filename)
        compareCoordinateSystems(filename)
    }
}

private val ignoreVariables = "time"

private class LocalFilter : CdmObjFilter() {
    override fun attCheckOk(att: Attribute): Boolean {
        return false;
        /* val name = att.shortName
        return !name.startsWith("_Coordinate") &&
                name != "process" &&
                name != "_enhanced" &&
                name != _Coordinate._CoordSysBuilder */
    }

    override fun ignoreVariable(v: Variable): Boolean {
        return ignoreVariables.contains(v.shortName)
    }
}

fun compareCoordinateSystems(filename: String) {
    // TODO these are the same now
    CdmDatasets.openDatasetCS(filename, true).use { ncdc ->
        openNewCoordSys(filename, true).use { withcs ->
            println("Conv = ${withcs.conventionBuilder}")
            val ok = CompareCdmDataset().compare(ncdc, withcs, LocalFilter())
            assertThat(ok).isTrue()
        }
    }
}

fun openNewCoordSys(filename: String, enhance: Boolean): CdmDatasetCS {
    val orgDataset = CdmDatasets.openDataset(filename, enhance, null)
    if (showOrg) println("original = ${orgDataset.write()}")

    val coordSysBuilder = chooseCoordSysBuilder(orgDataset)
    val augmentedDataset = coordSysBuilder.augment(orgDataset)
    if (showAug) println("augmented = ${augmentedDataset.write()}")

    val coords = coordSysBuilder.buildCoordinateSystems(augmentedDataset)
    if (showInfo) println(coordSysBuilder.info)

    val withcs = CdmDatasetCS.builder().copyFrom(augmentedDataset)
        .setCoordsHelper(coords)
        .setConventionUsed(coords.conventionName)
        .build()

    if (showResult) println(withcs.writeDsl())
    return withcs
}