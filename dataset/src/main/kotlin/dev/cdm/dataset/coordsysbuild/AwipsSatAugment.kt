package dev.cdm.dataset.coordsysbuild

import dev.cdm.array.ArrayType
import dev.cdm.core.api.Attribute
import dev.cdm.core.constants.AxisType
import dev.cdm.core.constants.CDM
import dev.cdm.core.constants._Coordinate
import dev.cdm.dataset.api.CdmDataset
import dev.cdm.dataset.api.CoordinateAxis
import dev.cdm.dataset.api.CoordinateAxis1D
import dev.cdm.dataset.geoloc.LatLonPoint
import dev.cdm.dataset.geoloc.ProjectionPoint
import dev.cdm.dataset.geoloc.projection.EquidistantCylindrical
import dev.cdm.dataset.geoloc.projection.LambertConformal
import dev.cdm.dataset.geoloc.projection.Mercator
import dev.cdm.dataset.transform.horiz.ProjectionCTV

class AwipsSatAugment(orgDataset: CdmDataset, info : StringBuilder) : AwipsAugment(orgDataset, info) {

    override fun augment(): CdmDataset {
        val nx: Int = rootBuilder.findDimension("x").map {it.length}
            .orElseThrow { RuntimeException("missing dimension x") }
        val ny: Int = rootBuilder.findDimension("y").map {it.length}
            .orElseThrow { RuntimeException("missing dimension y") }
        val projName = rootBuilder.getAttributeContainer().findAttributeString("projName", "none")!!
        if (projName.equals("LAT_LON", ignoreCase = true)) {
            makeLatLonProjection(nx, ny)
            datasetBuilder.replaceCoordinateAxis(rootBuilder, makeLonCoordAxis(nx,"lat"))
            datasetBuilder.replaceCoordinateAxis(rootBuilder, makeLatCoordAxis(ny,"lon"))
        } else {
            if (projName.equals("CYLINDRICAL_EQUIDISTANT", ignoreCase = true)) projCT = makeCEProjection(projName, nx, ny)
            if (projName.equals("LAMBERT_CONFORMAL", ignoreCase = true)) projCT = makeLCProjection(projName, nx, ny)
            if (projName.equals("MERCATOR", ignoreCase = true)) projCT = makeMercatorProjection(projName, nx, ny)
            datasetBuilder.replaceCoordinateAxis(rootBuilder, makeXCoordAxis("x"))
            datasetBuilder.replaceCoordinateAxis(rootBuilder, makeYCoordAxis("y"))
        }

        // long_name; TODO: not sure of units
        val datav = rootBuilder.findVariableLocal("image")
            .orElseThrow { RuntimeException("must have varible 'image'") }
        val long_name = rootBuilder.getAttributeContainer().findAttributeString("channel", null)
        if (null != long_name) {
            datav.addAttribute(Attribute(CDM.LONG_NAME, long_name))
        }
        /*
        datav.setArrayType(ArrayType.UBYTE);
        datav.addAttribute(
            Attribute.builder(CDM.MISSING_VALUE).setArrayType(ArrayType.UBYTE).setValues(listOf<Byte>(0, -127), true).build()
        )
         */
        // missing values
        datav.addAttribute(
            Attribute.builder(CDM.MISSING_VALUE).setArrayType(ArrayType.BYTE).setValues(listOf<Byte>(0, -127), false).build()
        )
        if (projCT != null) {
            val v = makeCoordinateTransformVariable(projCT!!)
            v.addAttribute(Attribute(_Coordinate.Axes, "x y"))
            rootBuilder.addVariable(v)
        }

        return datasetBuilder.build()
    }

    private fun makeLatLonProjection(nx: Int, ny: Int) {
        val lat0 = findAttributeDouble("lat00")
        val lon0 = findAttributeDouble("lon00")
        val latEnd = findAttributeDouble("latNxNy")
        var lonEnd = findAttributeDouble("lonNxNy")
        if (lonEnd < lon0) {
            lonEnd += 360.0
        }
        startx = lon0
        starty = lat0
        dx = (lonEnd - lon0) / nx
        dy = (latEnd - lat0) / ny
    }

    // see TestEquidistantCylindrical
    private fun makeCEProjection(name: String, nx: Int, ny: Int): ProjectionCTV {
        val centralLat = findAttributeDouble("centralLat")
        val centralLon = findAttributeDouble("centralLon")

        // lat0, lon0, par1, par2
        val proj = EquidistantCylindrical(centralLat, centralLon, centralLat)

        val lat00 = findAttributeDouble("lat00")
        val lon00 = findAttributeDouble("lon00")
        val latNxNy = findAttributeDouble("latNxNy")
        val lonNxNy = findAttributeDouble("lonNxNy")

        val startp = proj.latLonToProj(lat00, lon00)!!
        val endp = proj.latLonToProj(latNxNy, lonNxNy)!!

        this.startx = startp.x()
        this.starty = startp.y()
        val endx = endp.x()
        val endy = endp.y()

        this.dx = (endx - startx) / (nx - 1)
        this.dy = (endy - starty) / (ny - 1)

        return ProjectionCTV(name, proj)
    }

