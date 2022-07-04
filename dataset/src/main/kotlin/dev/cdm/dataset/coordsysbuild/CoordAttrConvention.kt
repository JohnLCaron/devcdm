package dev.cdm.dataset.coordsysbuild

import dev.cdm.core.api.Dimension
import dev.cdm.core.constants.AxisType
import dev.cdm.core.constants._Coordinate
import dev.cdm.dataset.api.VariableDS
import java.util.*

// see coord_attr_conv.md
class CoordAttrConvention(val builder: CoordSysBuilder) {

    fun identifyAxisType(vds: VariableDS): AxisType? {
        val coordAxisType = vds.findAttributeString(_Coordinate.AxisType, null)
        return coordAxisType?.let { AxisType.getType(coordAxisType) }
    }

    fun identifyZIsPositive(vds: VariableDS): Boolean? {
        val positive = vds.findAttributeString(_Coordinate.ZisPositive, null)
        if (positive != null) {
            return positive.trim { it <= ' ' }.lowercase() == "up"
        }
        return null
    }

    /** Identify coordinate axes, using _Coordinate.Axes attribute.  */
    fun identifyCoordinateAxes() {
        // A Variable is made into a Coordinate Axis if of these is true:

        // It has any of the _CoordinateAxisType, or _CoordinateZisPositive attributes.
        builder.varList.forEach { vp ->
            if (null != vp.vds.findAttributeString(_Coordinate.AxisType, null) ||
                null != vp.vds.findAttributeString(_Coordinate.ZisPositive, null)
            ) {
                vp.setIsCoordinateAxis()
            }
        }

        // It is listed in a _CoordinateAxes attribute from any variable in the file.
        builder.varList.forEach { vp ->
            val coordinatesAll = vp.vds.findAttributeString(_Coordinate.Axes, null)
            if (coordinatesAll != null) {
                vp.coordinatesAll = coordinatesAll
            }
        }
    }

    fun identifyCoordinateVariables() {
        builder.varList.forEach { vp ->
            var coordVarAlias = vp.vds.findAttributeString(_Coordinate.AliasForDimension, null)
            if (coordVarAlias != null) {
                coordVarAlias = coordVarAlias.trim { it <= ' ' }
                if (vp.vds.rank != 1) {
                    vp.isCoordinateAxis = false
                    builder.info.appendLine("**ERROR Coordinate Variable Alias '${vp.vds.fullName}' has rank ${vp.vds.rank}%n")
                } else {
                    val coordDimOpt = vp.group.findDimension(coordVarAlias)
                    coordDimOpt.ifPresent { coordDim: Dimension ->
                        val vDim = vp.vds.dimensions[0].shortName
                        if (coordDim.shortName != vDim) {
                            vp.isCoordinateAxis = false
                            builder.info.appendLine("**ERROR Coordinate Variable Alias '${vp.vds.fullName}' names wrong dimension '${coordVarAlias}'")
                        } else {
                            vp.setIsCoordinateAxis("Alias '${vp.vds.fullName}' for dimension '${coordVarAlias}'")
                            builder.coordVarsForDimension.put(
                                CoordSysBuilder.DimensionWithGroup(coordDim, vp.group),
                                vp
                            )
                        }
                    }
                }
            }
        }
    }

    /** Identify coordinate systems, using _Coordinate.Systems and _Coordinate.SystemFor attributes.  */
    fun identifyCoordinateSystems() {
        // A variable is a Coordinate System Variable if one of these is true:

        builder.varList.forEach { vp ->
            // It is listed in a _CoordinateSystems attribute from any variable in the file.
            val coordinateSystems: String? = vp.vds.findAttributeString(_Coordinate.Systems, null)
            coordinateSystems?.split(" ")?.forEach { vname ->
                val ref = builder.findVarProcess(vname, vp)
                if (ref != null) {
                    ref.setIsCoordinateSystem("referenced from var '${vp}'")

                } else {
                    builder.info.appendLine("***Cant find CoordinateSystem = '${vname}'; referenced from var '${vp}'")
                }
            }

            // It has a _CoordinateSystemFor attribute.
            val coordinateSystemsFor = vp.vds.findAttributeString(_Coordinate.SystemFor, null)
            if (coordinateSystemsFor != null) {
                vp.setIsCoordinateSystem("coordinateSystemFor")
            }
        }
    }

