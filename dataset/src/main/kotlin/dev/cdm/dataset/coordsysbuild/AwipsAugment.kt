package dev.cdm.dataset.coordsysbuild

import com.google.common.collect.ImmutableList
import dev.cdm.array.*
import dev.cdm.array.Array
import dev.cdm.array.Arrays
import dev.cdm.core.api.Attribute
import dev.cdm.core.api.AttributeContainerMutable
import dev.cdm.core.api.Dimension
import dev.cdm.core.api.Variable
import dev.cdm.core.constants.AxisType
import dev.cdm.core.constants.CDM
import dev.cdm.core.constants._Coordinate
import dev.cdm.core.iosp.IospUtils
import dev.cdm.core.util.StringUtil2
import dev.cdm.dataset.api.*
import dev.cdm.dataset.geoloc.LatLonPoint
import dev.cdm.dataset.geoloc.projection.LambertConformal
import dev.cdm.dataset.geoloc.projection.Stereographic
import dev.cdm.dataset.transform.horiz.ProjectionCTV
import java.io.IOException
import java.util.*

val debugBreakup = false
val debugProj = false

open class AwipsAugment(val orgDataset: CdmDataset, val info : StringBuilder) {
    val datasetBuilder = CdmDatasetCS.builder().copyFrom(orgDataset)
    val rootBuilder = datasetBuilder.rootGroup
    val globalAtts : AttributeContainerMutable = rootBuilder.attributeContainer
    
    var projCT: ProjectionCTV? = null
    var startx = 0.0
    var starty  = 0.0
    var dx = 0.0
    var dy = 0.0

    open fun augment(): CdmDataset {
        val nx: Int = rootBuilder.findDimension("x").map {it.length}
            .orElseThrow { RuntimeException("missing dimension x") }
        val ny: Int = rootBuilder.findDimension("y").map {it.length}
            .orElseThrow { RuntimeException("missing dimension y") }
        val projName = rootBuilder.getAttributeContainer().findAttributeString("projName", "none")!!
        if (projName.equals("LATLON", ignoreCase = true)) {
            datasetBuilder.replaceCoordinateAxis(rootBuilder, makeLonCoordAxis(nx, "x"))
            datasetBuilder.replaceCoordinateAxis(rootBuilder, makeLatCoordAxis(ny, "y"))
        } else if (projName.equals("LAMBERT_CONFORMAL", ignoreCase = true)) {
            projCT = makeLCProjection(projName)
            datasetBuilder.replaceCoordinateAxis(rootBuilder, makeXCoordAxis("x"))
            datasetBuilder.replaceCoordinateAxis(rootBuilder, makeYCoordAxis("y"))
        } else if (projName.equals("STEREOGRAPHIC", ignoreCase = true)) {
            projCT = makeStereoProjection(projName)
            datasetBuilder.replaceCoordinateAxis(rootBuilder, makeXCoordAxis("x"))
            datasetBuilder.replaceCoordinateAxis(rootBuilder, makeYCoordAxis("y"))
        }
        val timeCoord = makeTimeCoordAxis()
        if (timeCoord != null) {
            datasetBuilder.replaceCoordinateAxis(rootBuilder, timeCoord)
            val dimName = timeCoord.firstDimensionName
            if (timeCoord.shortName != dimName) {
                timeCoord.addAttribute(Attribute(_Coordinate.AliasForDimension, dimName))
            }
        }

        // AWIPS cleverly combines multiple z levels into a single variable (!!)
        for (ncvar in ImmutableList.copyOf<Variable.Builder<*>>(rootBuilder.vbuilders)) {
            val levelName = ncvar.shortName + "Levels"
            if (rootBuilder.findVariableLocal(levelName).isPresent()) {
                val levelVar = rootBuilder.findVariableLocal(levelName).get() as VariableDS.Builder<*>
                if (levelVar.rank != 2) continue
                if (levelVar.dataType != ArrayType.CHAR) continue
                try {
                    val levels = breakupLevels(levelVar)
                    createNewVariables(ncvar as VariableDS.Builder<*>, levels, levelVar.orgVar.getDimension(0))
                } catch (ioe: InvalidRangeException) {
                    info.appendLine("createNewVariables IOException%n")
                }
            }
        }
        if (projCT != null) {
            val v = makeCoordinateTransformVariable(projCT!!)
            v.addAttribute(Attribute(_Coordinate.Axes, "x y"))
            rootBuilder.addVariable(v)
        }

        // kludge in fixing the units
        for (v in rootBuilder.vbuilders) {
            val units = v.attributeContainer.findAttributeString(CDM.UNITS, null)
            if (units != null) {
                (v as VariableDS.Builder<*>).setUnits(normalize(units)) // removes the old
            }
        }

        return datasetBuilder.build()
    }

