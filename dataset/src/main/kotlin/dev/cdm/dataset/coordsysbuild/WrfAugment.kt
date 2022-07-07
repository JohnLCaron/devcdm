package dev.cdm.dataset.coordsysbuild

import dev.cdm.array.*
import dev.cdm.array.Array
import dev.cdm.array.Arrays
import dev.cdm.core.api.Attribute
import dev.cdm.core.api.AttributeContainerMutable
import dev.cdm.core.api.Dimension
import dev.cdm.core.api.Variable
import dev.cdm.core.calendar.CalendarDate
import dev.cdm.core.constants.AxisType
import dev.cdm.core.constants.CDM
import dev.cdm.core.constants.CF
import dev.cdm.core.constants._Coordinate
import dev.cdm.core.util.StringUtil2
import dev.cdm.dataset.api.*
import dev.cdm.dataset.geoloc.LatLonPoint
import dev.cdm.dataset.geoloc.Projection
import dev.cdm.dataset.geoloc.projection.FlatEarth
import dev.cdm.dataset.geoloc.projection.LambertConformal
import dev.cdm.dataset.geoloc.projection.Mercator
import dev.cdm.dataset.geoloc.projection.Stereographic
import java.io.IOException
import java.util.*
import java.util.regex.Pattern

class WrfAugment(val dataset: CdmDataset, val info : StringBuilder) {
    val datasetBuilder = CdmDatasetCS.builder().copyFrom(dataset)
    val rootBuilder = datasetBuilder.rootGroup
    val globalAtts : AttributeContainerMutable = rootBuilder.attributeContainer

    val gridE: Boolean
    val projType: Int
    val projCT: CoordinateTransform?
    var isLatLon = false
    var centerX = 0.0
    var centerY = 0.0

    init {
        val type: String? = globalAtts.findAttributeString("GRIDTYPE", null)
        gridE = (type != null) && type.equals("E", ignoreCase = true)

        val lat1: Double = globalAtts.findAttributeDouble("TRUELAT1", Double.NaN)
        val lat2: Double = globalAtts.findAttributeDouble("TRUELAT2", Double.NaN)
        val centralLat: Double = globalAtts.findAttributeDouble("CEN_LAT", Double.NaN)// center of grid
        val centralLon: Double = globalAtts.findAttributeDouble("CEN_LON", Double.NaN) // center of grid
        val standardLon: Double = globalAtts.findAttributeDouble("STAND_LON", Double.NaN) // true longitude
        val standardLat: Double = globalAtts.findAttributeDouble("MOAD_CEN_LAT", Double.NaN)
        var proj: Projection? = null

        projType = globalAtts.findAttributeInteger("MAP_PROJ", -1)
        check(projType != -1) { "WRF must have numeric MAP_PROJ attribute" }
        when (projType) {
            0 -> {
                // for diagnostic runs with no georeferencing
                proj = FlatEarth()
                projCT = CoordinateTransform(CDM.FlatEarth, proj.projectionAttributes, true)
            }
            1 -> {
                // TODO what should be the correct value when STAND_LON and MOAD_CEN_LAT is missing ??
                // TODO Just follow what appears to be correct for Stereographic.
                val lon0 = if (java.lang.Double.isNaN(standardLon)) centralLon else standardLon
                val lat0 = if (java.lang.Double.isNaN(standardLat)) lat2 else standardLat
                proj = LambertConformal(lat0, lon0, lat1, lat2, 0.0, 0.0, 6370.0)
                projCT = CoordinateTransform("Lambert", proj.projectionAttributes, true)
            }
            2 -> {
                // Thanks to Heiko Klein for figuring out WRF Stereographic
                val lon0 = if (java.lang.Double.isNaN(standardLon)) centralLon else standardLon
                val lat0 = if (java.lang.Double.isNaN(centralLat)) lat2 else centralLat // ?? 7/20/2010
                val scaleFactor = (1 + Math.abs(Math.sin(Math.toRadians(lat1)))) / 2.0 // R Schmunk 9/10/07
                proj = Stereographic(lat0, lon0, scaleFactor, 0.0, 0.0, 6370.0)
                projCT = CoordinateTransform("Stereographic", proj.projectionAttributes, true)
            }
            3 -> {
                // thanks to Robert Schmunk with edits for non-MOAD grids
                proj = Mercator(standardLon, lat1, 0.0, 0.0, 6370.0)
                projCT = CoordinateTransform("Mercator", proj.projectionAttributes, true)
            }
            6 -> {
                // version 3 "lat-lon", including global
                // http://www.mmm.ucar.edu/wrf/users/workshops/WS2008/presentations/1-2.pdf
                // use 2D XLAT, XLONG
                isLatLon = true
                projCT = null
            }
            203 -> {
                projCT = null
            }
            else -> {
                info.appendLine("ERROR: unknown projection type = $projType")
                projCT = null
            }
        }

        if (proj != null) {
            val lpt1 = LatLonPoint(centralLat, centralLon) // center of the grid
            val ppt1 = proj.latLonToProj(lpt1)
            centerX = ppt1.x()
            centerY = ppt1.y()
        }
    }

