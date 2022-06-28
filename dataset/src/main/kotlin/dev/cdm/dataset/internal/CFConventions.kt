package dev.cdm.dataset.internal

import dev.cdm.core.calendar.CalendarDateUnit
import dev.cdm.core.constants.AxisType
import dev.cdm.core.constants.CF
import dev.cdm.dataset.api.CdmDatasetCS
import dev.cdm.dataset.api.SimpleUnit
import dev.cdm.dataset.api.VariableDS

class CFConventions(dataset: CdmDatasetCS.Builder<*>) : DefaultConventions(dataset) {
    private val verticalUnits = arrayOf(
        "atmosphere_ln_pressure_coordinate", "atmosphere_sigma_coordinate",
        "atmosphere_hybrid_sigma_pressure_coordinate", "atmosphere_hybrid_height_coordinate",
        "atmosphere_sleve_coordinate", "ocean_sigma_coordinate", "ocean_s_coordinate", "ocean_sigma_z_coordinate",
        "ocean_double_sigma_coordinate", "ocean_s_coordinate_g1",  // -sachin 03/25/09
        "ocean_s_coordinate_g2"
    )

    override fun identifyAxisType(vds : VariableDS.Builder<*>) : AxisType? {
        // standard names for unitless vertical coords
        var sname: String? = vds.attributeContainer.findAttributeString(CF.STANDARD_NAME, null)
        if (sname != null) {
            sname = sname.trim { it <= ' ' }
            verticalUnits.forEach {
                if (sname.equals(it, ignoreCase = true)) {
                    return AxisType.GeoZ
                }
            }
        }

        // standard unit check
        val at: AxisType? = super.identifyAxisType(vds)
        if (at != null) {
            return at
        }

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
            if (sname.equals(CF.PROJECTION_X_COORDINATE, ignoreCase = true) ||
                sname.equals(CF.GRID_LONGITUDE, ignoreCase = true) ||
                sname.equals("rotated_longitude", ignoreCase = true)
            ) {
                return AxisType.GeoX
            }
            if (sname.equals(CF.PROJECTION_Y_COORDINATE, ignoreCase = true) ||
                sname.equals(CF.GRID_LATITUDE, ignoreCase = true) ||
                sname.equals("rotated_latitude", ignoreCase = true)
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
        val unit = vds.getUnits()
        var axis: String? = vds.attributeContainer.findAttributeString(CF.AXIS, null)
        if (axis != null) {
            axis = axis.trim { it <= ' ' }
            val sunit = SimpleUnit.factory(unit)
            if (axis.equals("X", ignoreCase = true) && SimpleUnit.kmUnit.isCompatible(sunit)) {
                    return AxisType.GeoX
            } else if (axis.equals("Y", ignoreCase = true) && SimpleUnit.kmUnit.isCompatible(sunit)) {
                    return AxisType.GeoY
            } else if (axis.equals("Z", ignoreCase = true)) {
                return if (SimpleUnit.kmUnit.isCompatible(sunit)) {
                    AxisType.Height
                } else if (SimpleUnit.pressureUnit.isCompatible(sunit)) {
                    AxisType.Pressure
                } else {
                    AxisType.GeoZ
                }
            }
        }

        // Check time units
        return if (CalendarDateUnit.fromUdunitString(null, unit).isPresent) AxisType.Time else null
    }
}