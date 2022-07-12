package dev.ucdm.dataset.coordsysbuild

import dev.ucdm.core.api.CdmFile
import dev.ucdm.core.constants.AxisType
import dev.ucdm.core.constants.CDM
import dev.ucdm.dataset.api.*

open class AwipsSatConventions(name: String = "AWIPS-Sat") : AwipsConventions(name) {

    override fun augment(orgDataset: CdmDataset): CdmDataset {
        augmenter = AwipsSatAugment(orgDataset, this.info)
        return augmenter!!.augment()
    }

    override fun identifyAxisType(vds: VariableDS) : AxisType? {
        val units = vds.unitsString
        if (units != null) {
            if (units.equals(CDM.LON_UNITS, ignoreCase = true)) return AxisType.Lon
            if (units.equals(CDM.LAT_UNITS, ignoreCase = true)) return AxisType.Lat
        }
        return super.identifyAxisType(vds)
    }

}

fun isAwipsSatConvention(ncfile: CdmFile): Boolean {
    return null != ncfile.findAttribute("projName") &&
            null != ncfile.findAttribute("lon00") &&
            null != ncfile.findAttribute("lat00") &&
            null != ncfile.findAttribute("lonNxNy") &&
            null != ncfile.findAttribute("latNxNy") &&
            null != ncfile.findAttribute("centralLon") &&
            null != ncfile.findAttribute("centralLat") &&
            null != ncfile.findDimension("x") &&
            null != ncfile.findDimension("y") &&
            null != ncfile.findVariable("image")
}