package dev.cdm.dataset.internal

import dev.cdm.core.calendar.CalendarDateUnit
import dev.cdm.core.constants.AxisType
import dev.cdm.core.constants.CF
import dev.cdm.dataset.api.CdmDatasetCS
import dev.cdm.dataset.api.SimpleUnit
import dev.cdm.dataset.api.VariableDS

open class DefaultConventions(dataset: CdmDatasetCS.Builder<*>) : CoordSysBuilder(dataset) {
    private val lonUnits = arrayOf(
        "degrees_east", "degrees_E", "degreesE", "degree_east", "degree_E", "degreeE",
    )
    private val latUnits = arrayOf(
        "degrees_north", "degrees_N", "degreesN", "degree_north", "degree_N", "degreeN",
    )
    private val vertUnits = arrayOf(
        "level", "layer", "sigma_level",
    )

    override fun identifyAxisType(vds : VariableDS.Builder<*>) : AxisType? {
        val unit = vds.getUnits() ?: return null

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
        val positive = vds.attributeContainer.findAttributeString(CF.POSITIVE, null)
        return if (positive != null) {
            if (SimpleUnit.isCompatible("m", unit)) {
                AxisType.Height
            } else {
                AxisType.GeoZ
            }
        } else null
    }

}