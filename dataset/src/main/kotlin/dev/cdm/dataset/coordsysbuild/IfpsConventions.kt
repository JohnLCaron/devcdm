package dev.cdm.dataset.coordsysbuild

import dev.cdm.array.*
import dev.cdm.array.Array
import dev.cdm.core.api.*
import dev.cdm.core.constants.AxisType
import dev.cdm.core.constants.CDM
import dev.cdm.core.constants._Coordinate
import dev.cdm.dataset.api.CdmDataset
import dev.cdm.dataset.api.CdmDatasetCS
import dev.cdm.dataset.api.CoordinateAxis1D
import dev.cdm.dataset.api.VariableDS
import dev.cdm.dataset.geoloc.LatLonPoint
import dev.cdm.dataset.geoloc.Projection
import dev.cdm.dataset.geoloc.projection.LambertConformal
import dev.cdm.dataset.transform.horiz.ProjectionCTV
import java.io.IOException

open class IfpsConventions(name: String = "IFPS") : CoordSysBuilder(name) {

    override fun augment(orgDataset: CdmDataset): CdmDataset {
        val datasetBuilder = CdmDatasetCS.builder().copyFrom(orgDataset)
        val rootBuilder = datasetBuilder.rootGroup

        // Figure out projection info. Assume the same for all variables
        val lonVar = rootBuilder.findVariable("longitude")
            .orElseThrow { IllegalStateException("Cant find variable longitude") } as VariableDS.Builder<*>
        lonVar.setUnits(CDM.LON_UNITS)
        lonVar.addAttribute(Attribute(_Coordinate.AxisType, AxisType.Lon.toString()))

        val latVar = rootBuilder.findVariableLocal("latitude")
            .orElseThrow { IllegalStateException("Cant find variable latitude") } as VariableDS.Builder<*>

        latVar.setUnits(CDM.LAT_UNITS)
        latVar.addAttribute(Attribute(_Coordinate.AxisType, AxisType.Lat.toString()))

        val projType = latVar.attributeContainer.findAttributeString("projectionType", "LAMBERT_CONFORMAL")!!
        if ("LAMBERT_CONFORMAL" == projType) {
            val proj = makeLCProjection(rootBuilder, latVar)
            makeXYcoords(rootBuilder, proj, latVar, lonVar)
        }

        // figure out the time coordinate for each data variable
        // always separate; could try to discover if they are the same
        for (ncvar in rootBuilder.vbuilders) {
            // variables that are used but not displayable or have no data have DIM_0, also don't want history, since those
            // are just how the person edited the grids
            if (ncvar.rank > 2 && "DIM_0" != ncvar.firstDimensionName
                && !ncvar.shortName.endsWith("History") && !ncvar.shortName.startsWith("Tool")
            ) {
                val timeCoord = createTimeCoordinate(rootBuilder, ncvar as VariableDS.Builder<*>)
                if (timeCoord != null) {
                    datasetBuilder.replaceCoordinateAxis(rootBuilder, timeCoord)
                    info.appendLine(" added time coordinate variable ${timeCoord.shortName}")
                }
            } else if (ncvar.shortName == "Topo") {
                // Deal with Topography variable
                ncvar.addAttribute(Attribute(CDM.LONG_NAME, "Topography"))
                ncvar.addAttribute(Attribute(CDM.UNITS, "ft"))
            }
        }
        return datasetBuilder.build()
    }

    private fun createTimeCoordinate(rootBuilder : Group.Builder, timeVar: VariableDS.Builder<*>) : CoordinateAxis1D.Builder<*>? {
        // Time coordinate is stored in the attribute validTimes
        // One caveat is that the times have two bounds an upper and a lower

        // get the times values
        val timesAtt = timeVar.attributeContainer.findAttribute("validTimes")
        if (timesAtt == null || timesAtt.arrayValues == null) {
            info.appendLine("*** attribute validTimes doesnt exist for time variable ${timeVar.fullName}")
            return null
        }
        var timesArray = timesAtt.arrayValues

        // get every other one
        timesArray = try {
            val n = timesArray!!.size.toInt()
            val sectionb = Section.builder()
            sectionb.appendRange(Range(0, n - 1, 2))
            Arrays.section(timesArray, sectionb.build())
        } catch (e: InvalidRangeException) {
            throw IllegalStateException(e)
        }

        // make sure it matches the dimension
        val dtype = timesArray!!.arrayType
        val nTimesAtt = timesArray!!.size.toInt()

        // create a special dimension and coordinate variable
        val dimTime = timeVar.orgVar.getDimension(0)
        val nTimesDim = dimTime.length
        if (nTimesDim != nTimesAtt) {
            info.appendLine("*** ntimes in attribute ($nTimesAtt) doesnt match dimension length ($nTimesDim) for variable ${timeVar.fullName}")
            return null
        }

        // add the dimension
        val dimName = timeVar.shortName + "_timeCoord"
        val newDim = Dimension(dimName, nTimesDim)
        rootBuilder.addDimension(newDim)

        // add the coordinate variable
        val units = "seconds since 1970-1-1 00:00:00"
        val desc = "time coordinate for " + timeVar.shortName
        val timeCoord = CoordinateAxis1D.builder().setName(dimName).setArrayType(dtype)
            .setParentGroupBuilder(rootBuilder).setDimensionsByName(dimName).setUnits(units).setDesc(desc)
        timeCoord.setSourceData(timesArray)
        timeCoord.addAttribute(Attribute(_Coordinate.AxisType, AxisType.Time.toString()))

        // now make the original variable use the new dimension
        val newDims = ArrayList(timeVar.dimensions)
        newDims[0] = newDim
        timeVar.dimensions = newDims

        // better to explicitly set the coordinate system
        timeVar.addAttribute(Attribute(_Coordinate.Axes, "$dimName yCoord xCoord"))

        // fix the attributes
        var att = timeVar.attributeContainer.findAttribute("fillValue")
        if (att != null) timeVar.addAttribute(Attribute(CDM.FILL_VALUE, att.numericValue))
        att = timeVar.attributeContainer.findAttribute("descriptiveName")
        if (null != att) timeVar.addAttribute(Attribute(CDM.LONG_NAME, att.stringValue))

        return timeCoord
    }

