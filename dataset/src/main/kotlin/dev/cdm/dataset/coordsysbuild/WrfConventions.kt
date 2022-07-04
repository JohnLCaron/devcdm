package dev.cdm.dataset.coordsysbuild

import dev.cdm.core.api.Attribute
import dev.cdm.core.constants.AxisType
import dev.cdm.core.constants.CDM
import dev.cdm.core.constants._Coordinate
import dev.cdm.dataset.api.*
import dev.cdm.dataset.transform.vertical.WrfEta

class WrfConventions() : CoordSysBuilder("WrfConventions") {

    var augmenter: WrfAugment? = null
    override fun augment(orgDataset: CdmDataset): CdmDataset {
        augmenter = WrfAugment(orgDataset, this.info)
        return augmenter!!.augment()
    }

    override fun identifyAxisType(vds: VariableDS): AxisType? {
        val vname: String = vds.shortName

        if (vname.equals("x", ignoreCase = true) || vname.equals("x_stag", ignoreCase = true)) return AxisType.GeoX

        if (vname.equals("lon", ignoreCase = true)) return AxisType.Lon

        if (vname.equals("y", ignoreCase = true) || vname.equals("y_stag", ignoreCase = true)) return AxisType.GeoY

        if (vname.equals("lat", ignoreCase = true)) return AxisType.Lat

        if (vname.equals("z", ignoreCase = true) || vname.equals("z_stag", ignoreCase = true)) return AxisType.GeoZ

        if (vname.equals("Z", ignoreCase = true)) return AxisType.Height

        if (vname.equals("time", ignoreCase = true) || vname.equals(
                "times",
                ignoreCase = true
            )
        ) return AxisType.Time

        val unit = vds.unitsString
        if (unit != null) {
            if (SimpleUnit.pressureUnit.isCompatible(unit)) return AxisType.Pressure
            if (SimpleUnit.kmUnit.isCompatible(unit)) return AxisType.Height
        }
        return null
    }

    override fun assignCoordinateTransforms() {
        super.assignCoordinateTransforms()

        // public Optional<CoordinateAxis.Builder> findZAxis(CoordinateSystem.Builder csys) {
        // any cs with a vertical coordinate with no units gets one
        coords.coordSys.forEach { cs ->
            val axis = coords.findAxisByType(cs, AxisType.GeoZ)
            if (axis != null) {
                val units = axis.units
                if (units == null || units.trim { it <= ' ' }.isEmpty()) {
                    info.appendLine("Added WRF_ETA_COORDINATE to '${cs.coordAxesNames}'")
                    axis.addAttribute(Attribute(_Coordinate.TransformType, "Vertical"))
                    axis.addAttribute(Attribute(CDM.TRANSFORM_NAME, WrfEta.WRF_ETA_COORDINATE))
                }
            }
        }
    }

    override fun makeCoordinateTransforms() {
        val projCT = augmenter?.projCT
        if (projCT != null) {
            val vp = findVarProcess(projCT.getName(), null)
            if (vp != null) {
                vp.isCoordinateTransform = true
                vp.ctv = projCT
            }
        }
        super.makeCoordinateTransforms()
    }
}


fun isWrfConventions(ncfile: CdmDataset): Boolean {
    if (null == ncfile.findDimension("south_north")) return false

    // ARW only
    val dynOpt = ncfile.rootGroup.attributes().findAttributeInteger("DYN_OPT", -1)
    if (dynOpt != -1 && dynOpt != 2) { // if it exists, it must equal 2.
        return false
    }
    // ig gridType exixts, must be C or E
    val gridType = ncfile.rootGroup.findAttributeString("GRIDTYPE", null)
    if (gridType != null && !gridType.equals("C", ignoreCase = true) && !gridType.equals("E", ignoreCase = true)) {
        return false
    }
    return ncfile.findAttribute("MAP_PROJ") != null
}