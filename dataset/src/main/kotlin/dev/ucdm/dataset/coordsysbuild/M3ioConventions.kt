package dev.ucdm.dataset.coordsysbuild

import com.google.common.base.Preconditions
import dev.ucdm.array.*
import dev.ucdm.array.Arrays
import dev.ucdm.core.api.*
import dev.ucdm.core.constants.AxisType
import dev.ucdm.core.constants._Coordinate
import dev.ucdm.dataset.api.*
import dev.ucdm.dataset.geoloc.LatLonProjection
import dev.ucdm.dataset.geoloc.projection.*
import java.text.SimpleDateFormat
import java.util.*

private const val earthRadius = 6370.000 // km

open class M3ioConventions(name: String = "M3IO") : CoordinatesBuilder(name) {
    var projCT: CoordinateTransform? = null
    var globalAtts : AttributeContainer? = null

    override fun augment(orgDataset: CdmDataset): CdmDataset {
        globalAtts = orgDataset.attributes()
        val datasetBuilder = CdmDatasetCS.builder().copyFrom(orgDataset)
        val rootBuilder = datasetBuilder.rootGroup

        val projType: Int = globalAtts!!.findAttributeInteger("GDTYP", 1)
        val isLatLon = projType == 1
        if (isLatLon) {
            addCoordAxisLatLon(datasetBuilder, "lon", "COL", "XORIG", "XCELL", "degrees east")
            addCoordAxisLatLon(datasetBuilder, "lat", "ROW", "YORIG", "YCELL", "degrees north")
            projCT = makeLatLongProjection()
            val v = makeCoordinateTransformVariable(projCT!!)
            v.addAttribute(Attribute(_Coordinate.Axes, "lon lat"))
            rootBuilder.addVariable(v)
            
        } else {
            addCoordAxis(datasetBuilder,"x", "COL", "XORIG", "XCELL", "km")
            addCoordAxis(datasetBuilder,"y", "ROW", "YORIG", "YCELL", "km")

            when (projType) {
                2 -> projCT = makeLCProjection()
                3 -> projCT = makeTMProjection()
                4 -> projCT = makeSTProjection()
                5 -> projCT = makeUTMProjection()
                6 -> projCT = makePolarStereographicProjection()
                7 -> projCT = makeEquitorialMercatorProjection()
                8 -> projCT = makeTransverseMercatorProjection()
                9 -> projCT = makeAlbersProjection()
                10 -> projCT = makeLambertAzimuthalProjection()
            }
            if (projCT != null) {
                val v = makeCoordinateTransformVariable(projCT!!)
                v.addAttribute(Attribute(_Coordinate.Axes, "x y"))
                rootBuilder.addVariable(v)
            }
        }
        makeZCoordAxis(datasetBuilder, "LAY", "VGLVLS", "sigma")
        makeTimeCoordAxis(datasetBuilder, "TSTEP")
        return datasetBuilder.build()
    }

    private fun addCoordAxis(datasetBuilder : CdmDatasetCS.Builder<*>,
        name: String, dimName: String, startName: String, incrName: String, unitName: String
    ) {
        var start: Double = .001 * findAttributeDouble(startName) // km
        val incr: Double = .001 * findAttributeDouble(incrName) // km
        start = start + incr / 2.0 // shifting x and y to central
        val coordVar: CoordinateAxis.Builder<*> = CoordinateAxis1D.builder().setName(name).setArrayType(ArrayType.DOUBLE)
            .setParentGroupBuilder(datasetBuilder.rootGroup).setDimensionsByName(dimName).setUnits(unitName)
            .setDesc("synthesized coordinate from $startName $incrName global attributes")
        coordVar.addAttribute(Attribute(_Coordinate.AliasForDimension, dimName))
        coordVar.setAutoGen(start, incr)
        datasetBuilder.replaceCoordinateAxis(datasetBuilder.rootGroup, coordVar)
    }

