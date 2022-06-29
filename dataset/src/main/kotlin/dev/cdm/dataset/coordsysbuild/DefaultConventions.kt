package dev.cdm.dataset.coordsysbuild

import dev.cdm.core.calendar.CalendarDateUnit
import dev.cdm.core.constants.AxisType
import dev.cdm.core.constants.CF
import dev.cdm.core.constants._Coordinate
import dev.cdm.dataset.api.CdmDataset
import dev.cdm.dataset.api.SimpleUnit
import dev.cdm.dataset.api.VariableDS

open class DefaultConventions(dataset: CdmDataset, conventionName : String = "DefaultConventions") :
            CoordSysBuilder(dataset, conventionName) {
    private val lonUnits = arrayOf(
        "degrees_east", "degrees_E", "degreesE", "degree_east", "degree_E", "degreeE",
    )
    private val latUnits = arrayOf(
        "degrees_north", "degrees_N", "degreesN", "degree_north", "degree_N", "degreeN",
    )
    private val vertUnits = arrayOf(
        "level", "layer", "sigma_level",
    )

    override fun identifyAxisType(vds : VariableDS) : AxisType? {
        val unit = vds.unitsString?: return null

        lonUnits.forEach {
            if (unit.equals(it, ignoreCase = true)) {
                return AxisType.Lon
            }
        }
        latUnits.forEach {
            if (unit.equals(it, ignoreCase = true)) {
                return AxisType.Lat
            }
        }
        vertUnits.forEach {
            if (unit.equals(it, ignoreCase = true)) {
                return AxisType.GeoZ
            }
        }

        if (CalendarDateUnit.isDateUnit(unit)) {
            return AxisType.Time
        }
        if (SimpleUnit.pressureUnit.isCompatible(unit)) {
            return AxisType.Pressure
        }
        val positive = vds.findAttributeString(CF.POSITIVE, null)
        if (positive != null) {
            if (SimpleUnit.isCompatible("m", unit)) {
                return AxisType.Height
            } else {
                return AxisType.GeoZ
            }
        }
        return super.identifyAxisType(vds)
    }

}