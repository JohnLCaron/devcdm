package dev.ucdm.dataset.coordsysbuild

import dev.ucdm.core.calendar.CalendarDateUnit
import dev.ucdm.core.constants.AxisType
import dev.ucdm.core.constants.CF
import dev.ucdm.dataset.api.SimpleUnit
import dev.ucdm.dataset.api.VariableDS

open class DefaultConventions(name: String = "DefaultConventions") : CoordinatesBuilder(name) {

    override fun identifyAxisType(vds: VariableDS): AxisType? {
        defaultAxisType(vds)?.let { return it }
        return super.identifyAxisType(vds)
    }
}

private val lonUnits = arrayOf(
    "degrees_east", "degrees_E", "degreesE", "degree_east", "degree_E", "degreeE", "degrees E"
)
private val latUnits = arrayOf(
    "degrees_north", "degrees_N", "degreesN", "degree_north", "degree_N", "degreeN", "degrees N"
)
private val vertUnits = arrayOf(
    "level", "layer", "sigma_level",
)

fun defaultAxisType(vds: VariableDS): AxisType? {
    val unit = vds.unitsString ?: return null

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
        return if (SimpleUnit.isCompatible("m", unit)) {
            AxisType.Height
        } else {
            AxisType.GeoZ
        }
    }
    return null
}

fun desperateAxisType(vb: VariableDS): AxisType? {
    defaultAxisType(vb)?.let { return it }

    val vname = vb.shortName ?: return null
    var unit = vb.unitsString
    if (unit == null) {
        unit = ""
    }
    var desc = vb.description
    if (desc == null) {
        desc = ""
    }
    if (vname.equals("x", ignoreCase = true)) {
        return AxisType.GeoX
    }
    if (vname.equals("lon", ignoreCase = true) || vname.equals("longitude", ignoreCase = true)) {
        return AxisType.Lon
    }
    if (vname.equals("y", ignoreCase = true)) {
        return AxisType.GeoY
    }
    if (vname.equals("lat", ignoreCase = true) || vname.equals("latitude", ignoreCase = true)) {
        return AxisType.Lat
    }
    if (vname.equals("lev", true) || vname.equals("level", true) || vname.equals("zlev", true)) {
        return AxisType.GeoZ
    }
    if (vname.equals("z", ignoreCase = true) || vname.equals("altitude", ignoreCase = true) ||
        desc.contains("altitude") || vname.equals("depth", ignoreCase = true) ||
        vname.equals("elev", ignoreCase = true) || vname.equals("elevation", ignoreCase = true)
    ) {
        if (SimpleUnit.kmUnit.isCompatible(unit)) { // has units compatible with km
            return AxisType.Height
        }
    }
    if (vname.contains("time", ignoreCase = true)) {
        if (CalendarDateUnit.isDateUnit(unit)) {
            return AxisType.Time
        }
        // return AxisType.Time // kludge: see aggSynGrid.xml example test
    }

    return null
}