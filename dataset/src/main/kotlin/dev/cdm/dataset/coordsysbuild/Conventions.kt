package dev.cdm.dataset.coordsysbuild

import dev.cdm.dataset.api.CdmDataset

fun findConvention(dataset: CdmDataset) : CoordSysBuilder {
    val conv = dataset.rootGroup.findAttributeString("Conventions", "null")
    return when {
        conv == "Default" -> DefaultConventions(dataset)
        conv.startsWith("CF-1.") -> CFConventions(dataset)
        else -> CoordSysBuilder(dataset)
    }
}
