package dev.cdm.dataset.coordsysbuild

import dev.cdm.array.ArrayType
import dev.cdm.core.api.Attribute
import dev.cdm.core.api.AttributeContainerMutable
import dev.cdm.core.api.CdmFile
import dev.cdm.core.api.Group
import dev.cdm.core.constants.*
import dev.cdm.dataset.api.CdmDataset
import dev.cdm.dataset.api.CdmDatasetCS
import dev.cdm.dataset.api.CoordinateAxis
import dev.cdm.dataset.api.CoordinateAxis1D
import java.util.*

open class HdfEosOmiConventions(name: String = "HdfEos-Aura-Omi") : CoordSysBuilder(name) {

    override fun augment(orgDataset: CdmDataset): CdmDataset {
        val nestedAtt = "/HDFEOS/ADDITIONAL/FILE_ATTRIBUTES/@ProcessLevel"
        val processLevel = orgDataset.findAttribute(nestedAtt)
        if (processLevel == null || processLevel.stringValue!!.startsWith("2")) {
            this.info.appendLine("*** HdfEosOmiConventions expected to find attribute $nestedAtt with value 3")
            return orgDataset
        }

        val datasetBuilder = CdmDatasetCS.builder().copyFrom(orgDataset)
        val rootBuilder = datasetBuilder.rootGroup
        val globalAtts : AttributeContainerMutable = rootBuilder.attributeContainer

        val gridso: Optional<Group.Builder> = rootBuilder.findGroupNested("/HDFEOS/GRIDS")
        gridso.ifPresent { grids: Group.Builder ->
            for (g2 in grids.gbuilders) {
                val gctp = g2.attributeContainer.findAttribute("GCTPProjectionCode")
                if (gctp == null || gctp.numericValue == null || gctp.numericValue != 0) {
                    continue
                }
                val nlon = g2.attributeContainer.findAttribute("NumberOfLongitudesInGrid")
                val nlat = g2.attributeContainer.findAttribute("NumberOfLatitudesInGrid")
                if (nlon == null || nlon.numericValue == null || nlat == null || nlat.numericValue == null) {
                    continue
                }
                datasetBuilder.replaceCoordinateAxis(g2, makeLonCoordAxis(g2, nlon.numericValue!!.toInt(), "XDim"))
                datasetBuilder.replaceCoordinateAxis(g2, makeLatCoordAxis(g2, nlat.numericValue!!.toInt(), "YDim"))
                for (g3 in g2.gbuilders) {
                    for (vb in g3.vbuilders) {
                        vb.addAttribute(Attribute(_Coordinate.Axes, "lat lon"))
                    }
                }
            }
        }
        return datasetBuilder.build()
    }

    private fun makeLatCoordAxis(g2: Group.Builder, n: Int, dimName: String): CoordinateAxis.Builder<*>? {
        val v: CoordinateAxis.Builder<*> = CoordinateAxis1D.builder().setName("lat").setArrayType(ArrayType.FLOAT)
            .setParentGroupBuilder(g2).setDimensionsByName(dimName).setUnits(CDM.LAT_UNITS).setDesc("latitude")
        val incr = 180.0 / n
        v.setAutoGen(-90.0 + 0.5 * incr, incr)
        v.addAttribute(Attribute(_Coordinate.AxisType, AxisType.Lat.toString()))
        return v
    }

    private fun makeLonCoordAxis(g2: Group.Builder, n: Int, dimName: String): CoordinateAxis.Builder<*>? {
        val v: CoordinateAxis.Builder<*> = CoordinateAxis1D.builder().setName("lon").setArrayType(ArrayType.FLOAT)
            .setParentGroupBuilder(g2).setDimensionsByName(dimName).setUnits(CDM.LON_UNITS).setDesc("longitude")
        val incr = 360.0 / n
        v.setAutoGen(-180.0 + 0.5 * incr, incr)
        v.addAttribute(Attribute(_Coordinate.AxisType, AxisType.Lon.toString()))
        return v
    }
}

fun isHdfEosOmi(ncfile: CdmFile): Boolean {
    if (ncfile.cdmFileTypeId == null || ncfile.cdmFileTypeId != "HDF-EOS2") {
        return false
    }
    val typeName = ncfile.rootGroup.findAttributeString(CF.FEATURE_TYPE, null) ?: return false
    if (typeName != FeatureType.GRID.toString() && typeName != FeatureType.SWATH.toString()) {
        return false
    }
    val instName = ncfile.findAttribute("/HDFEOS/ADDITIONAL/FILE_ATTRIBUTES/@InstrumentName")
    if (instName == null || instName.stringValue != "OMI") {
        return false
    }
    val level = ncfile.findAttribute("/HDFEOS/ADDITIONAL/FILE_ATTRIBUTES/@ProcessLevel")
    return if (level == null || level.stringValue == null) {
        false
    } else (level.stringValue!!.startsWith("2") || level.stringValue!!.startsWith("3"))
}