    private fun addCoordAxisLatLon(datasetBuilder : CdmDatasetCS.Builder<*>,
        name: String, dimName: String, startName: String, incrName: String,
        unitName: String
    ) {
        // Makes coordinate axes for Lat/Lon
        var start: Double = findAttributeDouble(startName) // degrees
        val incr: Double = findAttributeDouble(incrName) // degrees
        // The coordinate value should be the cell center.
        // I recommend also adding a bounds coordinate variable for clarity in the future.
        start = start + incr / 2.0 // shiftin lon and lat to central
        val coordVar: CoordinateAxis.Builder<*> = CoordinateAxis1D.builder().setName(name).setArrayType(ArrayType.DOUBLE)
            .setParentGroupBuilder(datasetBuilder.rootGroup).setDimensionsByName(dimName).setUnits(unitName)
            .setDesc("synthesized coordinate from $startName $incrName global attributes")
        coordVar.addAttribute(Attribute(_Coordinate.AliasForDimension, dimName))
        coordVar.setAutoGen(start, incr)
        datasetBuilder.replaceCoordinateAxis(datasetBuilder.rootGroup, coordVar)
    }

    private fun makeZCoordAxis(datasetBuilder : CdmDatasetCS.Builder<*>, 
                               dimName: String, levelsName: String, unitName: String) {
        val dimzOpt = datasetBuilder.rootGroup.findDimension(dimName)
        if (dimzOpt.isEmpty()) {
            info.appendLine("Missing layer dimension names $dimName in a global attribute")
            return
        }

        val layers: Attribute? = datasetBuilder.rootGroup.getAttributeContainer().findAttribute(levelsName)
        if (layers == null) {
            info.appendLine("Missing layer values in a global attribute array $levelsName")
            return
        }

        val nz = dimzOpt.get().length
        val dataLev = DoubleArray(nz)
        val dataLayers = DoubleArray(nz + 1)

        // layer values are a numeric global attribute array !!
        Preconditions.checkArgument(layers.length == nz + 1)
        for (i in 0..nz) {
            dataLayers[i] = layers.getNumericValue(i)!!.toDouble()
        }
        for (i in 0 until nz) {
            dataLev[i] = (dataLayers[i] + dataLayers[i + 1]) / 2
        }

        val v: CoordinateAxis.Builder<*> = CoordinateAxis1D.builder().setName("level").setArrayType(ArrayType.DOUBLE)
            .setParentGroupBuilder(datasetBuilder.rootGroup).setDimensionsByName(dimName).setUnits(unitName)
            .setDesc("synthesized coordinate from $levelsName global attributes")
        v.setSourceData(Arrays.factory<Any>(ArrayType.DOUBLE, intArrayOf(nz), dataLev))
        v.addAttribute(Attribute("positive", "down"))
        v.addAttribute(Attribute(_Coordinate.AxisType, AxisType.GeoZ.toString()))
        v.addAttribute(Attribute(_Coordinate.AliasForDimension, dimName))

        // layer edges
        val edge_name = "layer"
        val lay_edge = Dimension(edge_name, nz + 1)
        datasetBuilder.rootGroup.addDimension(lay_edge)
        val vedge: CoordinateAxis.Builder<*> =
            CoordinateAxis1D.builder().setName(edge_name).setArrayType(ArrayType.DOUBLE)
                .setParentGroupBuilder(datasetBuilder.rootGroup).setDimensionsByName(edge_name).setUnits(unitName)
                .setDesc("synthesized coordinate from $levelsName global attributes")
        vedge.setSourceData(Arrays.factory<Any>(ArrayType.DOUBLE, intArrayOf(nz + 1), dataLayers))
        v.setBoundary(edge_name)
        datasetBuilder.replaceCoordinateAxis(datasetBuilder.rootGroup, v)
        datasetBuilder.replaceCoordinateAxis(datasetBuilder.rootGroup, vedge)
    }