    private fun normalize(orgUnits: String): String {
        var units = orgUnits
        if (units == "/second") units = "1/sec"
        if (units == "degrees K") units = "K" else {
            units = units.replace("**", "^")
            units = StringUtil2.remove(units, ')'.code)
            units = StringUtil2.remove(units, '('.code)
        }
        return units
    }

    // TODO not dealing with "FHAG 0 10 ", "FHAG 0 30 "
    // take a combined level variable and create multiple levels out of it
    // return the list of Dimensions that were created
    private fun breakupLevels(levelVar: VariableDS.Builder<*>): List<Dimension> {
        if (debugBreakup) info.appendLine("breakupLevels ${levelVar.shortName}")
        val dimList: MutableList<Dimension> = ArrayList()
        val levelVarData: Array<Byte>
        levelVarData = try {
            levelVar.orgVar.readArray() as Array<Byte>
        } catch (ioe: IOException) {
            return dimList
        }
        var values: MutableList<String>? = null
        var currentUnits: String? = null
        val levels = Arrays.makeStringsFromChar(levelVarData)
        for (levelS in levels) {
            val stoke = StringTokenizer(levelS)

            /*
       * problem with blank string:
       * char pvvLevels(levels_35=35, charsPerLevel=10);
       * "MB 1000   ", "MB 975    ", "MB 950    ", "MB 925    ", "MB 900    ", "MB 875    ", "MB 850    ", "MB 825    ",
       * "MB 800    ", "MB 775    ", "MB 750    ",
       * "MB 725    ", "MB 700    ", "MB 675    ", "MB 650    ", "MB 625    ", "MB 600    ", "MB 575    ", "MB 550    ",
       * "MB 525    ", "MB 500    ", "MB 450    ",
       * "MB 400    ", "MB 350    ", "MB 300    ", "MB 250    ", "MB 200    ", "MB 150    ", "MB 100    ", "BL 0 30   ",
       * "BL 60 90  ", "BL 90 120 ", "BL 120 150",
       * "BL 150 180", ""
       */
        if (!stoke.hasMoreTokens()) continue  // skip it
            // first token is the unit
            val units = stoke.nextToken().trim { it <= ' ' }
            if (units != currentUnits) {
                if (values != null) dimList.add(makeZCoordAxis(values, currentUnits))
                values = ArrayList()
                currentUnits = units
            }

            // next token is the value
            if (stoke.hasMoreTokens()) values!!.add(stoke.nextToken()) else values!!.add("0")
        }
        if (values != null) dimList.add(makeZCoordAxis(values, currentUnits))
        if (debugBreakup) info.appendLine("  done breakup")
        return dimList
    }

    // make a new variable out of the list in "values"
    private fun makeZCoordAxis(values: List<String>, units: String?): Dimension {
        val len = values.size
        var name = makeZCoordName(units)
        name = if (len > 1) {
            name + len
        } else {
            name + values[0]
        }
        StringUtil2.replace(name, ' ', "-")
        if (rootBuilder.findDimension(name).isPresent()) {
            val dim: Dimension = rootBuilder.findDimension(name).get()
            if (dim.length == len) {
                if (rootBuilder.findVariableLocal(name).isPresent()) {
                    return dim
                }
            }
        }
        val orgName: String = name
        var count = 1
        while (rootBuilder.findDimension(name).isPresent()) {
            name = "$orgName-$count"
            count++
        }

        // create new one
        val dim = Dimension(name, len)
        rootBuilder.addDimension(dim)
        if (debugBreakup) {
            info.appendLine("  make Dimension and ZCoord $name length $len")
        }
        val zunits = makeUnitsName(units)
        val v = CoordinateAxis1D.builder().setName(name).setArrayType(ArrayType.DOUBLE)
            .setParentGroupBuilder(rootBuilder)
            .setDimensionsByName(name).setUnits(zunits).setDesc(makeLongName(name))

        val positive: String = if (SimpleUnit.pressureUnit.isCompatible(zunits)) "up" else "down"
        v.addAttribute(Attribute(_Coordinate.ZisPositive, positive))

        val dvalues = DoubleArray(values.size)
        var countv = 0
        for (s in values) {
            try {
                dvalues[countv++] = s.toDouble()
            } catch (e: NumberFormatException) {
                System.out.printf("NumberFormatException '%s'", s)
                throw e
            }
        }
        val data = Arrays.factory<Double>(ArrayType.DOUBLE, intArrayOf(values.size), dvalues)
        v.setSourceData(data)
        datasetBuilder.replaceCoordinateAxis(rootBuilder, v)
        info.appendLine("Created Z Coordinate Axis = $name")
        return dim
    }

