package dev.ucdm.dataset.coordsysbuild

import dev.ucdm.core.api.CdmFile
import dev.ucdm.core.constants.*
import dev.ucdm.dataset.api.CdmDataset

// Not needed = but leaving it in case
open class HdfEosConventions(name: String = "HdfEos") : CoordinatesBuilder(name) {
    var isSwath: Boolean? = false
    override fun augment(orgDataset: CdmDataset): CdmDataset {
        val featureType = orgDataset.findAttribute("featureType")
        if (featureType != null && featureType.stringValue == "SWATH") {
            isSwath = true
        }
        return orgDataset
    }

    /* override fun makeCoordinateSystems() {
        val coordsys = CoordinateSystem.builder("swath")
        coordsys.setCoordAxesNames("Latitude Longitude")
        coords.addCoordinateSystem(coordsys)
        info.appendLine("HdfEosConventions added swath CoordinateSystems")
        super.makeCoordinateSystems()
    }

     */
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