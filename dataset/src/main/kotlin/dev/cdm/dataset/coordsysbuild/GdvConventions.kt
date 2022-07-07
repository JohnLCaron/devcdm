package dev.cdm.dataset.coordsysbuild

import dev.cdm.array.ArrayType
import dev.cdm.core.api.Attribute
import dev.cdm.core.api.Group
import dev.cdm.core.constants.AxisType
import dev.cdm.core.constants._Coordinate
import dev.cdm.dataset.api.CdmDataset
import dev.cdm.dataset.api.CdmDatasetCS
import dev.cdm.dataset.api.CoordinateTransform
import dev.cdm.dataset.api.VariableDS
import dev.cdm.dataset.geoloc.Projection
import dev.cdm.dataset.geoloc.projection.LambertConformal
import dev.cdm.dataset.geoloc.projection.Stereographic
import dev.cdm.dataset.geoloc.projection.TransverseMercator
import java.util.*

open class GdvConventions(name: String = "GDV") : DefaultConventions(name) {
    var projCT : CoordinateTransform? = null

    override fun augment(orgDataset: CdmDataset): CdmDataset {
        projCT = makeProjectionCT(orgDataset.rootGroup)
        if (projCT != null) {
            val datasetBuilder = CdmDatasetCS.builder().copyFrom(orgDataset)

            val vb = makeCoordinateTransformVariable(projCT!!)
            vb.addAttribute(Attribute(_Coordinate.AxisTypes, "${AxisType.GeoY.name} ${AxisType.GeoX.name}"))
            datasetBuilder.rootGroup.addVariable(vb)
            return datasetBuilder.build()
        }
        return orgDataset
    }

    private fun makeProjectionCT(rootGroup : Group): CoordinateTransform? {
        // look for projection in global attribute
        val projection = rootGroup.findAttributeString("projection", null)
        if (null == projection) {
            info.appendLine("GDV Conventions error: NO projection name found %n")
            return null
        }
        var params = rootGroup.findAttributeString("projection_params", null)
        if (null == params) {
            params = rootGroup.findAttributeString("proj_params", null)
        }
        if (null == params) {
            info.appendLine("GDV Conventions error: NO projection parameters found")
            return null
        }

        // parse the parameters
        var count = 0
        val p = DoubleArray(4)
        try {
            // new way : just the parameters
            val stoke = StringTokenizer(params, " ,")
            while (stoke.hasMoreTokens() && count < 4) {
                p[count++] = stoke.nextToken().toDouble()
            }
        } catch (e: NumberFormatException) {
            // old way : every other one
            val stoke = StringTokenizer(params, " ,")
            while (stoke.hasMoreTokens() && count < 4) {
                stoke.nextToken() // skip
                p[count++] = stoke.nextToken().toDouble()
            }
        }
        val proj: Projection
        proj = if (projection.equals("LambertConformal", true))
            LambertConformal(p[0], p[1], p[2], p[3])
        else if (projection.equals("TransverseMercator", true))
            TransverseMercator(p[0], p[1], p[2])
        else if (projection.equals("Stereographic", true) || projection.equals("Oblique_Stereographic", true))
            Stereographic(p[0], p[1], p[2])
        else {
            info.appendLine("GDV Conventions error: Unknown projection $projection")
            return null
        }
        info.appendLine("GDV Conventions add projection $projection")
        return CoordinateTransform(proj.name, proj.projectionAttributes, true)
    }

    /*
        private fun findCoordinateName(axisType: AxisType): String? {
        for (aVlist in rootGroup.vbuilders) {
            val ve = aVlist as VariableDS.Builder<*>
            if (axisType == getAxisType(ve)) {
                return ve.fullName
            }
        }
        return null
    }
     */

    ////////////////////////////////////////////////////////////////////////////////////////////////////

    override fun identifyAxisType(vds: VariableDS): AxisType? {
        val vname = vds.shortName
        if (vname.equals("x", true) ||
            findAlias(vds).equals("x", true))
            return AxisType.GeoX
        if (vname.equals("lon", true) ||
            vname.equals("longitude", true) ||
            findAlias(vds).equals("lon", true))
            return AxisType.Lon
        if (vname.equals("y", true) ||
            findAlias(vds).equals("y", true))
            return AxisType.GeoY
        if (vname.equals("lat", true) ||
            vname.equals("latitude", true) ||
            findAlias(vds).equals("lat", true))
            return AxisType.Lat
        if (vname.equals("lev", true) ||
            findAlias(vds).equals("lev", true) ||
            vname.equals("level", true) ||
            findAlias(vds).equals("level", true))
            return AxisType.Pressure
        if (vname.equals("z", true) ||
            findAlias(vds).equals("z", true) ||
            vname.equals("altitude", true) ||
            vname.equals("depth", true))
            return AxisType.Height
        if (vname.equals("time", true) ||
            findAlias(vds).equals("time", true))
            return AxisType.Time

        return super.identifyAxisType(vds)
    }

    // look for an coord_axis or coord_alias attribute
    private fun findAlias(v: VariableDS): String? {
        var alias = v.findAttributeString("coord_axis", null)
        if (alias == null) {
            alias = v.findAttributeString("coord_alias", "")
        }
        return alias
    }

    override fun identifyCoordinateAxes() {
        for (vp in varList) {
            if (vp.isCoordinateVariable) continue
            if (vp.vds.arrayType == ArrayType.STRUCTURE) continue  // cant be a structure
            val dimName = findAlias(vp.vds)
            if (dimName == null || dimName.isEmpty()) { // none
                continue
            }
            val coordDimOpt = vp.group.findDimension(dimName) // make sure it exists
            if (coordDimOpt.isPresent) {
                vp.isCoordinateAxis = true
                info.appendLine("Coordinate Axis added (GDV alias) $vp' for dimension $dimName")
            }
        }
        super.identifyCoordinateAxes()

        // desperado
        identifyCoordinateAxesForce()
    }

    private fun identifyCoordinateAxesForce() {
        val map = HashMap<AxisType, VarProcess>()

        // find existing axes, so we dont duplicate
        for (vp in varList) {
            if (vp.isCoordinateAxis) {
                val atype = identifyAxisType(vp.vds)
                if (atype != null) map[atype] = vp
            }
        }

        // look for variables to turn into axes
        for (vp in varList) {
            if (vp.isCoordinateVariable) continue
            if (vp.vds.arrayType == ArrayType.STRUCTURE) continue  // cant be a structure
            val atype = identifyAxisType(vp.vds)
            if (atype != null) {
                if (map[atype] == null) {
                    vp.isCoordinateAxis = true
                    info.appendLine(" Coordinate Axis added (GDV forced) = $vp  for axis $atype")
                }
            }
        }
    }

    override fun makeCoordinateTransforms() {
        if (projCT != null) {
            coords.addCoordinateTransform(projCT!!)
        }
        super.makeCoordinateTransforms()
    }
}