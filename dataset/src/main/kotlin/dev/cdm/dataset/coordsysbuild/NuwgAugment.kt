package dev.cdm.dataset.coordsysbuild

import dev.cdm.array.Array
import dev.cdm.array.ArrayType
import dev.cdm.array.Arrays
import dev.cdm.core.api.Attribute
import dev.cdm.core.api.Dimension
import dev.cdm.core.api.Variable
import dev.cdm.core.constants.AxisType
import dev.cdm.core.constants.CDM
import dev.cdm.core.constants.CF
import dev.cdm.core.constants._Coordinate
import dev.cdm.core.util.StringUtil2
import dev.cdm.dataset.api.*
import dev.cdm.dataset.geoloc.LatLonPoint
import dev.cdm.dataset.geoloc.projection.LambertConformal
import dev.cdm.dataset.geoloc.projection.Stereographic
import dev.cdm.dataset.transform.horiz.ProjectionCTV
import java.io.IOException
import java.util.*

class NuwgAugment(val dataset: CdmDataset, val info : StringBuilder) {
    val datasetBuilder = CdmDatasetCS.builder().copyFrom(dataset)
    val rootBuilder = datasetBuilder.rootGroup
    var xaxisName: String? = ""
    var yaxisName: String? = ""

    private val navInfoList = NavInfoList()
    private var grib: Grib1? = null
    private val dumpNav = false

    fun augment(): CdmDataset {
        // find all variables that have the nav dimension
        // put them into a NavInfoList
        // make their data into metadata
        for (vb in rootBuilder.vbuilders) {
            if (vb is VariableDS.Builder<*>) {
                if (0 <= vb.orgVar.findDimensionIndex("nav")) {
                    if (dumpNav) info.appendLine("NUWG has NAV var '$vb'")
                    try {
                        navInfoList.navInfo.add(NavInfo(vb))
                    } catch (ex: IOException) {
                        info.appendLine("ERROR NUWG reading NAV var '$vb'")
                    }
                }
            }
        }
        navInfoList.navInfo.sortWith(NavComparator())
        info.appendLine("$navInfoList")

        // problem is NUWG doesnt identify the x, y coords.
        // so we get to hack it in here
        var mode = 3 // default is LambertConformal
        try {
            mode = navInfoList.getInt("grid_type_code")
        } catch (e: NoSuchElementException) {
            info.appendLine("   No mode in navInfo - assume 3")
        }
        try {
            if (mode == 0) {
                xaxisName = navInfoList.getString("i_dim")
                yaxisName = navInfoList.getString("j_dim")
            } else {
                xaxisName = navInfoList.getString("x_dim")
                yaxisName = navInfoList.getString("y_dim")
            }
        } catch (e: NoSuchElementException) {
            info.appendLine("  (warn) No mode in navInfo - assume = 1")
        }
        grib = Grib1(mode)
        if (rootBuilder.findVariableLocal(xaxisName).isEmpty()) {
            grib!!.makeXCoordAxis(xaxisName)
            info.appendLine("Generated x axis from NUWG nav= $xaxisName")
        } else if (xaxisName.equals("lon", ignoreCase = true)) {
            try {
                // check monotonicity
                var ok = true
                val dc = rootBuilder.findVariableLocal(xaxisName).get() as VariableDS.Builder<*>
                val coordVal = dc.orgVar.readArray() as Array<Number>
                val coord1 = coordVal[0].toDouble()
                val coord2 = coordVal[1].toDouble()
                val increase = coord1 > coord2
                var last: Number? = null
                for (`val` in coordVal) {
                    if (last != null) {
                        if (`val`.toDouble() > last.toDouble() != increase) {
                            ok = false
                            break
                        }
                    }
                    last = `val`
                }
                if (!ok) {
                    info.appendLine("ERROR lon axis is not monotonic, regen from nav%n")
                    grib!!.makeXCoordAxis(xaxisName)
                }
            } catch (ioe: IOException) {
                info.appendLine("IOException when reading xaxis = $xaxisName")
            }
        }
        if (rootBuilder.findVariableLocal(yaxisName).isEmpty()) {
            grib!!.makeYCoordAxis(yaxisName)
            info.appendLine("Generated y axis from NUWG nav $yaxisName")
        }

        // "referential" variables
        for (dim in rootBuilder.getDimensions()) {
            val dimName = dim.shortName
            if (rootBuilder.findVariableLocal(dimName).isPresent()) continue  // already has coord axis
            val ncvars = searchAliasedDimension(dim)
            if (ncvars == null || ncvars.isEmpty()) // no alias
                continue
            if (ncvars.size == 1) {
                val ncvar = ncvars[0]
                if (ncvar.dataType == ArrayType.STRUCTURE) continue  // cant be a structure
                if (makeCoordinateAxis(ncvar, dim)) {
                    info.appendLine("Added referential coordAxis ${ncvar.shortName}")
                } else {
                    info.appendLine("Couldnt add referential coordAxis ${ncvar.shortName}")
                }
            } else if (ncvars.size == 2) {
                if (dimName == "record") {
                    val ncvar0 = ncvars[0]
                    val ncvar1 = ncvars[1]
                    val ncvar = (if (ncvar0.shortName.equals(
                            "valtime",
                            ignoreCase = true
                        )
                    ) ncvar0 else ncvar1) as VariableDS.Builder<*>
                    if (makeCoordinateAxis(ncvar, dim)) {
                        info.appendLine("Added referential coordAxis (2) ${ncvar.shortName}")

                        // the usual crap - clean up time units
                        var units = ncvar.units
                        if (units != null) {
                            units = StringUtil2.remove(units, '('.code)
                            units = StringUtil2.remove(units, ')'.code)
                            ncvar.addAttribute(Attribute(CDM.UNITS, units))
                            ncvar.setUnits(units)
                        }
                    } else {
                        info.appendLine("Couldnt add referential coordAxis ${ncvar.shortName}")
                    }
                } else {
                    if (makeCoordinateAxis(ncvars[0], ncvars[1], dim)) {
                        info.appendLine("Added referential boundary coordAxis (2) = ${ncvars[0].shortName}, ${ncvars[1].shortName}")
                    } else {
                        info.appendLine("Couldnt add referential coordAxis = ${ncvars[0].shortName}, ${ncvars[1].shortName}")
                    }
                }
            } // 2
        } // loop over dims
        if (grib!!.projectionCT != null) {
            val v = makeCoordinateTransformVariable(grib!!.projectionCT!!)
            v.addAttribute(Attribute(_Coordinate.Axes, "$xaxisName $yaxisName"))
            rootBuilder.addVariable(v)
        }
        
        return datasetBuilder.build()
    }

