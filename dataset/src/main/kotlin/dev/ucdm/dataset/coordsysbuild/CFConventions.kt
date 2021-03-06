package dev.ucdm.dataset.coordsysbuild

import dev.ucdm.core.api.Attribute
import dev.ucdm.core.calendar.Calendar
import dev.ucdm.core.calendar.CalendarDateUnit
import dev.ucdm.core.constants.AxisType
import dev.ucdm.core.constants.CF
import dev.ucdm.dataset.api.CoordinateAxis
import dev.ucdm.dataset.api.SimpleUnit
import dev.ucdm.dataset.api.VariableDS

open class CFConventions(name: String = "CFConventions") : DefaultConventions(name) {
    private val verticalUnits = arrayOf(
        "atmosphere_ln_pressure_coordinate", "atmosphere_sigma_coordinate",
        "atmosphere_hybrid_sigma_pressure_coordinate", "atmosphere_hybrid_height_coordinate",
        "atmosphere_sleve_coordinate", "ocean_sigma_coordinate", "ocean_s_coordinate", "ocean_sigma_z_coordinate",
        "ocean_double_sigma_coordinate", "ocean_s_coordinate_g1",  // -sachin 03/25/09
        "ocean_s_coordinate_g2"
    )

    override fun identifyAxisType(vds: VariableDS): AxisType? {
        // standard names for unitless vertical coords
        var stdName: String? = vds.findAttributeString(CF.STANDARD_NAME, null)
        if (stdName != null) {
            stdName = stdName.trim { it <= ' ' }
            verticalUnits.forEach {
                if (stdName.equals(it, ignoreCase = true)) {
                    return AxisType.GeoZ
                }
            }
        }

        // standard names for X, Y : bug in CDO putting wrong standard name, so check units first (!)
        if (stdName != null) {
            if (stdName.equals(CF.ENSEMBLE, ignoreCase = true)) {
                return AxisType.Ensemble
            }
            if (stdName.equals(CF.LATITUDE, ignoreCase = true)) {
                return AxisType.Lat
            }
            if (stdName.equals(CF.LONGITUDE, ignoreCase = true)) {
                return AxisType.Lon
            }
            if (stdName.equals(CF.PROJECTION_X_COORDINATE, ignoreCase = true) ||
                stdName.equals(CF.GRID_LONGITUDE, ignoreCase = true) ||
                stdName.equals("rotated_longitude", ignoreCase = true)
            ) {
                return AxisType.GeoX
            }
            if (stdName.equals(CF.PROJECTION_Y_COORDINATE, ignoreCase = true) ||
                stdName.equals(CF.GRID_LATITUDE, ignoreCase = true) ||
                stdName.equals("rotated_latitude", ignoreCase = true)
            ) {
                return AxisType.GeoY
            }
            if (stdName.equals(CF.TIME_REFERENCE, ignoreCase = true)) {
                return AxisType.RunTime
            }
            if (stdName.equals(CF.TIME_OFFSET, ignoreCase = true)) {
                return AxisType.TimeOffset
            }
        }

        // check axis attribute - only for X, Y, Z
        val unit = vds.unitsString
        var axis: String? = vds.findAttributeString(CF.AXIS, null)
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


        // standard unit check
        val at: AxisType? = super.identifyAxisType(vds)
        if (at != null) {
            return at
        }

        // Check time units
        if (CalendarDateUnit.fromUdunitString(null, unit).isPresent) {
            return AxisType.Time
        }
        return super.identifyAxisType(vds)
    }

    override fun identifyZIsPositive(vds: VariableDS): Boolean? {
        val vertCoordUnits = vds.unitsString
        if (vertCoordUnits == null) {
            return true
        }
        if (vertCoordUnits.isEmpty()) {
            return true
        }
        if (SimpleUnit.isCompatible("millibar", vertCoordUnits)) {
            return false
        }
        if (SimpleUnit.isCompatible("m", vertCoordUnits)) {
            return true
        }

        return super.identifyZIsPositive(vds)
    }

    /** Identify coordinate systems for a variable, using "coordinates" attribute.  */
    override fun identifyCoordinateAxes() {
        // A Variable is made into a Coordinate Axis if listed in a coordinates attribute from any variable
        varList.forEach { vp ->
            val coordinates = vp.vds.findAttributeString(CF.COORDINATES, null)
            if (coordinates != null) {
                vp.setPartialCoordinates(coordinates)
            }
        }
        super.identifyCoordinateAxes()
    }

    override fun identifyCoordinateTransforms() {
        // look for vertical transforms
        varList.forEach { vp ->
            val stdName = vp.vds.findAttributeString(CF.STANDARD_NAME, null)
            verticalUnits.forEach {
                if (stdName.equals(it, ignoreCase = true)) {
                    info.appendLine("Identify CoordinateTransform '${vp}'")
                    vp.setIsCoordinateTransform("from ${CF.STANDARD_NAME}")
                }
            }
        }

        // look for horizontal transforms LOOK could check if they are known
        // thjis tells us that this variable's coordinate system has this projection
        varList.forEach { vp ->
            val gridMapping = vp.vds.findAttributeString(CF.GRID_MAPPING, null)
            if (gridMapping != null) {
                val gridMapVar = vp.group.findVariableOrInParent(gridMapping)
                if (gridMapVar == null) {
                    info.appendLine("***Cant find gridMapping variable '${gridMapping}' referenced by variable '$vp'")
                } else {
                    // TODO might be group relative - CF does not specify
                    val gridVp = findVarProcess(gridMapping, vp)
                    if (gridVp == null) {
                        info.appendLine("***Cant find gridMapping '${gridMapping}' referenced by variable '$vp'")
                    } else {
                        gridVp.setIsCoordinateTransform("from ${vp} to ${gridVp}")
                        vp.gridMapping = gridMapping
                    }
                }
            }
        }
        super.identifyCoordinateTransforms()
    }

    override fun makeCoordinateAxes() {
        super.makeCoordinateAxes();

        coords.coordAxes.filter { it.axisType?.isTime ?: false }.forEach { checkTimeVarForCalendar(it) }
    }

    // add gregorian calendar attribute to any time coordinate.
    // CF mandates gregorian as default, CDM uses proleptic_gregorian as default.
    // No one would use gregorian is they actually understood it.
    open fun checkTimeVarForCalendar(vb: CoordinateAxis.Builder<*>) {
        var calAttr = vb.attributeContainer.findAttributeIgnoreCase(CF.CALENDAR)
        if (calAttr == null) {
            calAttr = Attribute(CF.CALENDAR, Calendar.gregorian.toString())
            vb.addAttribute(calAttr)
        }
    }
}