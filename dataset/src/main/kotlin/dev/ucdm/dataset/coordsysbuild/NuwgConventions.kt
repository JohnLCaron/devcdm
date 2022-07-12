package dev.ucdm.dataset.coordsysbuild

import dev.ucdm.core.constants.AxisType
import dev.ucdm.dataset.api.*

open class NuwgConventions(name: String = "NUWGConventions") : CoordinatesBuilder(name) {

    var nuwg: NuwgAugment? = null
    override fun augment(orgDataset: CdmDataset): CdmDataset {
        nuwg = NuwgAugment(orgDataset, this.info)
        return nuwg!!.augment()
    }

    override fun makeCoordinateTransforms() {
        val projCTV = nuwg!!.getProjectionCT()
        if (projCTV != null) {
            coords.addCoordinateTransform(projCTV)
        }
        super.makeCoordinateTransforms()
    }

    override fun identifyAxisType(vds: VariableDS): AxisType? {
        val vname = vds.shortName
        if (vname.equals("lat", ignoreCase = true)) return AxisType.Lat
        if (vname.equals("lon", ignoreCase = true)) return AxisType.Lon
        if (vname.equals(nuwg!!.xaxisName, ignoreCase = true)) return AxisType.GeoX
        if (vname.equals(nuwg!!.yaxisName, ignoreCase = true)) return AxisType.GeoY
        if (vname.equals("record", ignoreCase = true)) return AxisType.Time
        val dimName = vds.dimensions[0].shortName
        if (dimName != null && dimName.equals("record", ignoreCase = true)) { // wow thats bad!
            return AxisType.Time
        }
        val unit = vds.unitsString
        if (unit != null) {
            if (SimpleUnit.isCompatible("millibar", unit)) return AxisType.Pressure
            if (SimpleUnit.isCompatible("m", unit)) return AxisType.Height
            if (SimpleUnit.isCompatible("sec", unit)) return null
        }
        return AxisType.GeoZ // AxisType.GeoZ;
    }

    override fun identifyZIsPositive(vds: VariableDS): Boolean? {
        // gotta have a length unit
        val unit = vds.unitsString
        if (unit != null) {
            return SimpleUnit.kmUnit.isCompatible(unit)
        }
        return null
    }

}