    /** Identify coordinate transforms, using _Coordinate attributes.  */
    fun identifyCoordinateTransforms() {
        // A variable is a Coordinate Transform Variable if one of these is true:

        // It has any of the _CoordinateTransformType, _CoordinateAxisTypes attributes.
        builder.varList.forEach { vp ->
            if (null != vp.vds.findAttributeString(_Coordinate.TransformType, null) ||
                null != vp.vds.findAttributeString(_Coordinate.AxisTypes, null)
            ) {
                vp.setIsCoordinateTransform()
            }
        }

        builder.varList.forEach { vp ->
            // It is listed in a _CoordinateTransforms attribute from any variable in the file.
            val coordinateTransforms: String? = vp.vds.findAttributeString(_Coordinate.Transforms, null)
            coordinateTransforms?.split(" ")?.forEach { vname ->
                val ref = builder.findVarProcess(vname, vp)
                if (ref != null) {
                    ref.setIsCoordinateTransform("referenced from var '${vp}'")
                } else {
                    builder.info.appendLine("***Cant find CoordinateTransform '${vname}'; referenced from var '${vp}'")
                }
            }
        }
    }

    /** Assign explicit CoordinateSystem objects to variables. */
    fun assignCoordinateSystemsExplicit() {

        // _Coordinate.Systems on a data Variable points to its Coordinate System Variable(s)
        builder.varList.forEach { vp ->
            val coordinateSystems: String? = vp.vds.findAttributeString(_Coordinate.Systems, null)
            if (!vp.isCoordinateTransform) {
                coordinateSystems?.split(" ")?.forEach { vname ->
                    val ap = builder.findVarProcess(vname, vp)
                    if (ap == null) {
                        builder.info.appendLine("***Cant find Coordinate System variable '$vname' referenced from var '$vp'")
                    } else if (ap.cs == null) {
                        builder.info.appendLine("***Not a Coordinate System variable $vname referenced from var '$vp'")
                    } else {
                        val sysName = builder.coords.makeCanonicalName(vp.vds, ap.cs!!.coordAxesNames)
                        vp.assignCoordinateSystem(sysName, "(explicit _Coordinate.Systems)")
                    }
                }
            }
        }

        // _CoordinateSystemFor attribute point to dimensions used by data variables
        builder.varList.forEach { csysVar ->
            val coordinateSystemsFor: String? = csysVar.vds.findAttributeString(_Coordinate.SystemFor, null)
            if (coordinateSystemsFor != null && csysVar.cs != null) {
                val dimSet: MutableSet<String> = HashSet()
                coordinateSystemsFor.split(" ").forEach { dname ->
                    val wantDim: Dimension? = csysVar.group.findDimension(dname).orElse(null)
                    if (wantDim != null) {
                        dimSet.add(wantDim.shortName)
                    } else {
                        builder.info.appendLine("***Cant find Dimension '$dname' referenced from '${csysVar.vds}'")
                        return
                    }
                }

                // look for vars with those dimensions
                builder.varList.forEach { dataVar ->
                    if (dataVar.isData()) {
                        if (dimSet == dataVar.vds.dimensionNamesAll()) {
                            dataVar.coordSysNames.add(csysVar.cs!!.name)
                        }
                    }
                }
            }
        }
    }

