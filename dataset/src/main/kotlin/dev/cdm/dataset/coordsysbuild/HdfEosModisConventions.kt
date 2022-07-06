package dev.cdm.dataset.coordsysbuild

import dev.cdm.array.ArrayType
import dev.cdm.core.api.*
import dev.cdm.core.calendar.CalendarDate
import dev.cdm.core.constants.*
import dev.cdm.core.iosp.IospUtils
import dev.cdm.dataset.api.*
import dev.cdm.dataset.geoloc.projection.Sinusoidal
import dev.cdm.dataset.transform.horiz.ProjectionCTV

private  val  CRS = "sinusoidal"
private  val  DATA_GROUP = "Data_Fields"
private  val  DIMX_NAME = "XDim"
private  val  DIMY_NAME = "YDim"
private  val  TIME_NAME = "time"
private  val  CONVENTION_NAME = "HDF4-EOS-MODIS"

open class HdfEosModisConventions(name: String = CONVENTION_NAME) : CFConventions(name) {
    private var addTimeCoord : Boolean = false
    private var projCTV : ProjectionCTV? = null

    override fun augment(orgDataset: CdmDataset): CdmDataset {
        val datasetBuilder = CdmDatasetCS.builder().copyFrom(orgDataset)
        addTimeCoord = addTimeCoordinate(datasetBuilder)

        augmentGroup(datasetBuilder, datasetBuilder.rootGroup)
        return datasetBuilder.build()
    }

    private fun addTimeCoordinate(datasetBuilder : CdmDatasetCS.Builder<*>): Boolean {
        val rootBuilder = datasetBuilder.rootGroup

        // add time coordinate by parsing the filename, of course!
        val ncd: CdmFile = datasetBuilder.orgFile ?: return false
        val cd = parseFilenameForDate(ncd.location) ?: return false
        rootBuilder.addAttribute(Attribute("_MODIS_Date", cd.toString()))

        // add the time dimension
        rootBuilder.addDimension(Dimension(TIME_NAME, 1))

        // add the coordinate variable
        val units = "seconds since $cd"
        val timeCoord: CoordinateAxis.Builder<*> =
            CoordinateAxis1D.builder()
                .setName(TIME_NAME)
                .setArrayType(ArrayType.DOUBLE)
                .setParentGroupBuilder(rootBuilder)
                .setDimensionsByName("")
                .setUnits(units)
                .setDesc("time coordinate")
        timeCoord.setAutoGen(0.0, 0.0)
        timeCoord.addAttribute(Attribute(_Coordinate.AxisType, AxisType.Time.toString()))
        datasetBuilder.replaceCoordinateAxis(rootBuilder, timeCoord)
        return true
    }

    private fun parseFilenameForDate(filename: String): CalendarDate? {
        // filename MOD13Q1.A2000065.h11v04.005.2008238031620.hdf
        val tokes = filename.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        if (tokes.size < 2) {
            return null
        }
        if (tokes[1].length < 8) {
            return null
        }
        val want = tokes[1]
        val yearS = want.substring(1, 5)
        val jdayS = want.substring(5, 8)
        return try {
            val year = yearS.toInt()
            val jday = jdayS.toInt()
            CalendarDate.ofDoy(year, jday, 0, 0, 0, 0)
        } catch (e: Exception) {
            null
        }
    }

    private fun augmentGroup(datasetBuilder : CdmDatasetCS.Builder<*>, g: Group.Builder) {
        val crs = g.findVariableLocal(IospUtils.HDFEOS_CRS)
        if (crs.isPresent) {
            augmentGroupWithProjectionInfo(datasetBuilder, g)
        }
        for (nested in g.gbuilders) {
            augmentGroup(datasetBuilder, nested)
        }
    }

    // TODO this is non standard for adding projection, does it work?
    private fun augmentGroupWithProjectionInfo(datasetBuilder : CdmDatasetCS.Builder<*>, g: Group.Builder) {
        val dataGopt = g.findGroupLocal(DATA_GROUP)
        if (dataGopt.isEmpty) {
            return
        }
        val dataG = dataGopt.get()
        val dimXopt = dataG.findDimensionLocal(DIMX_NAME)
        val dimYopt = dataG.findDimensionLocal(DIMY_NAME)
        if (dimXopt.isEmpty || dimYopt.isEmpty) {
            return
        }
        val dimX = dimXopt.get()
        val dimY = dimYopt.get()
        g.findVariableLocal(IospUtils.HDFEOS_CRS).ifPresent { crs: Variable.Builder<*> ->
            val projAtt = crs.attributeContainer.findAttributeString(IospUtils.HDFEOS_CRS_Projection, "")
            val upperLeft = crs.attributeContainer.findAttribute(IospUtils.HDFEOS_CRS_UpperLeft)
            val lowerRight = crs.attributeContainer.findAttribute(IospUtils.HDFEOS_CRS_LowerRight)
            if (upperLeft == null || lowerRight == null) {
                return@ifPresent
            }
            val minX = upperLeft.getNumericValue(0)!!.toDouble()
            val minY = upperLeft.getNumericValue(1)!!.toDouble()
            val maxX = lowerRight.getNumericValue(0)!!.toDouble()
            val maxY = lowerRight.getNumericValue(1)!!.toDouble()
            var hasProjection = false
            var coordinates: String? = null
            if (projAtt == "GCTP_SNSOID") {
                hasProjection = true
                val projParams = crs.attributeContainer.findAttribute(IospUtils.HDFEOS_CRS_ProjParams)
                check(projParams != null) {"Cant find attribute " + IospUtils.HDFEOS_CRS_ProjParams}

                projCTV = makeSinusoidalProjection(CRS, projParams)
                val crss = makeCoordinateTransformVariable(projCTV!!)
                crss.addAttribute(Attribute(_Coordinate.AxisTypes, "GeoX GeoY"))
                // LOOK addVariable CTV shouldne be needed ??
                dataG.addVariable(crss)
                // LOOK replace needed ??
                datasetBuilder.replaceCoordinateAxis(dataG, makeCoordAxis(dataG, DIMX_NAME, dimX.length, minX, maxX, true))
                datasetBuilder.replaceCoordinateAxis(dataG, makeCoordAxis(dataG, DIMY_NAME, dimY.length, minY, maxY, false))
                coordinates = if (addTimeCoord) TIME_NAME + " " + DIMX_NAME + " " + DIMY_NAME else DIMX_NAME + " " + DIMY_NAME

            } else if (projAtt == "GCTP_GEO") {
                datasetBuilder.replaceCoordinateAxis(dataG, makeLatLonCoordAxis(dataG, dimX.length, minX * 1e-6, maxX * 1e-6, true))
                datasetBuilder.replaceCoordinateAxis(dataG, makeLatLonCoordAxis(dataG, dimY.length, minY * 1e-6, maxY * 1e-6, false))
                coordinates = if (addTimeCoord) TIME_NAME + " Lat Lon" else "Lat Lon"
            }
            for (v in dataG.vbuilders) {
                if (v.rank != 2) {
                    continue
                }
                if (v.getDimensionName(0) != dimY.shortName) {
                    continue
                }
                if (v.getDimensionName(1) != dimX.shortName) {
                    continue
                }
                if (coordinates != null) {
                    v.addAttribute(Attribute(CF.COORDINATES, coordinates))
                }
                if (hasProjection) {
                    v.addAttribute(Attribute(CF.GRID_MAPPING, CRS))
                }
            }
        }
    }

