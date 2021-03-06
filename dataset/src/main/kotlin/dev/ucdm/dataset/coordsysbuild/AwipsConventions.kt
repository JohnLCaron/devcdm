package dev.ucdm.dataset.coordsysbuild

import dev.ucdm.core.api.CdmFile
import dev.ucdm.core.constants.AxisType
import dev.ucdm.dataset.api.CdmDataset
import dev.ucdm.dataset.api.SimpleUnit
import dev.ucdm.dataset.api.VariableDS

open class AwipsConventions(name: String = "AWIPS") : CoordinatesBuilder(name) {

    var augmenter: AwipsAugment? = null
    override fun augment(orgDataset: CdmDataset): CdmDataset {
        augmenter = AwipsAugment(orgDataset, this.info)
        return augmenter!!.augment()
    }

    override fun makeCoordinateTransforms() {
        val projCT = augmenter?.projCT
        if (projCT != null) {
            coords.addCoordinateTransform(projCT)
            coords.setCoordinateTransformFor(projCT.name, listOf("x", "y"));
        }
        super.makeCoordinateTransforms()
    }

    override fun identifyAxisType(vds: VariableDS): AxisType? {
        val vname = vds.shortName
        if (vname.equals("x", ignoreCase = true)) return AxisType.GeoX
        if (vname.equals("lon", ignoreCase = true)) return AxisType.Lon
        if (vname.equals("y", ignoreCase = true)) return AxisType.GeoY
        if (vname.equals("lat", ignoreCase = true)) return AxisType.Lat
        if (vname.equals("record", ignoreCase = true)) return AxisType.Time
        val dimName = vds.getDimension(0)?.shortName
        if (dimName != null && dimName.equals("record", ignoreCase = true)) {
            return AxisType.Time
        }
        val unit = vds.unitsString
        if (unit != null) {
            if (SimpleUnit.pressureUnit.isCompatible(unit)) return AxisType.Pressure
            if (SimpleUnit.pressureUnit.isCompatible(unit)) return AxisType.Pressure
            if (SimpleUnit.kmUnit.isCompatible(unit)) return AxisType.Height
        }
        // dunno
        return super.identifyAxisType(vds)
    }

    override fun identifyZIsPositive(vds: VariableDS): Boolean? {
        val attValue = vds.findAttributeString("positive", null)
        if (null != attValue) {
            return (attValue.equals("up", ignoreCase = true))
        }
        val unit = vds.unitsString
        if (unit != null) {
            if (SimpleUnit.pressureUnit.isCompatible(unit)) return false
            if (SimpleUnit.kmUnit.isCompatible(unit)) return true
        }

        // dunno
        return super.identifyZIsPositive(vds);
    }
}


fun isAwipsConvention(ncfile: CdmFile): Boolean {
    return null != ncfile.findAttribute("projName") &&
            null != ncfile.findDimension("charsPerLevel") &&
            null != ncfile.findDimension("x") &&
            null != ncfile.findDimension("y")
}