    fun augment(): CdmDataset {
        // fix the #$*&% units
        for (v in rootBuilder.vbuilders) {
            val units = v.attributeContainer.findAttributeString(CDM.UNITS, null)
            if (units != null) {
                (v as VariableDS.Builder<*>).setUnits(normalize(units))
            }
        }

        // make projection transform
        if (projType == 203) {
            val glatOpt = rootBuilder.findVariableLocal("GLAT")
            if (glatOpt.isEmpty()) {
                info.appendLine("Projection type 203 - expected GLAT variable not found")
            } else {
                val glatb = glatOpt.get()
                glatb.addAttribute(Attribute(_Coordinate.AxisType, AxisType.Lat.toString()))
                if (gridE) glatb.addAttribute(Attribute(_Coordinate.Stagger, CDM.ARAKAWA_E))
                glatb.setDimensionsByName("south_north west_east")
                glatb.setSourceData(convertToDegrees(glatb))
                (glatb as VariableDS.Builder<*>).setUnits(CDM.LAT_UNITS)
            }
            val glonOpt = rootBuilder.findVariableLocal("GLON")
            if (glonOpt.isEmpty()) {
                info.appendLine("Projection type 203 - expected GLON variable not found%n")
            } else {
                val glonb = glonOpt.get()
                glonb.addAttribute(Attribute(_Coordinate.AxisType, AxisType.Lon.toString()))
                if (gridE) glonb.addAttribute(Attribute(_Coordinate.Stagger, CDM.ARAKAWA_E))
                glonb.setDimensionsByName("south_north west_east")
                glonb.setSourceData(convertToDegrees(glonb))
                (glonb as VariableDS.Builder<*>).setUnits(CDM.LON_UNITS)
            }

            // Make coordinate system variable
            val v = VariableDS.builder().setName("LatLonCoordSys").setArrayType(ArrayType.CHAR)
            v.addAttribute(Attribute(_Coordinate.Axes, "GLAT GLON Time"))
            val data = Arrays.factory<Byte>(ArrayType.CHAR, intArrayOf(), charArrayOf(' '))
            v.setSourceData(data)
            rootBuilder.addVariable(v)
            rootBuilder.findVariableLocal("LANDMASK").ifPresent { dataVar ->
                dataVar.addAttribute(Attribute(_Coordinate.Systems, "LatLonCoordSys"))
            }
        } else if (projType == 6) {
            rootBuilder.vbuilders.toMutableList().forEach { v ->
                if (v.shortName.startsWith("XLAT")) {
                    val v2 = removeConstantTimeDim(v)
                    v2.addAttribute(Attribute(_Coordinate.AxisType, AxisType.Lat.toString()))
                } else if (v.shortName.startsWith("XLONG")) {
                    val v2 = removeConstantTimeDim(v)
                    v2.addAttribute(Attribute(_Coordinate.AxisType, AxisType.Lon.toString()))
                } /* else if (v.shortName == "T") { // TODO ANOTHER MAJOR KLUDGE to pick up 4D fields
                    v.addAttribute(Attribute(_Coordinate.Axes, "Time XLAT XLONG z"))
                } else if (v.shortName == "U") {
                    v.addAttribute(Attribute(_Coordinate.Axes, "Time XLAT_U XLONG_U z"))
                } else if (v.shortName == "V") {
                    v.addAttribute(Attribute(_Coordinate.Axes, "Time XLAT_V XLONG_V z"))
                } else if (v.shortName == "W") {
                    v.addAttribute(Attribute(_Coordinate.Axes, "Time XLAT XLONG z_stag"))
                } */
            }
        }

        // make axes
        if (!isLatLon) {
            datasetBuilder.replaceCoordinateAxis(rootBuilder, makeXCoordAxis("x", "west_east"))
            datasetBuilder.replaceCoordinateAxis(rootBuilder, makeXCoordAxis("x_stag", "west_east_stag"))
            datasetBuilder.replaceCoordinateAxis(rootBuilder, makeYCoordAxis("y", "south_north"))
            datasetBuilder.replaceCoordinateAxis(rootBuilder, makeYCoordAxis("y_stag", "south_north_stag"))
        }
        datasetBuilder.replaceCoordinateAxis(rootBuilder, makeZCoordAxis("z", "bottom_top"))
        datasetBuilder.replaceCoordinateAxis(rootBuilder, makeZCoordAxis("z_stag", "bottom_top_stag"))
        if (projCT != null) {
            val v: VariableDS.Builder<*> = makeCoordinateTransformVariable(projCT)
            v.addAttribute(Attribute(_Coordinate.AxisTypes, "GeoX GeoY"))
            if (gridE) v.addAttribute(Attribute(_Coordinate.Stagger, CDM.ARAKAWA_E))
            rootBuilder.addVariable(v)
        }

        // time coordinate variations
        val timeVar: Optional<Variable.Builder<*>> = rootBuilder.findVariableLocal("Time")
        if (timeVar.isEmpty) { // Can skip this if its already there, eg from NcML
            var taxis: CoordinateAxis.Builder<*>? = makeTimeCoordAxis("Time", "Time")
            if (taxis == null) taxis = makeTimeCoordAxis("Time", "Times")
            if (taxis != null) datasetBuilder.replaceCoordinateAxis(rootBuilder, taxis)
        }
        datasetBuilder.replaceCoordinateAxis(rootBuilder, makeSoilDepthCoordAxis("ZS"))

        return datasetBuilder.build()
    }