    private fun makeZCoordName(units: String?): String? {
        if (units.equals("MB", ignoreCase = true)) return "PressureLevels"
        if (units.equals("K", ignoreCase = true)) return "PotTempLevels"
        if (units.equals("BL", ignoreCase = true)) return "BoundaryLayers"
        if (units.equals("FHAG", ignoreCase = true)) return "FixedHeightAboveGround"
        if (units.equals("FH", ignoreCase = true)) return "FixedHeight"
        if (units.equals("SFC", ignoreCase = true)) return "Surface"
        if (units.equals("MSL", ignoreCase = true)) return "MeanSeaLevel"
        if (units.equals("FRZ", ignoreCase = true)) return "FreezingLevel"
        if (units.equals("TROP", ignoreCase = true)) return "Tropopause"
        return if (units.equals("MAXW", ignoreCase = true)) "MaxWindLevel" else units
    }

    private fun makeUnitsName(units: String?): String {
        if (units.equals("MB", ignoreCase = true)) return "hPa"
        if (units.equals("BL", ignoreCase = true)) return "hPa"
        if (units.equals("FHAG", ignoreCase = true)) return "m"
        return if (units.equals("FH", ignoreCase = true)) "m" else ""
    }

    private fun makeLongName(name: String?): String? {
        if (name.equals("PotTempLevels", ignoreCase = true)) return "Potential Temperature Level"
        return if (name.equals("BoundaryLayers", ignoreCase = true)) "BoundaryLayer hectoPascals above ground" else name
    }

    // create new variables as sections of ncVar
    @Throws(InvalidRangeException::class)
    private fun createNewVariables(ncVar: VariableDS.Builder<*>, newDims: List<Dimension>, levelDim: Dimension) {
        val dims = ArrayList(ncVar.orgVar.dimensions)
        val newDimIndex = dims.indexOf(levelDim)
        val origin = IntArray(ncVar.rank)
        val shape = ncVar.orgVar.shape
        var count = 0
        for (dim in newDims) {
            origin[newDimIndex] = count
            shape[newDimIndex] = dim.length
            val varSection = ncVar.orgVar.section(Section(origin, shape))
            val name = ncVar.shortName + "-" + dim.shortName
            val varNew = VariableDS.builder().setName(name).setOriginalVariable(varSection).setArrayType(ncVar.dataType)
            dims[newDimIndex] = dim
            varNew.addDimensions(dims)
            varNew.addAttributes(ncVar.attributeContainer)

            // synthesize long name
            var long_name = ncVar.attributeContainer.findAttributeString(CDM.LONG_NAME, ncVar.shortName)
            long_name = long_name + "-" + dim.shortName
            varNew.attributeContainer.addAttribute(Attribute(CDM.LONG_NAME, long_name))
            rootBuilder.addVariable(varNew)
            info.appendLine("Created New Variable as section $name")
            count += dim.length
        }
    }

    @Throws(NoSuchElementException::class)
    protected fun makeLCProjection(name: String): ProjectionCTV {
        val centralLat = findAttributeDouble("centralLat")
        val centralLon = findAttributeDouble("centralLon")
        val rotation = findAttributeDouble("rotation")

        // we have to project in order to find the origin
        val lc = LambertConformal(rotation, centralLon, centralLat, centralLat)
        val lat0 = findAttributeDouble("lat00")
        val lon0 = findAttributeDouble("lon00")
        val start = lc.latLonToProj(LatLonPoint(lat0, lon0))
        if (debugProj) info.appendLine("getLCProjection start at proj coord $start")
        startx = start.x()
        starty = start.y()
        dx = findAttributeDouble("dxKm")
        dy = findAttributeDouble("dyKm")
        return ProjectionCTV(name, lc)
    }