    private fun makeLCProjection(name: String, nx: Int, ny: Int): ProjectionCTV {
        val centralLat = findAttributeDouble("centralLat")
        val centralLon = findAttributeDouble("centralLon")
        val rotation = findAttributeDouble("rotation")

        // lat0, lon0, par1, par2
        val proj = LambertConformal(rotation, centralLon, centralLat, centralLat)
        // we have to project in order to find the origin
        val lat0 = findAttributeDouble("lat00")
        val lon0 = findAttributeDouble("lon00")
        val start = proj.latLonToProj(LatLonPoint(lat0, lon0))
        if (debugProj) info.appendLine("getLCProjection start at proj coord $start")
        startx = start.x()
        starty = start.y()

        // we will use the end to compute grid size
        val latN = findAttributeDouble("latNxNy")
        val lonN = findAttributeDouble("lonNxNy")
        val end = proj.latLonToProj(LatLonPoint(latN, lonN))
        dx = (end.x() - startx) / nx
        dy = (end.y() - starty) / ny
        if (debugProj) {
            info.appendLine("  makeProjectionLC start at proj coord $startx $starty")
            info.appendLine("  makeProjectionLC end at proj coord ${end.x()} ${end.y()}")
            val fdx = findAttributeDouble("dxKm")
            val fdy = findAttributeDouble("dyKm")
            info.appendLine("  makeProjectionLC calc dx= $dx file_dx= $fdx")
            info.appendLine("  makeProjectionLC calc dy= $dy file_dy= $fdy")
        }
        return ProjectionCTV(name, proj)
    }

    @Throws(NoSuchElementException::class)
    private fun makeMercatorProjection(name: String, nx: Int, ny: Int): ProjectionCTV? {
        // double centralLat = findAttributeDouble( ds, "centralLat");
        // Center longitude for the mercator projection, where the mercator projection is parallel to the Earth's surface.
        // from this, i guess is actually transverse mercator
        // double centralLon = findAttributeDouble( ds, "centralLon");
        // lat0, central meridian, scale factor
        // TransverseMercator proj = new TransverseMercator(centralLat, centralLon, 1.0);
        val latDxDy = findAttributeDouble("latDxDy")
        val lonDxDy = findAttributeDouble("lonDxDy")

        // lat0, lon0, par
        val proj = Mercator(lonDxDy, latDxDy)

        // we have to project in order to find the start
        val lat0 = findAttributeDouble("lat00")
        val lon0 = findAttributeDouble("lon00")
        val start = proj.latLonToProj(LatLonPoint(lat0, lon0))
        startx = start.x()
        starty = start.y()

        // we will use the end to compute grid size
        val latN = findAttributeDouble("latNxNy")
        val lonN = findAttributeDouble("lonNxNy")
        val end = proj.latLonToProj(LatLonPoint(latN, lonN))
        dx = (end.x() - startx) / nx
        dy = (end.y() - starty) / ny
        if (debugProj) {
            info.appendLine("  makeProjectionMercator start at proj coord $startx $starty")
            info.appendLine("  makeProjectionMercator end at proj coord ${end.x()} ${end.y()}")
            val fdx = findAttributeDouble("dxKm")
            val fdy = findAttributeDouble("dyKm")
            info.appendLine("  makeProjectionMercator calc dx= $dx file_dx= $fdx")
            info.appendLine("  makeProjectionMercator calc dy= $dy file_dy= $fdy")
        }
        return ProjectionCTV(name, proj)
    }

    private fun makeLonCoordAxis(xname: String): CoordinateAxis.Builder<*>? {
        val v = CoordinateAxis1D.builder().setName(xname).setArrayType(ArrayType.DOUBLE)
            .setParentGroupBuilder(rootBuilder).setDimensionsByName(xname).setUnits(CDM.LON_UNITS).setDesc("longitude")
        v.addAttribute(Attribute(_Coordinate.AxisType, AxisType.Lon.toString()))
        v.setAutoGen(startx, dx)
        info.appendLine("Created Lon Coordinate Axis = $xname")
        return v
    }

    private fun makeLatCoordAxis(yname: String): CoordinateAxis.Builder<*>? {
        val v = CoordinateAxis1D.builder().setName(yname).setArrayType(ArrayType.DOUBLE)
            .setParentGroupBuilder(rootBuilder).setDimensionsByName(yname).setUnits(CDM.LAT_UNITS).setDesc("latitude")
        v.addAttribute(Attribute(_Coordinate.AxisType, AxisType.Lat.toString()))
        v.setAutoGen(starty, dy)
        info.appendLine("Created Lat Coordinate Axis = $yname")
        return v
    }
}
