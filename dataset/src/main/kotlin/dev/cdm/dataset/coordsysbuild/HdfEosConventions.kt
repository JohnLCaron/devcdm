package dev.cdm.dataset.coordsysbuild

import dev.cdm.core.api.CdmFile
import dev.cdm.core.constants.*
import dev.cdm.dataset.api.CdmDataset
import dev.cdm.dataset.api.CdmDatasetCS

open class HdfEosConventions(name: String = "HdfEos-Aura-Omi") : CoordinatesBuilder(name) {

    override fun augment(orgDataset: CdmDataset): CdmDataset {
        val featureType = orgDataset.findAttribute("featureType")
        if (featureType != null && featureType.stringValue == "SWATH") {
            val datasetBuilder = CdmDatasetCS.builder().copyFrom(orgDataset)

            return datasetBuilder.build()
        }
        return orgDataset
    }

}

fun isHdfEos(ncfile: CdmFile): Boolean {
    if (ncfile.cdmFileTypeId == null || ncfile.cdmFileTypeId != "HDF-EOS2") {
        return false
    }
    val typeName = ncfile.rootGroup.findAttributeString(CF.FEATURE_TYPE, null) ?: return false
    if (typeName != FeatureType.GRID.toString() && typeName != FeatureType.SWATH.toString()) {
        return false
    }
    val version = ncfile.findAttribute("HDFEOSVersion")
    if (version == null || !version.stringValue?.startsWith("HDFEOS")!!) {
        return false
    }
    val feature = ncfile.findAttribute("featureType")
    if (feature == null) {
        return false
    }
    return true
}