    private fun makeLCProjection(rootBuilder : Group.Builder, projVar : VariableDS.Builder<*>): Projection {
        val latLonOrigin = projVar.getAttributeContainer().findAttributeIgnoreCase("latLonOrigin")
        check(!(latLonOrigin == null || latLonOrigin.isString))
        val centralLon = latLonOrigin.getNumericValue(0)!!.toDouble()
        val centralLat = latLonOrigin.getNumericValue(1)!!.toDouble()
        val par1 = projVar.getAttributeContainer().findAttributeDouble("stdParallelOne", Double.NaN)
        val par2 = projVar.getAttributeContainer().findAttributeDouble("stdParallelTwo", Double.NaN)
        val lc = LambertConformal(centralLat, centralLon, par1, par2)

        // make Coordinate Transform Variable
        val ct = ProjectionCTV("lambertConformalProjection", lc)
        val ctVar = makeCoordinateTransformVariable(ct)
        ctVar.addAttribute(Attribute(_Coordinate.Axes, "xCoord yCoord"))
        rootBuilder.addVariable(ctVar)
        return lc
    }

    @Throws(IOException::class)
    private fun makeXYcoords(rootBuilder : Group.Builder, proj: Projection, latVar: VariableDS.Builder<*>, lonVar: VariableDS.Builder<*>) {

        // lat, lon are 2D with same shape
        val latData = latVar.orgVar.readArray() as Array<Number>
        val lonData = lonVar.orgVar.readArray() as Array<Number>
        val y_dim = latVar.orgVar.getDimension(0)
        val x_dim = latVar.orgVar.getDimension(1)
        val xData = DoubleArray(x_dim.length)
        val yData = DoubleArray(y_dim.length)
        val latlonIndex = latData.index

        // construct x coord
        for (i in 0 until x_dim.length) {
            val lat = latData[latlonIndex.set1(i)].toDouble()
            val lon = lonData[latlonIndex].toDouble()
            val latlon = LatLonPoint(lat, lon)
            val pp = proj.latLonToProj(latlon)
            xData[i] = pp.x()
        }

        // construct y coord
        for (i in 0 until y_dim.length) {
            val lat = latData[latlonIndex.set0(i)].toDouble()
            val lon = lonData[latlonIndex].toDouble()
            val latlon = LatLonPoint(lat, lon)
            val pp = proj.latLonToProj(latlon)
            yData[i] = pp.y()
        }
        val xaxis =
            VariableDS.builder().setName("xCoord").setArrayType(ArrayType.FLOAT).setParentGroupBuilder(rootBuilder)
                .setDimensionsByName(x_dim.shortName).setUnits("km").setDesc("x on projection")
        xaxis.addAttribute(Attribute(CDM.UNITS, "km"))
        xaxis.addAttribute(Attribute(CDM.LONG_NAME, "x on projection"))
        xaxis.addAttribute(Attribute(_Coordinate.AxisType, "GeoX"))
        rootBuilder.addVariable(xaxis)

        val yaxis =
            VariableDS.builder().setName("yCoord").setArrayType(ArrayType.FLOAT).setParentGroupBuilder(rootBuilder)
                .setDimensionsByName(y_dim.shortName).setUnits("km").setDesc("y on projection")
        yaxis.addAttribute(Attribute(CDM.UNITS, "km"))
        yaxis.addAttribute(Attribute(CDM.LONG_NAME, "y on projection"))
        yaxis.addAttribute(Attribute(_Coordinate.AxisType, "GeoY"))
        xaxis.setSourceData(Arrays.factory<Any>(ArrayType.DOUBLE, intArrayOf(x_dim.length), xData))
        yaxis.setSourceData(Arrays.factory<Any>(ArrayType.DOUBLE, intArrayOf(y_dim.length), yData))
        rootBuilder.addVariable(yaxis)
    }

    //////////////////////////////////////////////////////////////////////////////

    override fun identifyZIsPositive(vds: VariableDS): Boolean {
        return true
    }
}

fun isIfpsConventions(ncfile: CdmFile): Boolean {
    // check that file has a latitude and longitude variable, and that latitude has an attribute called projectionType
    val lat = ncfile.findVariable("latitude")
    val lon = ncfile.findVariable("longitude")
    if (lat == null || lon == null) {
        return false
    }
    val geoVarsCheck = lat.findAttributeString("projectionType", null)

    // check that there is a global attribute called fileFormatVersion, and that it has a known value
    var fileFormatCheck = false
    val ff = ncfile.findAttribute("fileFormatVersion")
    if (ff != null && ff.stringValue != null) {
        val ffValue = ff.stringValue
        // two possible values (as of now)
        fileFormatCheck = ffValue.equals("20030117", ignoreCase = true) || ffValue.equals("20010816", ignoreCase = true)
    }

    // both must be true
    return (geoVarsCheck != null) && fileFormatCheck
}