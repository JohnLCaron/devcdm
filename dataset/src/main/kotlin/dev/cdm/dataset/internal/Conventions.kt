package dev.cdm.dataset.internal

import dev.cdm.core.calendar.CalendarDateUnit
import dev.cdm.core.constants.AxisType
import dev.cdm.core.constants.CF
import dev.cdm.dataset.api.SimpleUnit
import dev.cdm.dataset.api.VariableDS

interface Conventions {
    fun findAxisType(vds : VariableDS) : AxisType?
}

fun findConvention(name : String?) : Conventions {
    return CFConventions()
}

open class CoardsConventions : Conventions {

    override fun findAxisType(vds : VariableDS) : AxisType? {
        var unit = vds.unitsString
        if (unit == null) {
            return null
        }
        unit = unit.trim()

        if (unit.equals("degrees_east", ignoreCase = true) ||
            unit.equals("degrees_E", ignoreCase = true) ||
            unit.equals("degreesE", ignoreCase = true) ||
            unit.equals("degree_east", ignoreCase = true) ||
            unit.equals("degree_E", ignoreCase = true) ||
            unit.equals("degreeE", ignoreCase = true)
        ) {
            return AxisType.Lon
        }
        if (unit.equals("degrees_north", ignoreCase = true) ||
            unit.equals("degrees_N", ignoreCase = true) ||
            unit.equals("degreesN", ignoreCase = true) ||
            unit.equals("degree_north", ignoreCase = true) ||
            unit.equals("degree_N", ignoreCase = true) ||
            unit.equals("degreeN", ignoreCase = true)
        ) {
            return AxisType.Lat
        }
        if (CalendarDateUnit.isDateUnit(unit)) {
            return AxisType.Time
        }
        // look for other z coordinate
        if (SimpleUnit.isCompatible("mbar", unit)) {
            return AxisType.Pressure
        }
        if (unit.equals("level", ignoreCase = true) ||
            unit.equals("layer", ignoreCase = true) ||
            unit.equals("sigma_level", ignoreCase = true)
        ) {
            return AxisType.GeoZ
        }
        val positive = vds.findAttributeString(CF.POSITIVE, null)
        return if (positive != null) {
            if (SimpleUnit.isCompatible("m", unit)) {
                AxisType.Height
            } else {
                AxisType.GeoZ
            }
        } else null
    }
}

class CFConventions : CoardsConventions() {
    private val vertical_coords = arrayOf(
        "atmosphere_ln_pressure_coordinate", "atmosphere_sigma_coordinate",
        "atmosphere_hybrid_sigma_pressure_coordinate", "atmosphere_hybrid_height_coordinate",
        "atmosphere_sleve_coordinate", "ocean_sigma_coordinate", "ocean_s_coordinate", "ocean_sigma_z_coordinate",
        "ocean_double_sigma_coordinate", "ocean_s_coordinate_g1",  // -sachin 03/25/09
        "ocean_s_coordinate_g2"
    )

    override fun findAxisType(vds : VariableDS) : AxisType? {
        // standard names for unitless vertical coords

        // standard names for unitless vertical coords
        var sname: String = vds.findAttributeString(CF.STANDARD_NAME, null)
        if (sname != null) {
            sname = sname.trim { it <= ' ' }
            for (vertical_coord in vertical_coords) {
                if (sname.equals(vertical_coord, ignoreCase = true)) {
                    return AxisType.GeoZ
                }
            }
        }

        // COARDS - check units

        // COARDS - check units
        val at: AxisType? = super.findAxisType(vds)
        if (at != null) {
            return at
        }

        // standard names for X, Y : bug in CDO putting wrong standard name, so check units first (!)

        // standard names for X, Y : bug in CDO putting wrong standard name, so check units first (!)
        if (sname != null) {
            if (sname.equals(CF.ENSEMBLE, ignoreCase = true)) {
                return AxisType.Ensemble
            }
            if (sname.equals(CF.LATITUDE, ignoreCase = true)) {
                return AxisType.Lat
            }
            if (sname.equals(CF.LONGITUDE, ignoreCase = true)) {
                return AxisType.Lon
            }
            if (sname.equals(CF.PROJECTION_X_COORDINATE, ignoreCase = true) || sname.equals(
                    CF.GRID_LONGITUDE,
                    ignoreCase = true
                )
                || sname.equals("rotated_longitude", ignoreCase = true)
            ) {
                return AxisType.GeoX
            }
            if (sname.equals(CF.PROJECTION_Y_COORDINATE, ignoreCase = true) || sname.equals(
                    CF.GRID_LATITUDE,
                    ignoreCase = true
                )
                || sname.equals("rotated_latitude", ignoreCase = true)
            ) {
                return AxisType.GeoY
            }
            if (sname.equals(CF.TIME_REFERENCE, ignoreCase = true)) {
                return AxisType.RunTime
            }
            if (sname.equals(CF.TIME_OFFSET, ignoreCase = true)) {
                return AxisType.TimeOffset
            }
        }

        // check axis attribute - only for X, Y, Z

        // check axis attribute - only for X, Y, Z
        var axis: String = vds.findAttributeString(CF.AXIS, null)
        if (axis != null) {
            axis = axis.trim { it <= ' ' }
            val unit: String = vds.unitsString?: ""
            if (axis.equals("X", ignoreCase = true)) {
                if (SimpleUnit.isCompatible("m", unit)) {
                    return AxisType.GeoX
                }
            } else if (axis.equals("Y", ignoreCase = true)) {
                if (SimpleUnit.isCompatible("m", unit)) {
                    return AxisType.GeoY
                }
            } else if (axis.equals("Z", ignoreCase = true)) {
                if (unit == null) {
                    return AxisType.GeoZ
                }
                return if (SimpleUnit.isCompatible("m", unit)) {
                    AxisType.Height
                } else if (SimpleUnit.isCompatible("mbar", unit)) {
                    AxisType.Pressure
                } else {
                    AxisType.GeoZ
                }
            }
        }

        // Check time units
        val units: String = vds.unitsString?: ""
        return if (CalendarDateUnit.fromUdunitString(null, units).isPresent) AxisType.Time else null
    }
}