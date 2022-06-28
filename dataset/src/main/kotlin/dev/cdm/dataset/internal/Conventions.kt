package dev.cdm.dataset.internal

import dev.cdm.dataset.api.CdmDatasetCS

fun findConvention(dataset: CdmDatasetCS.Builder<*>) : CoordSysBuilder {
    val conv = dataset.rootGroup.attributeContainer.findAttributeString("Conventions", null)
    return CFConventions(dataset)
}