    @Throws(NoSuchElementException::class)
    protected fun makeStereoProjection(name: String): ProjectionCTV {
        val centralLat = findAttributeDouble("centralLat")
        val centralLon = findAttributeDouble("centralLon")

        // scale factor at lat = k = 2*k0/(1+sin(lat)) [Snyder,Working Manual p157]
        // then to make scale = 1 at lat, k0 = (1+sin(lat))/2
        val latDxDy = findAttributeDouble("latDxDy")
        val latR = Math.toRadians(latDxDy)
        val scale = (1.0 + Math.abs(Math.sin(latR))) / 2 // thanks to R Schmunk

        // Stereographic(double latt, double lont, double scale)
        val proj = Stereographic(centralLat, centralLon, scale)
        // we have to project in order to find the origin
        val lat0 = findAttributeDouble("lat00")
        val lon0 = findAttributeDouble("lon00")
        val start = proj.latLonToProj(LatLonPoint(lat0, lon0))
        startx = start.x()
        starty = start.y()
        dx = findAttributeDouble("dxKm")
        dy = findAttributeDouble("dyKm")

        // projection info
        info.appendLine("---makeStereoProjection start at proj coord $start")
        val latN = findAttributeDouble("latNxNy")
        val lonN = findAttributeDouble("lonNxNy")
        val pt = proj.latLonToProj(LatLonPoint(latN, lonN))
        info.appendLine("                        end at proj coord $pt")
        info.appendLine("                        scale $scale")
        return ProjectionCTV(name, proj)
    }

    protected fun makeXCoordAxis(xname: String?): CoordinateAxis.Builder<*>? {
        val v = CoordinateAxis1D.builder().setName(xname).setArrayType(ArrayType.DOUBLE)
            .setParentGroupBuilder(rootBuilder).setDimensionsByName(xname).setUnits("km").setDesc("x on projection")
        v.setAutoGen(startx, dx)
        info.appendLine("Created X Coordinate Axis $xname")
        return v
    }

    protected fun makeYCoordAxis(yname: String?): CoordinateAxis.Builder<*>? {
        val v = CoordinateAxis1D.builder().setName(yname).setArrayType(ArrayType.DOUBLE)
            .setParentGroupBuilder(rootBuilder).setDimensionsByName(yname).setUnits("km").setDesc("y on projection")
        v.setAutoGen(starty, dy)
        info.appendLine("Created Y Coordinate Axis $yname")
        return v
    }

    protected fun makeLonCoordAxis(n: Int, xname: String): CoordinateAxis.Builder<*>? {
        val min = findAttributeDouble("xMin")
        val max = findAttributeDouble("xMax")
        val d = findAttributeDouble("dx")
        if (java.lang.Double.isNaN(min) || java.lang.Double.isNaN(max) || java.lang.Double.isNaN(d)) return null
        val v = CoordinateAxis1D.builder().setName(xname).setArrayType(ArrayType.DOUBLE)
            .setParentGroupBuilder(rootBuilder).setDimensionsByName(xname).setUnits(CDM.LON_UNITS).setDesc("longitude")
        v.addAttribute(Attribute(_Coordinate.AxisType, AxisType.Lon.toString()))
        v.setAutoGen(min, d)
        val maxCalc = min + d * n
        info.appendLine("Created Lon Coordinate Axis (max calc= $maxCalc should be = $max)")
        return v
    }

    protected fun makeLatCoordAxis(n: Int, name: String): CoordinateAxis.Builder<*>? {
        val min = findAttributeDouble("yMin")
        val max = findAttributeDouble("yMax")
        val d = findAttributeDouble("dy")
        if (java.lang.Double.isNaN(min) || java.lang.Double.isNaN(max) || java.lang.Double.isNaN(d)) return null
        val v = CoordinateAxis1D.builder().setName(name).setArrayType(ArrayType.DOUBLE)
            .setParentGroupBuilder(rootBuilder).setDimensionsByName(name).setUnits(CDM.LAT_UNITS).setDesc("latitude")
        v.addAttribute(Attribute(_Coordinate.AxisType, AxisType.Lat.toString()))
        v.setAutoGen(min, d)
        val maxCalc = min + d * n
        info.appendLine("Created Lat Coordinate Axis (max calc= $maxCalc should be = $max)")
        return v
    }