    fun assignCoordinateTransforms() {
        // look for transform assignments on the coordinate systems CSV to add to the CSV
        builder.varList.forEach { vp ->
            val coordinateTransforms = vp.vds.findAttributeString(_Coordinate.Transforms, null)
            if (coordinateTransforms != null && vp.isCoordinateSystem && vp.cs != null) {
                coordinateTransforms.split(" ").forEach { vname ->
                    val ap = builder.findVarProcess(vname, vp)
                    if (ap != null) {
                        if (ap.ctv != null) {
                            vp.cs!!.addTransformName(vname)
                            builder.info.appendLine("Assign explicit coordTransform '$vname' to CoordSys '${vp.cs!!.coordAxesNames}'")
                        } else {
                            builder.info.appendLine("***Cant find coordTransform '$vname' with cs referenced from var ${vp.vds.getFullName()}")
                        }
                    } else {
                        builder.info.appendLine(
                            "***Cant find coordTransform variable $vname referenced from var ${vp.vds.getFullName()}"
                        )
                    }
                }
            }
        }

        // look for explicit coordSys assignments on the coordinate transforms (CTV) and assign to CTS
        builder.varList.forEach { vp ->
            val coordinateSystems = vp.vds.findAttributeString(_Coordinate.Systems, null)
            if (vp.isCoordinateTransform && vp.ctv != null && coordinateSystems != null) {
                coordinateSystems.split(" ").forEach { vname ->
                    val cts = builder.findVarProcess(vname, vp)
                    if (cts == null) {
                        builder.info.appendLine("***Cant find coordSystem variable= '$vname' referenced from var '$vp'")
                    } else if (cts.cs == null) {
                        builder.info.appendLine("***Cant find coordSystem variable= '$vname 'referenced from var '$vp'")
                    } else {
                        cts.cs!!.addTransformName(vname)
                        builder.info.appendLine("Assign explicit coordTransform '$vname' to CoordSys '${cts.cs!!.coordAxesNames}'")
                    }
                }
            }
        }

        // look for _CoordinateAxes on the CTV, apply to any Coordinate Systems that contain all these axes
        builder.varList.forEach { vp ->
            val coordAxes = vp.vds.findAttributeString(_Coordinate.Axes, null)
            if (coordAxes != null && vp.isCoordinateTransform && vp.ctv != null) {
                //  look for Coordinate Systems that contain all these axes
                builder.coords.coordSys.forEach { coordSys ->
                    if (builder.coords.containsAxes(coordSys, coordAxes)) {
                        coordSys.addTransformName(vp.ctv!!.name) // TODO
                        coordSys.setProjectionName(vp.ctv!!.name)
                        builder.info.appendLine("Assign (_Coordinate.Axes) coordTransform '${vp.ctv!!.name}' to CoordSys '${coordSys.name}'")
                    }
                }
                // TODO do we need to do both?
                //  look for Coordinate Systems Variables that contain all these axes
                builder.varList.forEach { csv ->
                    if (csv.isCoordinateSystem && csv.cs != null) {
                        if (builder.coords.containsAxisTypes(csv.cs!!, coordAxes)) {
                            csv.cs!!.addTransformName(vp.ctv!!.name) // TODO
                            csv.cs!!.setProjectionName(vp.ctv!!.name)
                            builder.info.appendLine("Assign (_Coordinate.Axes) coordTransform '${vp.ctv!!.name}' to CoordSys '${vp.cs!!.coordAxesNames}'")
                        }
                    }
                }
            }


            // look for _CoordinateAxisTypes on the CTV, apply to any Coordinate Systems that contain all these axes
            builder.varList.forEach { vp ->
                val coordAxisTypes = vp.vds.findAttributeString(_Coordinate.AxisTypes, null)
                if (vp.isCoordinateTransform && vp.ctv != null && coordAxisTypes != null) {
                    //  look for Coordinate Systems that contain all these axes
                    builder.coords.coordSys.forEach { coordSys ->
                        if (builder.coords.containsAxisTypes(coordSys, coordAxisTypes)) {
                            coordSys.addTransformName(vp.ctv!!.name) // TODO
                            coordSys.setProjectionName(vp.ctv!!.name)
                            builder.info.appendLine("Assign (implicit coordAxisType) coordTransform '${vp.ctv!!.name}' to CoordSys '${coordSys.name}'")
                        }
                    }
                    // TODO do we need to do both?
                    //  look for Coordinate Systems Variables that contain all these axes
                    builder.varList.forEach { csv ->
                        if (csv.isCoordinateSystem && csv.cs != null) {
                            if (builder.coords.containsAxisTypes(csv.cs!!, coordAxisTypes)) {
                                csv.cs!!.addTransformName(vp.ctv!!.name) // TODO
                                csv.cs!!.setProjectionName(vp.ctv!!.name)
                                builder.info.appendLine("Assign (implicit coordAxisType) coordTransform '${vp.ctv!!.name}' to CoordSys '${vp.cs!!.coordAxesNames}'")
                            }
                        }
                    }
                }
            }
        }
    }
}