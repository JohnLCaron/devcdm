package dev.cdm.dataset.coordsysbuild

import dev.cdm.dataset.api.CdmDataset
import dev.cdm.dataset.api.CdmDatasetCS
import dev.cdm.dataset.api.CdmDatasets
import dev.cdm.dataset.cdmdsl.writeDsl

fun findCoordSysBuilder(dataset: CdmDataset): CoordinatesBuilder {
    var conv = dataset.rootGroup.findAttributeString("Conventions", "none")
    if (conv == "none") {
        conv = dataset.rootGroup.findAttributeString("Convention", "none")
    }
    return when {
        conv == "Default" -> DefaultConventions()
        conv.startsWith("CF-1.") -> CFConventions()
        conv.startsWith("COARDS") -> DefaultConventions()
        conv.startsWith("GDV") -> GdvConventions()
        conv.startsWith("GIEF") -> GiefConventions()
        conv.startsWith("MARS") -> DefaultConventions()
        conv.startsWith("NCAR-CSM") -> DefaultConventions()
        conv.startsWith("NUWG") -> NuwgConventions()
        isAwipsSatConvention(dataset) -> AwipsSatConventions() // must come before isAwipsConvention
        isAwipsConvention(dataset) -> AwipsConventions()
        isCedricRadarConvention(dataset) -> CedricRadarConventions()
        isHdfEosOmi(dataset) -> HdfEosOmiConventions()
        isHdfEosModis(dataset) -> HdfEosModisConventions()
        isHdfEos(dataset) -> HdfEosConventions() // come after other HdfEos
        isIfpsConventions(dataset) -> IfpsConventions()
        isM3ioConventions(dataset) -> M3ioConventions()
        isWrfConventions(dataset) -> WrfConventions()
        isZebraConvention(dataset) -> ZebraConventions()
        else -> CoordinatesBuilder()
    }
}

fun openDatasetWithCoordSys(location : String, enhance : Boolean) : CdmDatasetCS {
    val orgDataset = CdmDatasets.openDataset(location, enhance, null)
    return openDatasetWithCoordSys(orgDataset)
}

fun openDatasetWithCoordSys(orgDataset: CdmDataset) : CdmDatasetCS {
    val coordSysBuilder = findCoordSysBuilder(orgDataset)
    val augmentedDataset = coordSysBuilder.augment(orgDataset)
    println(augmentedDataset.writeDsl())

    val coords = coordSysBuilder.buildCoordinateSystems(augmentedDataset)
    val withcs = CdmDatasetCS.builder().copyFrom(augmentedDataset)
        .setCoordsHelper(coords)
        .setConventionUsed(coordSysBuilder.conventionName)
        .build()
    println(coordSysBuilder.info)

    return withcs
}