    private fun removeConstantTimeDim(vb: Variable.Builder<*>): VariableDS.Builder<*> {
        val vds = vb as VariableDS.Builder<*>
        val v = vds.orgVar
        val shape = v.shape
        if (v.rank == 3 && shape[0] == 1) { // remove time dependencies - TODO MAJOR KLUDGE
            val view: Variable
            view = try {
                v.slice(0, 0)
            } catch (e: InvalidRangeException) {
                info.appendLine("Cant remove first dimension in variable $v")
                return vds
            }
            // TODO test that this works
            val vbnew = VariableDS.builder().copyFrom(view)
            rootBuilder.replaceVariable(vbnew)
            return vbnew
        }
        return vds
    }

    private fun convertToDegrees(vb: Variable.Builder<*>): Array<*>? {
        val vds = vb as VariableDS.Builder<*>
        val v = vds.orgVar
        var data: Array<Number>
        try {
            data = v.readArray() as Array<Number>
            data = Arrays.reduce(data)
        } catch (ioe: IOException) {
            throw RuntimeException("data read failed on " + v.fullName + "=" + ioe.message)
        }
        val dataInDegrees = DoubleArray(data.size.toInt())
        var count = 0
        for (`val` in data) {
            dataInDegrees[count++] = Math.toDegrees(`val`.toDouble())
        }
        return Arrays.factory<Any>(ArrayType.DOUBLE, intArrayOf(dataInDegrees.size), dataInDegrees)
    }

    // clean up WRF specific units
    private fun normalize(input: String): String {
        var units = input
        when (units) {
            "fraction", "dimensionless", "-", "NA" -> units = ""
            else -> {
                units = units.replace("**", "^")
                units = StringUtil2.remove(units, '}'.code)
                units = StringUtil2.remove(units, '{'.code)
            }
        }
        return units
    }

    private fun makeLonCoordAxis(axisName: String, dim: Dimension?): CoordinateAxis.Builder<*>? {
        if (dim == null) return null
        val dx: Double = 4 * findAttributeDouble("DX")
        val nx = dim.length
        val startx = centerX - dx * (nx - 1) / 2
        val v: CoordinateAxis.Builder<*> =
            CoordinateAxis1D.builder().setName(axisName).setArrayType(ArrayType.DOUBLE)
                .setParentGroupName("").setParentGroupBuilder(rootBuilder)
                .setDimensionsByName(dim.shortName).setUnits("degrees_east")
                .setDesc("synthesized longitude coordinate")
        v.setAutoGen(startx, dx)
        v.setAxisType(AxisType.Lon)
        v.addAttribute(Attribute(_Coordinate.AxisType, "Lon"))
        if (axisName != dim.shortName) v.addAttribute(Attribute(_Coordinate.AliasForDimension, dim.shortName))
        return v
    }