    private fun makeTimeCoordAxis(): CoordinateAxis.Builder<*>? {
        val timeVar = rootBuilder.findVariableLocal("valtimeMINUSreftime")
            .orElseThrow { RuntimeException("must have varible 'valtimeMINUSreftime'") }
        val recordDim: Dimension = rootBuilder.findDimension("record")
            .orElseThrow { RuntimeException("must have dimension 'record'") }
        var vals: Array<Number>
        vals = try {
            (timeVar as VariableDS.Builder).orgVar.readArray() as Array<Number>
        } catch (ioe: IOException) {
            return null
        }

        // it seems that the record dimension does not always match valtimeMINUSreftime dimension!!
        // HAHAHAHAHAHAHAHA !
        val recLen = recordDim.length
        val valLen = vals.size.toInt()
        if (recLen != valLen) {
            try {
                val section = Section(intArrayOf(0), intArrayOf(recordDim.length))
                vals = Arrays.section(vals, section)
                info.appendLine(" corrected the TimeCoordAxis length%n")
            } catch (e: InvalidRangeException) {
                info.appendLine("makeTimeCoordAxis InvalidRangeException%n")
            }
        }

        // create the units out of the filename if possible
        val units = makeTimeUnitFromFilename(datasetBuilder.location)
            ?: // ok that didnt work, try something else
            return try {
                makeTimeCoordAxisFromReference(vals)
            } catch (e: IOException) {
                throw RuntimeException(e)
            }

        // create the coord axis
        val name = "timeCoord"
        val desc = "synthesized time coordinate from valtimeMINUSreftime and filename YYYYMMDD_HHMM"
        val timeCoord =
            CoordinateAxis1D.builder().setName(name).setArrayType(ArrayType.INT).setParentGroupBuilder(rootBuilder)
                .setDimensionsByName("record").setUnits(units).setDesc(desc).setSourceData(vals)
        info.appendLine("Created Time Coordinate Axis = $name")
        return timeCoord
    }

    private fun makeTimeUnitFromFilename(name: String): String? {
        var dsName = name
        dsName = dsName.replace('\\', '/')

        // posFirst: last '/' if it exists
        var posFirst = dsName.lastIndexOf('/')
        if (posFirst < 0) posFirst = 0

        // posLast: next '.' if it exists
        val posLast = dsName.indexOf('.', posFirst)
        dsName = if (posLast < 0) dsName.substring(posFirst + 1) else dsName.substring(posFirst + 1, posLast)

        // gotta be YYYYMMDD_HHMM
        if (dsName.length != 13) return null
        val year = dsName.substring(0, 4)
        val mon = dsName.substring(4, 6)
        val day = dsName.substring(6, 8)
        val hour = dsName.substring(9, 11)
        val min = dsName.substring(11, 13)
        return "seconds since $year-$mon-$day $hour:$min:0"
    }

    // construct time coordinate from reftime variable
    @Throws(IOException::class)
    private fun makeTimeCoordAxisFromReference(vals: Array<Number>): CoordinateAxis.Builder<*>? {
        if (rootBuilder.findVariableLocal("reftime").isEmpty()) {
            return null
        }
        val refVar = rootBuilder.findVariableLocal("reftime").get() as VariableDS.Builder<*>
        val refValue = refVar.orgVar.readScalarDouble()
        if (refValue == IospUtils.NC_FILL_DOUBLE || java.lang.Double.isNaN(refValue)) { // why?
            return null
        }

        // construct the values array - make it a double to be safe
        val dvals = DoubleArray(vals.length().toInt())
        var count = 0
        for (`val` in vals) {
            dvals[count++] = `val`.toDouble() + refValue
        }
        val dvalArray = Arrays.factory<Double>(ArrayType.DOUBLE, vals.shape, dvals)
        val name = "timeCoord"
        var units = refVar.attributeContainer.findAttributeString(CDM.UNITS, "seconds since 1970-1-1 00:00:00")!!
        units = normalize(units)
        val desc = "synthesized time coordinate from reftime, valtimeMINUSreftime"
        val timeCoord =
            CoordinateAxis1D.builder().setName(name).setArrayType(ArrayType.DOUBLE).setParentGroupBuilder(rootBuilder)
                .setDimensionsByName("record").setUnits(units).setDesc(desc).setSourceData(dvalArray)
        info.appendLine("Created Time Coordinate Axis From reftime Variable%n")
        return timeCoord
    }

    protected fun findAttributeDouble(attname: String?): Double {
        val att: Attribute? = globalAtts.findAttributeIgnoreCase(attname)
        if (att == null || att.isString) {
            info.appendLine("ERROR cant find numeric attribute= $attname")
            return Double.NaN
        }
        return att.numericValue!!.toDouble()
    }
}
