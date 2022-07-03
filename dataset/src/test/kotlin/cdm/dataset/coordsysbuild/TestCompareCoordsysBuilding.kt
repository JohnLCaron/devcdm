package cdm.dataset.coordsysbuild

import com.google.common.truth.Truth.assertThat
import dev.cdm.core.api.Attribute
import dev.cdm.core.constants._Coordinate
import dev.cdm.dataset.api.CdmDatasetCS
import dev.cdm.dataset.api.CdmDatasets
import dev.cdm.dataset.cdmdsl.write
import dev.cdm.dataset.cdmdsl.writeDsl
import dev.cdm.dataset.coordsysbuild.findCoordSysBuilder
import dev.cdm.dataset.util.CdmObjFilter
import dev.cdm.dataset.util.CompareCdmDataset
import dev.cdm.test.util.FileFilterSkipSuffixes
import dev.cdm.test.util.TestFiles
import dev.cdm.test.util.testFilesIn
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.io.IOException
import java.util.stream.Stream

var showOrg = true
var showInfo = true
var showDsl = true

class TestCompareCoordsysBuilding {
    companion object {
        @Throws(IOException::class)
        @JvmStatic
        fun params(): Stream<Arguments> {
            return testFilesIn(TestFiles.coreLocalDir + "/hdfeos2")
                .addNameFilter { !it.startsWith("WrfNoTimeVar") }
                .addNameFilter(FileFilterSkipSuffixes("cdl txt"))
                .withRecursion()
                .build()
           // return Stream.of(
               // Arguments.of(TestCdmDatasets.coreLocalDir + "WrfNoTimeVar.nc"),
           /* return Stream.concat(
                Files.list(Paths.get(TestCdmDatasets.datasetLocalDir))
                    .filter { file: Path? -> !Files.isDirectory(file) }
                    .filter { file: Path -> !file.fileName.toString().startsWith("WrfNoTimeVar") }
                    .map { obj: Path -> obj.toString() }
                    .map { arguments: String? -> Arguments.of(arguments) },
                Stream.of(
                    Arguments.of("/media/snake/0B681ADF0B681ADF/thredds-test-data/local/thredds-test-data/cdmUnitTest/conventions/gief/coamps.wind_uv.nc"),
                    Arguments.of(TestCdmDatasets.datasetLocalNcmlDir + "testRead.xml"),
                    Arguments.of(TestCdmDatasets.datasetLocalNcmlDir + "readMetadata.xml"),
                    Arguments.of(TestCdmDatasets.datasetLocalNcmlDir + "testReadHttps.xml"),
                )  // */

        }
    }

    class LocalFilter : CdmObjFilter() {
        override fun attCheckOk(att: Attribute): Boolean {
            val name = att.shortName
            return !name.startsWith("_Coordinate") &&
                    name != "calendar" &&
                    name != _Coordinate._CoordSysBuilder
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

fun compareCoordinateSystems(filename: String) {
    CdmDatasets.openDatasetCS(filename, true).use { ncdc ->
        openNewCoordSys(filename, true).use { withcs ->
            val ok = CompareCdmDataset().compare(ncdc, withcs, TestCompareCoordsysBuilding.LocalFilter())
            assertThat(ok).isTrue()
        }
    }
}

fun openNewCoordSys(filename: String, enhance : Boolean): CdmDatasetCS {
    val orgDataset = CdmDatasets.openDataset(filename, enhance, null)

    val convention = findCoordSysBuilder(orgDataset)
    val augmentedDataset = convention.augment(orgDataset)
    if (showOrg) println(augmentedDataset.write())

    val coords = convention.buildCoordinateSystems(augmentedDataset)
    if (showInfo) println(convention.info)

    val withcs = CdmDatasetCS.builder().copyFrom(augmentedDataset)
        .setCoordsHelper(coords)
        .setConventionUsed(convention.conventionName)
        .build()

    if (showDsl) println(withcs.writeDsl())
    return withcs
}