    private fun makeTimeCoordAxis(datasetBuilder : CdmDatasetCS.Builder<*>, timeName: String) {
        val start_date: Int = findAttributeInt("SDATE")
        var start_time: Int = findAttributeInt("STIME")
        var time_step: Int = findAttributeInt("TSTEP")
        val year = start_date / 1000
        val doy = start_date % 1000
        var hour = start_time / 10000
        start_time = start_time % 10000
        var min = start_time / 100
        var sec = start_time % 100
        // TODO replace
        val cal: Calendar = GregorianCalendar(SimpleTimeZone(0, "GMT"))
        cal.clear()
        cal[Calendar.YEAR] = year
        cal[Calendar.DAY_OF_YEAR] = doy
        cal[Calendar.HOUR_OF_DAY] = hour
        cal[Calendar.MINUTE] = min
        cal[Calendar.SECOND] = sec
        val dateFormatOut = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        dateFormatOut.timeZone = TimeZone.getTimeZone("GMT")
        val units = "seconds since " + dateFormatOut.format(cal.time) + " UTC"

        // parse the time step
        hour = time_step / 10000
        time_step = time_step % 10000
        min = time_step / 100
        sec = time_step % 100
        time_step = hour * 3600 + min * 60 + sec

        // create the coord axis
        val timeCoord = CoordinateAxis1D.builder().setName("time").setArrayType(ArrayType.INT)
            .setParentGroupBuilder(datasetBuilder.rootGroup).setDimensionsByName(timeName).setUnits(units)
            .setDesc("synthesized time coordinate from SDATE, STIME, STEP global attributes")
        timeCoord.setAutoGen(0.0, time_step.toDouble())
        timeCoord.addAttribute(Attribute(_Coordinate.AxisType, AxisType.Time.toString()))
        timeCoord.addAttribute(Attribute(_Coordinate.AliasForDimension, "TSTEP"))
        datasetBuilder.replaceCoordinateAxis(datasetBuilder.rootGroup, timeCoord)
    }

    private fun makeLatLongProjection(): CoordinateTransform {
        // Get lower left and upper right corners of domain in lat/lon
        val x1: Double = findAttributeDouble("XORIG")
        val x2: Double = x1 + findAttributeDouble("XCELL") * findAttributeDouble("NCOLS")
        val ll = LatLonProjection("LatitudeLongitudeProjection", null, (x1 + x2) / 2)
        return CoordinateTransform(ll.name, ll.projectionAttributes, true)
    }

    private fun makeLCProjection(): CoordinateTransform {
        val par1: Double = findAttributeDouble("P_ALP")
        val par2: Double = findAttributeDouble("P_BET")
        val lon0: Double = findAttributeDouble("XCENT")
        val lat0: Double = findAttributeDouble("YCENT")
        val lc = LambertConformal(lat0, lon0, par1, par2, 0.0, 0.0, earthRadius)
        return CoordinateTransform(lc.name, lc.projectionAttributes, true)
    }

    private fun makePolarStereographicProjection(): CoordinateTransform {
        val lon0: Double = findAttributeDouble("XCENT")
        val lat0: Double = findAttributeDouble("YCENT")
        val latts: Double = findAttributeDouble("P_BET")
        val sg = Stereographic(latts, lat0, lon0, 0.0, 0.0, earthRadius)
        return CoordinateTransform(sg.name, sg.projectionAttributes, true)
    }

    private fun makeEquitorialMercatorProjection(): CoordinateTransform {
        val lon0: Double = findAttributeDouble("XCENT")
        val par: Double = findAttributeDouble("P_ALP")
        val p = Mercator(lon0, par, 0.0, 0.0, earthRadius)
        return CoordinateTransform(p.name, p.projectionAttributes, true)
    }

    private fun makeTransverseMercatorProjection(): CoordinateTransform {
        val lat0: Double = findAttributeDouble("P_ALP")
        val tangentLon: Double = findAttributeDouble("P_GAM")
        val p = TransverseMercator(lat0, tangentLon, 1.0, 0.0, 0.0, earthRadius)
        return CoordinateTransform(p.name, p.projectionAttributes, true)
    }