    private fun makeLatCoordAxis(axisName: String, dim: Dimension?): CoordinateAxis.Builder<*>? {
        if (dim == null) return null
        val dy: Double = findAttributeDouble("DY")
        val ny = dim.length
        val starty = centerY - dy * (ny - 1) / 2
        val v: CoordinateAxis.Builder<*> =
            CoordinateAxis1D.builder().setName(axisName).setArrayType(ArrayType.DOUBLE)
                .setParentGroupName("").setParentGroupBuilder(rootBuilder)
                .setDimensionsByName(dim.shortName).setUnits("degrees_north")
                .setDesc("synthesized latitude coordinate")
        v.setAutoGen(starty, dy)
        v.setAxisType(AxisType.Lat)
        v.addAttribute(Attribute(_Coordinate.AxisType, "Lat"))
        if (axisName != dim.shortName) v.addAttribute(Attribute(_Coordinate.AliasForDimension, dim.shortName))
        return v
    }

    private fun makeXCoordAxis(axisName: String, dimName: String): CoordinateAxis.Builder<*>? {
        val dimOpt: Optional<Dimension> = rootBuilder.findDimension(dimName)
        if (dimOpt.isEmpty) {
            return null
        }
        val dim = dimOpt.get()
        val dx: Double = findAttributeDouble("DX") / 1000.0 // km ya just gotta know
        val nx = dim.length
        val startx = centerX - dx * (nx - 1) / 2 // ya just gotta know
        val v: CoordinateAxis.Builder<*> =
            CoordinateAxis1D.builder().setName(axisName).setArrayType(ArrayType.DOUBLE)
                .setParentGroupName("").setParentGroupBuilder(rootBuilder)
                .setDimensionsByName(dim.shortName).setUnits("km")
                .setDesc("synthesized GeoX coordinate from DX attribute")
        v.setAutoGen(startx, dx)
        v.setAxisType(AxisType.GeoX)
        v.addAttribute(Attribute(_Coordinate.AxisType, "GeoX"))
        if (axisName != dim.shortName) v.addAttribute(Attribute(_Coordinate.AliasForDimension, dim.shortName))
        if (gridE) v.addAttribute(Attribute(_Coordinate.Stagger, CDM.ARAKAWA_E))
        return v
    }

    private fun makeYCoordAxis(axisName: String, dimName: String): CoordinateAxis.Builder<*>? {
        val dimOpt: Optional<Dimension> = rootBuilder.findDimension(dimName)
        if (dimOpt.isEmpty) {
            return null
        }
        val dim = dimOpt.get()
        val dy: Double = findAttributeDouble("DY") / 1000.0
        val ny = dim.length
        val starty = centerY - dy * (ny - 1) / 2 // - dy/2; // ya just gotta know
        val v: CoordinateAxis.Builder<*> =
            CoordinateAxis1D.builder().setName(axisName).setArrayType(ArrayType.DOUBLE)
                .setParentGroupName("").setParentGroupBuilder(rootBuilder)
                .setDimensionsByName(dim.shortName).setUnits("km")
                .setDesc("synthesized GeoY coordinate from DY attribute")
        v.setAxisType(AxisType.GeoY)
        v.addAttribute(Attribute(_Coordinate.AxisType, "GeoY"))
        v.setAutoGen(starty, dy)
        if (axisName != dim.shortName) v.addAttribute(Attribute(_Coordinate.AliasForDimension, dim.shortName))
        if (gridE) v.addAttribute(Attribute(_Coordinate.Stagger, CDM.ARAKAWA_E))
        return v
    }

