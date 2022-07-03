package dev.cdm.dataset.coordsysbuild

import dev.cdm.array.ArrayType
import dev.cdm.core.api.Group
import dev.cdm.core.constants.AxisType
import dev.cdm.dataset.api.CdmDataset
import dev.cdm.dataset.api.VariableDS
import dev.cdm.dataset.geoloc.Projection
import dev.cdm.dataset.geoloc.projection.LambertConformal
import dev.cdm.dataset.geoloc.projection.Stereographic
import dev.cdm.dataset.geoloc.projection.TransverseMercator
import dev.cdm.dataset.transform.horiz.ProjectionCTV
import java.util.*

open class GdvConventions(name: String = "GDV") : DefaultConventions(name) {
    var projCT : ProjectionCTV? = null
    
    override fun augment(orgDataset: CdmDataset): CdmDataset {
        projCT = makeProjectionCT(orgDataset.rootGroup)
        if (projCT != null) {
            // LOOK we dont really have to add a CTV do we ?
            /* val vb = makeCoordinateTransformVariable(projCT!!)
            rootGroup.addVariable(vb)
            val xname = findCoordinateName(AxisType.GeoX)
            val yname = findCoordinateName(AxisType.GeoY)
            if (xname != null && yname != null) {
                vb.addAttribute(Attribute(_Coordinate.Axes, "$xname $yname"))
            }

             */
        }
        return orgDataset
    }

    private fun makeProjectionCT(rootGroup : Group): ProjectionCTV? {
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
        info.appendLine("GDV Conventions projection $projection params = ${p}")
        val proj: Projection
        proj = if (projection.equals("LambertConformal", ignoreCase = true)) 
            LambertConformal(p[0], p[1], p[2], p[3])
        else if (projection.equals("TransverseMercator", ignoreCase = true)) 
            TransverseMercator(p[0], p[1], p[2])
        else if (projection.equals("Stereographic", ignoreCase = true) || projection.equals("Oblique_Stereographic", ignoreCase = true))
            Stereographic(p[0], p[1], p[2])
        else {
            info.appendLine("GDV Conventions error: Unknown projection $projection")
            return null
        }
        return ProjectionCTV(proj.className, proj)
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
        if (vname.equals("x", ignoreCase = true) || 
            findAlias(vds).equals("x", ignoreCase = true)) 
            return AxisType.GeoX
        if (vname.equals("lon", ignoreCase = true) || 
            vname.equals("longitude", ignoreCase = true) || 
            findAlias(vds).equals("lon", ignoreCase = true))
            return AxisType.Lon
        if (vname.equals("y", ignoreCase = true) || 
            findAlias(vds).equals("y", ignoreCase = true)) 
            return AxisType.GeoY
        if (vname.equals("lat", ignoreCase = true) || 
            vname.equals("latitude", ignoreCase = true) || 
            findAlias(vds).equals("lat", ignoreCase = true)) 
            return AxisType.Lat
        if (vname.equals("lev", ignoreCase = true) || 
            findAlias(vds).equals("lev", ignoreCase = true) || 
            vname.equals("level", ignoreCase = true) || 
            findAlias(vds).equals("level", ignoreCase = true))
            return AxisType.GeoZ
        if (vname.equals("z", ignoreCase = true) || 
            findAlias(vds).equals("z", ignoreCase = true) || 
            vname.equals("altitude", ignoreCase = true) || 
            vname.equals("depth", ignoreCase = true))
            return AxisType.Height
        if (vname.equals("time", ignoreCase = true) || 
            findAlias(vds).equals("time", ignoreCase = true)) 
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
            val vp = findVarProcess(projCT!!.getName(), null)
            if (vp != null) {
                vp.isCoordinateTransform = true
                vp.ctv = projCT
            }
        }
        super.makeCoordinateTransforms()
    }
}