    private fun makeAlbersProjection(): CoordinateTransform {
        val lat0: Double = findAttributeDouble("YCENT")
        val lon0: Double = findAttributeDouble("XCENT")
        val par1: Double = findAttributeDouble("P_ALP")
        val par2: Double = findAttributeDouble("P_BET")
        val p = AlbersEqualArea(lat0, lon0, par1, par2, 0.0, 0.0, earthRadius)
        return CoordinateTransform(p.name, p.projectionAttributes, true)
    }

    private fun makeLambertAzimuthalProjection(): CoordinateTransform {
        val lat0: Double = findAttributeDouble("YCENT")
        val lon0: Double = findAttributeDouble("XCENT")
        val p = LambertAzimuthalEqualArea(lat0, lon0, 0.0, 0.0, earthRadius)
        return CoordinateTransform(p.name, p.projectionAttributes, true)
    }

    private fun makeSTProjection(): CoordinateTransform {
        var latt: Double = findAttributeDouble("PROJ_ALPHA")
        if (java.lang.Double.isNaN(latt)) latt = findAttributeDouble("P_ALP")
        var lont: Double = findAttributeDouble("PROJ_BETA")
        if (java.lang.Double.isNaN(lont)) lont = findAttributeDouble("P_BET")
        val st = Stereographic(latt, lont, 1.0, 0.0, 0.0, earthRadius)
        return CoordinateTransform(st.name, st.projectionAttributes, true)
    }

    private fun makeTMProjection(): CoordinateTransform {
        var lat0: Double = findAttributeDouble("PROJ_ALPHA")
        if (java.lang.Double.isNaN(lat0)) lat0 = findAttributeDouble("P_ALP")
        var tangentLon: Double = findAttributeDouble("PROJ_BETA")
        if (java.lang.Double.isNaN(tangentLon)) tangentLon = findAttributeDouble("P_BET")
        val tm = TransverseMercator(lat0, tangentLon, 1.0, 0.0, 0.0, earthRadius)
        return CoordinateTransform(tm.name, tm.projectionAttributes, true)
    }

    private fun makeUTMProjection(): CoordinateTransform {
        val zone: Int = findAttributeDouble("P_ALP").toInt()
        val ycent: Double = findAttributeDouble("YCENT")
        var isNorth = true
        if (ycent < 0) isNorth = false
        val utm = UtmProjection(zone, isNorth)
        return CoordinateTransform(utm.name, utm.projectionAttributes, true)
    }

    private fun findAttributeDouble(attname: String): Double {
        return globalAtts!!.findAttributeDouble(attname, Double.NaN)
    }

    private fun findAttributeInt(attname: String): Int {
        return globalAtts!!.findAttributeInteger(attname, 0)
    }

    //////////////////////////////////////////////////////////////////////////////

    override fun identifyAxisType(vds: VariableDS): AxisType? {
        val vname = vds.shortName
        if (vname.equals("x", ignoreCase = true)) return AxisType.GeoX
        if (vname.equals("y", ignoreCase = true)) return AxisType.GeoY
        if (vname.equals("lat", ignoreCase = true)) return AxisType.Lat
        if (vname.equals("lon", ignoreCase = true)) return AxisType.Lon
        if (vname.equals("time", ignoreCase = true)) return AxisType.Time
        if (vname.equals("level", ignoreCase = true)) return AxisType.GeoZ
        return null
    }

    override fun makeCoordinateTransforms() {
        if (projCT != null) {
            coords.addCoordinateTransform(projCT!!)
        }
        super.makeCoordinateTransforms()
    }
}

fun isM3ioConventions(ncfile: CdmFile): Boolean {
    return null != ncfile.findAttribute("XORIG") &&
            null != ncfile.findAttribute("YORIG") &&
            null != ncfile.findAttribute("XCELL") &&
            null != ncfile.findAttribute("YCELL") &&
            null != ncfile.findAttribute("NCOLS") &&
            null != ncfile.findAttribute("NROWS")
}