    private fun makeZCoordAxis(axisName: String, dimName: String): CoordinateAxis.Builder<*>? {
        val dimOpt: Optional<Dimension> = rootBuilder.findDimension(dimName)
        if (dimOpt.isEmpty) {
            return null
        }
        val dim = dimOpt.get()
        val fromWhere = if (axisName.endsWith("stag")) "ZNW" else "ZNU"
        val v: CoordinateAxis.Builder<*> =
            CoordinateAxis1D.builder().setName(axisName).setArrayType(ArrayType.DOUBLE)
                .setParentGroupName("").setParentGroupBuilder(rootBuilder)
                .setDimensionsByName(dim.shortName).setUnits("").setDesc("eta values from variable $fromWhere")
        v.addAttribute(Attribute(CF.POSITIVE, CF.POSITIVE_DOWN)) // eta coordinate is 1.0 at bottom, 0 at top
        v.setAxisType(AxisType.GeoZ)
        v.addAttribute(Attribute(_Coordinate.AxisType, "GeoZ"))
        if (axisName != dim.shortName) {
            v.addAttribute(Attribute(_Coordinate.AliasForDimension, dim.shortName))
        }

        // create eta values from file variables: ZNU, ZNW
        // But they are a function of time though the values are the same in the sample file
        // NOTE: Use first time sample assuming all are the same!!
        val etaVarOpt: Optional<Variable.Builder<*>> = rootBuilder.findVariableLocal(fromWhere)
        return if (etaVarOpt.isEmpty) {
            makeFakeCoordAxis(axisName, dim)
        } else {
            val etaVarDS = etaVarOpt.get() as VariableDS.Builder<*>
            val etaVar = etaVarDS.orgVar
            val n = etaVar.getShape(1) // number of eta levels
            val origin = intArrayOf(0, 0)
            val shape = intArrayOf(1, n)
            try {
                val array = etaVar.readArray(Section(origin, shape)) as Array<Number> // read first time slice
                val newArray = DoubleArray(n)
                var count = 0
                for (`val` in array) {
                    newArray[count++] = `val`.toDouble()
                }
                v.setSourceData(Arrays.factory<Any>(ArrayType.DOUBLE, intArrayOf(n), newArray))
            } catch (e: Exception) {
                e.printStackTrace()
            } // ADD: error?
            v
        }
    }

    private fun makeFakeCoordAxis(axisName: String, dim: Dimension?): CoordinateAxis.Builder<*>? {
        if (dim == null) return null
        val v: CoordinateAxis.Builder<*> =
            CoordinateAxis1D.builder().setName(axisName).setArrayType(ArrayType.SHORT)
                .setParentGroupName("").setParentGroupBuilder(rootBuilder)
                .setDimensionsByName(dim.shortName).setUnits("").setDesc("synthesized coordinate: only an index")
        v.setAxisType(AxisType.GeoZ)
        v.addAttribute(Attribute(_Coordinate.AxisType, "GeoZ"))
        if (axisName != dim.shortName) v.addAttribute(Attribute(_Coordinate.AliasForDimension, dim.shortName))
        v.setAutoGen(0.0, 1.0)
        return v
    }

    private fun makeTimeCoordAxis(axisName: String, dimName: String): CoordinateAxis.Builder<*>? {
        val dimOpt: Optional<Dimension> = rootBuilder.findDimension(dimName)
        if (dimOpt.isEmpty) {
            return null
        }
        val dim = dimOpt.get()
        val nt = dim.length
        val timeOpt: Optional<Variable.Builder<*>> = rootBuilder.findVariableLocal("Times")
        if (timeOpt.isEmpty) {
            return null
        }
        val timeV = (timeOpt.get() as VariableDS.Builder<*>).orgVar
        val timeData: Array<*>
        timeData = try {
            timeV.readArray()
        } catch (ioe: IOException) {
            return null
        }
        val values = DoubleArray(nt)
        var count = 0
        if (timeData.arrayType == ArrayType.CHAR) {
            val stringArray = Arrays.makeStringsFromChar(timeData as Array<Byte?>)
            val testTimeStr = stringArray[0]
            val isCanonicalIsoStr: Boolean
            // Maybe too specific to require WRF to give 10 digits or dashes for the date (e.g. yyyy-mm-dd)?
            val wrfDateWithUnderscore = "([\\-\\d]{10})_"
            val wrfDateWithUnderscorePattern = Pattern.compile(wrfDateWithUnderscore)
            val m = wrfDateWithUnderscorePattern.matcher(testTimeStr)
            isCanonicalIsoStr = m.matches()
            for (dateS in stringArray) {
                var cd: Optional<CalendarDate>
                cd = if (isCanonicalIsoStr) {
                    CalendarDate.fromUdunitIsoDate(null, dateS)
                } else {
                    CalendarDate.fromUdunitIsoDate(null, dateS.replaceFirst("_".toRegex(), "T"))
                }
                if (cd.isPresent) {
                    values[count++] = cd.get().millisFromEpoch.toDouble() / 1000
                } else {
                    info.appendLine("ERROR: cant parse Time string = '${dateS}'")

                    // one more try
                    val startAtt = rootBuilder.getAttributeContainer().findAttributeString("START_DATE", null)
                    if (nt == 1 && null != startAtt) {
                        try {
                            cd = CalendarDate.fromUdunitIsoDate(null, startAtt)
                            if (cd.isPresent) {
                                values[0] = cd.get().millisFromEpoch.toDouble() / 1000
                            } else {
                                info.appendLine("ERROR: cant parse global attribute START_DATE = '${startAtt}'")
                                return null
                            }
                        } catch (e2: Exception) {
                            info.appendLine("ERROR: cant parse global attribute START_DATE = '${startAtt}' err='${e2.message}'")
                        }
                    }
                }
            }
        } else if (timeData.arrayType == ArrayType.STRING) {
            for (dateS in timeData as Array<String>) {
                try {
                    val cd = CalendarDate.fromUdunitIsoDate(null, dateS).orElseThrow()
                    values[count++] = cd.millisFromEpoch.toDouble() / 1000
                } catch (e: IllegalArgumentException) {
                    info.appendLine("ERROR: cant parse Time string '${dateS}'")
                    return null
                }
            }
        } else {
            info.appendLine("ERROR: timeData must be CHAR or String = ${timeData.arrayType}")
            return null
        }
        val v: CoordinateAxis.Builder<*> =
            CoordinateAxis1D.builder().setName(axisName).setArrayType(ArrayType.DOUBLE)
                .setParentGroupName("").setParentGroupBuilder(rootBuilder)
                .setDimensionsByName(dim.shortName)
                .setUnits("secs since 1970-01-01 00:00:00").setDesc("synthesized time coordinate from Times(time)")
        v.setAxisType(AxisType.Time)
        v.addAttribute(Attribute(_Coordinate.AxisType, "Time"))
        if (axisName != dim.shortName) v.addAttribute(Attribute(_Coordinate.AliasForDimension, dim.shortName))
        v.setSourceData(Arrays.factory<Any>(ArrayType.DOUBLE, intArrayOf(values.size), values))
        return v
    }

