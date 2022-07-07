package dev.cdm.dataset.coordsysbuild

import dev.cdm.core.api.AttributeContainerMutable
import dev.cdm.core.constants.AxisType
import dev.cdm.core.constants.CF
import dev.cdm.dataset.api.*
import dev.cdm.dataset.transform.vertical.WrfEta

class WrfConventions() : CoordinatesBuilder("WrfConventions") {

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

    /** Identify coordinate systems for a variable, using "coordinates" attribute.  */
    override fun identifyCoordinateSystems() {
        // A Variable is made into a Coordinate Axis if listed in a coordinates attribute from any variable
        varList.forEach { vp ->
            val coordinates = vp.vds.findAttributeString(CF.COORDINATES, null)
            if (coordinates != null) {
                vp.setPartialCoordinates(coordinates)
            }
        }
        super.identifyCoordinateSystems()
    }


    override fun makeCoordinateTransforms() {
        val projCT = augmenter?.projCT
        if (projCT != null) {
            coords.addCoordinateTransform(projCT)
        }

        // experimental. it appears we done need any attributes, its all handled in WrfEta
        val vertTransform = CoordinateTransform(WrfEta.WRF_ETA_COORDINATE, AttributeContainerMutable.of(), false)
        coords.addCoordinateTransform(vertTransform)

        super.makeCoordinateTransforms()
    }

    override fun assignCoordinateTransforms() {
        super.assignCoordinateTransforms()

        // any cs with a GeoZ gets assigned WrfEta.WRF_ETA_COORDINATE transform
        coords.coordSys.forEach { cs ->
            val axis = coords.findAxisByType(cs, AxisType.GeoZ)
            if (axis != null) {
                val units = axis.units
                if (units == null || units.trim { it <= ' ' }.isEmpty()) {
                    coords.addTransformTo(WrfEta.WRF_ETA_COORDINATE, cs.name)
                    info.appendLine("Assign coordTransform '${WrfEta.WRF_ETA_COORDINATE}' to CoordSys '${cs.name}'")
                }
            }
        }
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