    fun getProjectionCTV() : ProjectionCTV? {
        return grib?.projectionCT
    }

    private fun makeCoordinateAxis(ncvar: Variable.Builder<*>, dim: Dimension): Boolean {
        if (ncvar.rank != 1) return false
        val vdimName = ncvar.firstDimensionName
        if (vdimName == null || vdimName != dim.shortName) return false
        if (dim.shortName != ncvar.shortName) {
            ncvar.addAttribute(Attribute(_Coordinate.AliasForDimension, dim.shortName))
        }
        return true
    }

    private fun makeCoordinateAxis(ncvar0: Variable.Builder<*>, ncvar1: Variable.Builder<*>, dim: Dimension): Boolean {
        if (ncvar0.rank != 1 || ncvar1.rank != 1) {
            return false
        }
        if (ncvar0.firstDimensionName != dim.shortName
            || ncvar1.firstDimensionName != dim.shortName
        ) {
            return false
        }
        val n = dim.length
        val midpointData = DoubleArray(n)
        val boundsData = DoubleArray(2 * n)
        try {
            val ds0 = ncvar0 as VariableDS.Builder<*>
            val ds1 = ncvar1 as VariableDS.Builder<*>
            val data0 = Arrays.toDouble(ds0.orgVar.readArray())
            val data1 = Arrays.toDouble(ds1.orgVar.readArray())
            var count = 0
            for (idx in 0 until n) {
                boundsData[count++] = data0[idx]
                boundsData[count++] = data1[idx]
                midpointData[idx] = (data0[idx] + data1[idx]) / 2
            }
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
        val boundsName = dim.shortName + "_bounds"
        val coordVarBounds: Variable.Builder<*> =
            VariableDS.builder().setName(boundsName).setArrayType(ArrayType.DOUBLE)
                .setDesc("synthesized Z coord bounds")
                .setParentGroupBuilder(this.rootBuilder).setDimensionsByName(dim.shortName + " 2")
                .setSourceData(Arrays.factory<Any>(ArrayType.DOUBLE, intArrayOf(n, 2), boundsData))
        this.rootBuilder.addVariable(coordVarBounds)
        val coordVar: Variable.Builder<*> = VariableDS.builder().setName(dim.shortName).setArrayType(ArrayType.DOUBLE)
            .setParentGroupBuilder(this.rootBuilder).addDimension(dim).setDesc("synthesized Z coord")
            .addAttribute(Attribute(CF.BOUNDS, boundsName))
            .addAttribute(Attribute(_Coordinate.AliasForDimension, dim.shortName))
            .setSourceData(Arrays.factory<Any>(ArrayType.DOUBLE, intArrayOf(n), midpointData))
        this.rootBuilder.addVariable(coordVar)
        return true
    }

    /**
     * Search for an aliased coord that may have multiple variables
     * :dimName = alias1, alias2;
     * Variable alias1(dim);
     * Variable alias2(dim);
     *
     * @param dim: look for this dimension name
     * @return Collection of nectdf variables, or null if none
     */
    private fun searchAliasedDimension(dim: Dimension): List<Variable.Builder<*>>? {
        val dimName = dim.shortName
        val alias: String = rootBuilder.getAttributeContainer().findAttributeString(dimName, null) ?: return null
        val vars: MutableList<Variable.Builder<*>> = ArrayList()
        val parser = StringTokenizer(alias, " ,")
        while (parser.hasMoreTokens()) {
            val token = parser.nextToken()
            if (rootBuilder.findVariableLocal(token).isEmpty()) {
                continue
            }
            val ncvar: Variable.Builder<*> = rootBuilder.findVariableLocal(token).get()
            if (ncvar.rank != 1) continue
            val firstDimName = ncvar.firstDimensionName
            if (dimName == firstDimName) {
                vars.add(ncvar)
            }
        }
        return vars
    }

    private class NavComparator : Comparator<NavInfo> {
        override fun compare(n1: NavInfo, n2: NavInfo): Int {
            return n1.name.compareTo(n2.name)
        }
    }

    inner class NavInfo(val vb: VariableDS.Builder<*>) {
        val orgVar: Variable
        val valueType: ArrayType
        var svalue: String? = null
        var bvalue: Byte = 0
        var ivalue = 0
        var dvalue = 0.0

        init {
            orgVar = vb.orgVar
            valueType = vb.dataType
            try {
                if (valueType == ArrayType.CHAR || valueType == ArrayType.STRING) {
                    svalue = orgVar.readScalarString()
                } else if (valueType == ArrayType.BYTE) {
                    bvalue = orgVar.readScalarByte()
                } else if (valueType == ArrayType.INT || valueType == ArrayType.SHORT) {
                    ivalue = orgVar.readScalarInt()
                } else {
                    dvalue = orgVar.readScalarDouble()
                }
            } catch (e: UnsupportedOperationException) {
                info.appendLine("  (warn) Nav variable $name not a scalar")
            }
        }

        val name: String
            get() = vb.shortName
        val description: String
            get() {
                val att = vb.attributeContainer.findAttributeIgnoreCase(CDM.LONG_NAME)
                return if (att == null) name else att.stringValue!!
            }
        val stringValue: String?
            get() = if (valueType == ArrayType.CHAR || valueType == ArrayType.STRING) svalue else if (valueType == ArrayType.BYTE) java.lang.Byte.toString(
                bvalue
            ) else if (valueType == ArrayType.INT || valueType == ArrayType.SHORT) Integer.toString(ivalue) else java.lang.Double.toString(
                dvalue
            )

        override fun toString(): String {
            return String.format("%14s %20s %s%n", name, stringValue, description)
        }
    }

    private class NavInfoList() {
        val navInfo = mutableListOf<NavInfo>()

        fun findInfo(name: String): NavInfo? {
            for (nav in navInfo) {
                if (name.equals(nav.name, ignoreCase = true)) return nav
            }
            return null
        }

        @Throws(NoSuchElementException::class)
        fun getDouble(name: String): Double {
            val nav = findInfo(name) ?: throw NoSuchElementException("GRIB1 $name")
            if (nav.valueType == ArrayType.DOUBLE || nav.valueType == ArrayType.FLOAT) return nav.dvalue else if (nav.valueType == ArrayType.INT || nav.valueType == ArrayType.SHORT) return nav.ivalue.toDouble() else if (nav.valueType == ArrayType.BYTE) return nav.bvalue.toDouble()
            throw IllegalArgumentException("NUWGConvention.GRIB1.getDouble " + name + " type = " + nav.valueType)
        }

        @Throws(NoSuchElementException::class)
        fun getInt(name: String): Int {
            val nav = findInfo(name) ?: throw NoSuchElementException("GRIB1 $name")
            if (nav.valueType == ArrayType.INT || nav.valueType == ArrayType.SHORT) return nav.ivalue else if (nav.valueType == ArrayType.DOUBLE || nav.valueType == ArrayType.FLOAT) return nav.dvalue.toInt() else if (nav.valueType == ArrayType.BYTE) return nav.bvalue.toInt()
            throw IllegalArgumentException("NUWGConvention.GRIB1.getInt " + name + " type = " + nav.valueType)
        }

        @Throws(NoSuchElementException::class)
        fun getString(name: String): String? {
            val nav = findInfo(name) ?: throw NoSuchElementException("GRIB1 $name")
            return nav.svalue
        }

        override fun toString(): String {
            val buf = StringBuilder(2000)
            buf.append("\nNav Info\n")
            buf.append("Name___________Value_____________________Description\n")
            for (nava in navInfo) {
                buf.append(nava).append("\n")
            }
            buf.append("\n")
            return buf.toString()
        }
    }

    // encapsolates GRIB-specific processing
    inner class Grib1(val grid_code: Int) {
        val grid_name = "Projection"
        var projectionCT: ProjectionCTV? = null
        var nx = 0
        var ny = 0
        var startx = 0.0
        var starty = 0.0
        var dx = 0.0
        var dy = 0.0

        init {
            if (0 == grid_code) processLatLonProjection() else if (3 == grid_code) projectionCT =
                makeLCProjection() else if (5 == grid_code) projectionCT =
                makePSProjection() else throw IllegalArgumentException(
                "NUWGConvention: unknown grid_code= $grid_code"
            )
        }

        fun makeXCoordAxis(xname: String?) {
            val v: CoordinateAxis.Builder<*> = CoordinateAxis1D.builder().setName(xname).setArrayType(ArrayType.DOUBLE)
                .setParentGroupBuilder(rootBuilder).setDimensionsByName(xname)
                .setUnits(if (0 == grid_code) CDM.LON_UNITS else "km")
                .setDesc("synthesized X coord")
            v.addAttribute(
                Attribute(
                    _Coordinate.AxisType,
                    if (0 == grid_code) AxisType.Lon.toString() else AxisType.GeoX.toString()
                )
            )
            v.setAutoGen(startx, dx)
            datasetBuilder.replaceCoordinateAxis(rootBuilder, v)
        }

        fun makeYCoordAxis(yname: String?) {
            val v: CoordinateAxis.Builder<*> = CoordinateAxis1D.builder().setName(yname).setArrayType(ArrayType.DOUBLE)
                .setParentGroupBuilder(rootBuilder).setDimensionsByName(yname)
                .setUnits(if (0 == grid_code) CDM.LAT_UNITS else "km")
                .setDesc("synthesized Y coord")
            v.addAttribute(
                Attribute(
                    _Coordinate.AxisType,
                    if (0 == grid_code) AxisType.Lat.toString() else AxisType.GeoY.toString()
                )
            )
            v.setAutoGen(starty, dy)
            datasetBuilder.replaceCoordinateAxis(rootBuilder, v)
        }

        @Throws(NoSuchElementException::class)
        fun makeLCProjection(): ProjectionCTV {
            val latin1: Double = navInfoList.getDouble("Latin1")
            val latin2: Double = navInfoList.getDouble("Latin2")
            val lov: Double = navInfoList.getDouble("Lov")
            val la1: Double = navInfoList.getDouble("La1")
            val lo1: Double = navInfoList.getDouble("Lo1")

            // we have to project in order to find the origin
            val lc = LambertConformal(latin1, lov, latin1, latin2)
            val start = lc.latLonToProj(LatLonPoint(la1, lo1))
            startx = start.x()
            starty = start.y()
            nx = navInfoList.getInt("Nx")
            ny = navInfoList.getInt("Ny")
            dx = navInfoList.getDouble("Dx") / 1000.0 // TODO need to be km : unit conversion
            dy = navInfoList.getDouble("Dy") / 1000.0
            return ProjectionCTV(grid_name, lc)
        }

        // polar stereographic
        @Throws(NoSuchElementException::class)
        fun makePSProjection(): ProjectionCTV {
            val lov: Double = navInfoList.getDouble("Lov")
            val la1: Double = navInfoList.getDouble("La1")
            val lo1: Double = navInfoList.getDouble("Lo1")

            // Why the scale factor?. accordining to GRID docs:
            // "Grid lengths are in units of meters, at the 60 degree latitude circle nearest to the pole"
            // since the scale factor at 60 degrees = k = 2*k0/(1+sin(60)) [Snyder,Working Manual p157]
            // then to make scale = 1 at 60 degrees, k0 = (1+sin(60))/2 = .933
            val ps = Stereographic(90.0, lov, .933)

            // we have to project in order to find the origin
            val start = ps.latLonToProj(LatLonPoint(la1, lo1))
            startx = start.x()
            starty = start.y()
            nx = navInfoList.getInt("Nx")
            ny = navInfoList.getInt("Ny")
            dx = navInfoList.getDouble("Dx") / 1000.0
            dy = navInfoList.getDouble("Dy") / 1000.0
            return ProjectionCTV(grid_name, ps)
        }

        @Throws(NoSuchElementException::class)
        fun processLatLonProjection() {
            // get stuff we need to construct axes
            starty = navInfoList.getDouble("La1")
            startx = navInfoList.getDouble("Lo1")
            nx = navInfoList.getInt("Ni")
            ny = navInfoList.getInt("Nj")
            dx = navInfoList.getDouble("Di")
            dy = navInfoList.getDouble("Dj")
        }
    } // GRIB1 */

}