    private fun makeSoilDepthCoordAxis(coordVarName: String): CoordinateAxis.Builder<*>? {
        val referencedVar = dataset.findVariable(coordVarName)
        if (referencedVar == null) {
            return null
        }
        val referencedVarDS = referencedVar as VariableDS
        val coordVar = referencedVarDS.originalVariable // why do we need the original?
        var soilDim: Dimension? = null
        val dims = coordVar!!.dimensions
        for (d in dims) {
            if (d.shortName.startsWith("soil_layers")) soilDim = d
        }
        if (null == soilDim) return null

        // One dimensional case, can convert existing variable
        if (coordVar.rank == 1) {
            val vbuilder = CoordinateAxis.fromVariableDS(referencedVar)
            vbuilder.addAttribute(Attribute(CF.POSITIVE, CF.POSITIVE_DOWN))
            // soil depth gets larger as you go down
            vbuilder.addAttribute(Attribute(_Coordinate.AxisType, "GeoZ"))
            if (coordVarName != soilDim.shortName) {
                vbuilder.addAttribute(Attribute(_Coordinate.AliasForDimension, soilDim.shortName))
            }
            return vbuilder
        }

        // otherwise synthesize a new one
        val units = coordVar.attributes().findAttributeString(CDM.UNITS, "")
        val vbuilder: CoordinateAxis.Builder<*> =
            CoordinateAxis1D.builder().setName("soilDepth").setArrayType(ArrayType.DOUBLE)
                .setParentGroupName("").setParentGroupBuilder(rootBuilder)
                .setDimensionsByName(soilDim.shortName).setUnits(units).setDesc("soil depth")
        vbuilder.addAttribute(Attribute(CF.POSITIVE, CF.POSITIVE_DOWN)) // soil depth gets larger as you go down
        vbuilder.setAxisType(AxisType.GeoZ)
        vbuilder.addAttribute(Attribute(_Coordinate.AxisType, "GeoZ"))
        vbuilder.setUnits(CDM.UNITS)
        if (vbuilder.shortName != soilDim.shortName) {
            vbuilder.addAttribute(Attribute(_Coordinate.AliasForDimension, soilDim.shortName))
        }

        // read first time slice
        val n = coordVar.getShape(1)
        val origin = intArrayOf(0, 0)
        val shape = intArrayOf(1, n)
        try {
            val array = coordVar.readArray(Section(origin, shape)) as Array<Number>
            val newArray = DoubleArray(n)
            var count = 0
            for (`val` in array) {
                newArray[count++] = `val`.toDouble()
            }
            vbuilder.setSourceData(Arrays.factory<Any>(ArrayType.DOUBLE, intArrayOf(n), newArray))
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
        return vbuilder
    }

    private fun findAttributeDouble(attname: String): Double {
        return globalAtts.findAttributeDouble(attname, Double.NaN)
    }
}
