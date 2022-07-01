package dev.cdm.dataset.coordsysbuild

import dev.cdm.dataset.api.CdmDataset
import dev.cdm.dataset.api.CdmDatasetCS
import dev.cdm.dataset.api.CdmDatasets
import dev.cdm.dataset.cdmdsl.CdmdslDataset
import dev.cdm.dataset.cdmdsl.write
import dev.cdm.dataset.internal.CoordinatesHelper

fun findConvention(dataset: CdmDataset): CoordSysBuilder {
    val conv = dataset.rootGroup.findAttributeString("Conventions", "none")
    return when {
        conv == "Default" -> DefaultConventions()
        conv.startsWith("CF-1.") -> CFConventions()
        conv.startsWith("GIEF") -> GiefConventions()
        isWrfConventions(dataset) -> WrfConventions()
        else -> CoordSysBuilder(conv)
    }
}

fun openDatasetWithCoordSys(location : String, enhance : Boolean) : CdmDatasetCS {
    val orgDataset = CdmDatasets.openDataset(location, enhance, null)
    return openDatasetWithCoordSys(orgDataset)
}

fun openDatasetWithCoordSys(orgDataset: CdmDataset) : CdmDatasetCS {
    val convention = findConvention(orgDataset)
    val augmentedDataset = convention.augment(orgDataset)

    val coords = convention.buildCoordinateSystems(augmentedDataset)
    val withcs = CdmDatasetCS.builder().copyFrom(augmentedDataset)
        .setCoordsHelper(coords)
        .setConventionUsed(convention.conventionName)
        .build()
    return withcs
}

