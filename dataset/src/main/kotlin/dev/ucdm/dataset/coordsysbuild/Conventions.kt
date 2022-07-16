package dev.ucdm.dataset.coordsysbuild

import dev.ucdm.dataset.api.CdmDataset
import dev.ucdm.dataset.api.CdmDatasetCS
import dev.ucdm.dataset.api.CdmDatasets

fun chooseCoordSysBuilder(dataset: CdmDataset): CoordinatesBuilder {
    var conv = dataset.rootGroup.findAttributeString("Conventions", "none")
    if (conv == "none") {
        conv = dataset.rootGroup.findAttributeString("Convention", "none")
    }
    return when {
        conv == "Default" -> DefaultConventions()
        conv.startsWith("CF-1.") -> CFConventions()
        conv.startsWith("COARDS") -> CFConventions()
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
    val coordSysBuilder = chooseCoordSysBuilder(orgDataset)
    val augmentedDataset = coordSysBuilder.augment(orgDataset)

    val coords = coordSysBuilder.buildCoordinateSystems(augmentedDataset)
    val withcs = CdmDatasetCS.builder().copyFrom(augmentedDataset)
        .setCoordsHelper(coords)
        .setConventionUsed(coordSysBuilder.conventionName)
        .build()
    //println(coordSysBuilder.info)
    //println(withcs.writeDsl())

    return withcs
}

