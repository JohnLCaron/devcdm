package dev.cdm.dataset.coordsysbuild

import dev.cdm.dataset.api.CdmDataset
import dev.cdm.dataset.api.CdmDatasetCS
import dev.cdm.dataset.api.CdmDatasets

fun findCoordSysBuilder(dataset: CdmDataset): CoordSysBuilder {
    val conv = dataset.rootGroup.findAttributeString("Conventions", "none")
    return when {
        conv == "Default" -> DefaultConventions()
        conv.startsWith("CF-1.") -> CFConventions()
        conv.startsWith("COARDS") -> DefaultConventions()
        conv.startsWith("GIEF") -> GiefConventions()
        conv.startsWith("MARS") -> DefaultConventions()
        conv.startsWith("NCAR-CSM") -> DefaultConventions()
        conv.startsWith("NUWG") -> NuwgConventions()
        conv.startsWith("Zebra") -> ZebraConventions()
        isHdfEosOmi(dataset) -> HdfEosOmiConventions()
        isHdfEosModis(dataset) -> HdfEosModisConventions()
        isIfpsConventions(dataset) -> IfpsConventions()
        isM3ioConventions(dataset) -> M3ioConventions()
        isWrfConventions(dataset) -> WrfConventions()
        isCedricRadarConvention(dataset) -> CedricRadarConventions()
        else -> CoordSysBuilder()
    }
}

fun openDatasetWithCoordSys(location : String, enhance : Boolean) : CdmDatasetCS {
    val orgDataset = CdmDatasets.openDataset(location, enhance, null)
    return openDatasetWithCoordSys(orgDataset)
}

fun openDatasetWithCoordSys(orgDataset: CdmDataset) : CdmDatasetCS {
    val coordSysBuilder = findCoordSysBuilder(orgDataset)
    val augmentedDataset = coordSysBuilder.augment(orgDataset)

    val coords = coordSysBuilder.buildCoordinateSystems(augmentedDataset)
    val withcs = CdmDatasetCS.builder().copyFrom(augmentedDataset)
        .setCoordsHelper(coords)
        .setConventionUsed(coordSysBuilder.conventionName)
        .build()

    return withcs
}

