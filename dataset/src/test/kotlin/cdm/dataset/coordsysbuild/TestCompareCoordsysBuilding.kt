package cdm.dataset.coordsysbuild

import com.google.common.truth.Truth.assertThat
import dev.cdm.core.api.Attribute
import dev.cdm.core.constants._Coordinate
import dev.cdm.dataset.api.CdmDatasetCS
import dev.cdm.dataset.api.CdmDatasets
import dev.cdm.dataset.api.TestCdmDatasets
import dev.cdm.dataset.cdmdsl.write
import dev.cdm.dataset.coordsysbuild.findConvention
import dev.cdm.dataset.util.CompareCdmDatasets
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.stream.Stream

val showOrg = true
val showInfo = true
val showResult = true

class TestCompareCoordsysBuilding {
    companion object {
        @Throws(IOException::class)
        @JvmStatic
        fun params(): Stream<Arguments> {
           return Stream.of(
               // Arguments.of(TestCdmDatasets.coreLocalDir + "WrfNoTimeVar.nc"),
               Arguments.of("/media/snake/0B681ADF0B681ADF/thredds-test-data/local/thredds-test-data/cdmUnitTest/conventions/gief/coamps.wind_uv.nc"),
           /* return Stream.concat(
                Files.list(Paths.get(TestCdmDatasets.coreLocalDir))
                    .filter { file: Path? -> !Files.isDirectory(file) }
                    // .filter { file: Path -> !file.fileName.toString().startsWith("Wrf") }
                    .map { obj: Path -> obj.toString() }
                    .map { arguments: String? -> Arguments.of(arguments) },
                Stream.of(
                    Arguments.of(TestCdmDatasets.datasetLocalNcmlDir + "testRead.xml"),
                    Arguments.of(TestCdmDatasets.datasetLocalNcmlDir + "readMetadata.xml"),
                    Arguments.of(TestCdmDatasets.datasetLocalNcmlDir + "testReadHttps.xml")
                )  // */
            )
        }
    }

    class LocalFilter : CompareCdmDatasets.ObjFilter {
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
    fun compareCoordinateSystems(filename: String) {
        System.out.printf("TestCdmDatasets  %s%n", filename)
            CdmDatasets.openDatasetCS(filename, true).use { ncdc ->
                openNewCoordSys(filename, true).use { withcs ->
                    val ok = CompareCdmDatasets().compare(ncdc, withcs, LocalFilter())
                    assertThat(ok).isTrue()
            }
        }
    }
}

fun openNewCoordSys(filename: String, enhance : Boolean): CdmDatasetCS {
    val orgDataset = CdmDatasets.openDataset(filename, enhance, null)
    if (showOrg) println(orgDataset.write())

    val convention = findConvention(orgDataset)
    val augmentedDataset = convention.augment(orgDataset)

    val coords = convention.buildCoordinateSystems(augmentedDataset)
    if (showInfo) println(convention.info)

    val withcs = CdmDatasetCS.builder().copyFrom(augmentedDataset)
        .setCoordsHelper(coords)
        .setConventionUsed(convention.conventionName)
        .build()

    if (showResult) println(withcs.write())
    return withcs
}