    /*
   * The UpperLeftPointMtrs is in projection coordinates, and identifies the very upper left corner of the upper left
   * pixel of the image data
   * • The LowerRightMtrs identifies the very lower right corner of the lower right pixel of the image data. These
   * projection coordinates are the only metadata that accurately reflect the extreme corners of the gridded image
   */
    private fun makeCoordAxis(dataG: Group.Builder, name: String, n: Int, start: Double, end: Double, isX: Boolean): CoordinateAxis.Builder<*>? {
        val vb: CoordinateAxis.Builder<*> =
            CoordinateAxis1D.builder().setName(name).setArrayType(ArrayType.DOUBLE).setParentGroupBuilder(dataG)
                .setDimensionsByName(name).setUnits("km").setDesc(if (isX) "x coordinate" else "y coordinate")
        val incr = (end - start) / n
        vb.setAutoGen(start * .001, incr * .001) // km
        vb.addAttribute(Attribute(_Coordinate.AxisType, if (isX) AxisType.GeoX.name else AxisType.GeoY.name))
        return vb
    }

    private fun makeLatLonCoordAxis(
        dataG: Group.Builder, n: Int, start: Double, end: Double,
        isLon: Boolean
    ): CoordinateAxis.Builder<*>? {
        val name = if (isLon) AxisType.Lon.toString() else AxisType.Lat.toString()
        val dimName = if (isLon) DIMX_NAME else DIMY_NAME
        val v: CoordinateAxis.Builder<*> = CoordinateAxis1D.builder().setName(name).setArrayType(ArrayType.DOUBLE)
            .setParentGroupBuilder(dataG).setDimensionsByName(dimName)
            .setUnits(if (isLon) "degrees_east" else "degrees_north")
        val incr = (end - start) / n
        v.setAutoGen(start, incr)
        v.addAttribute(Attribute(_Coordinate.AxisType, name))
        return v
    }

    private fun makeSinusoidalProjection(name: String, projParams: Attribute): ProjectionCTV {
        val radius = projParams.getNumericValue(0)!!.toDouble()
        val centMer = projParams.getNumericValue(4)!!.toDouble()
        val falseEast = projParams.getNumericValue(6)!!.toDouble()
        val falseNorth = projParams.getNumericValue(7)!!.toDouble()
        val proj = Sinusoidal(centMer, falseEast * .001, falseNorth * .001, radius * .001)
        return ProjectionCTV(name, proj)
    }

    ///////////////////////////////////////////

    override fun makeTransformBuilder(vb: VariableDS, isProjection : Boolean): CoordinateTransform? {
        return CoordinateTransform(projCTV!!)
    }
}

fun isHdfEosModis(ncfile: CdmFile): Boolean {
    if (ncfile.cdmFileTypeId == null || ncfile.cdmFileTypeId != "HDF-EOS2") {
        return false
    }
    val typeName = ncfile.attributes().findAttributeString(CF.FEATURE_TYPE, null) ?: return false
    return if (typeName != FeatureType.GRID.toString() && typeName != FeatureType.SWATH.toString()) {
        false
    } else checkGroup(ncfile.rootGroup)
}

private fun checkGroup(g: Group): Boolean {
    val crs = g.findVariableLocal(IospUtils.HDFEOS_CRS)
    val dataG = g.findGroupLocal(DATA_GROUP)
    if (crs != null && dataG != null) {
        val att = crs.findAttributeString(IospUtils.HDFEOS_CRS_Projection, null) ?: return false
        return if (att != "GCTP_SNSOID" && att != "GCTP_GEO") {
            false
        } else !(dataG.findDimensionLocal(DIMX_NAME) == null || dataG.findDimensionLocal(
            DIMY_NAME
        ) == null)
    }
    for (ng in g.groups) {
        if (checkGroup(ng)) {
            return true
        }
    }
    return false
}