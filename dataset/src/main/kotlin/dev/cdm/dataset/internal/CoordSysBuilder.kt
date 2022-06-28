package dev.cdm.dataset.internal

import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.Multimap
import dev.cdm.core.api.Attribute
import dev.cdm.core.api.Dimension
import dev.cdm.core.api.Group
import dev.cdm.core.constants.AxisType
import dev.cdm.core.constants.CF
import dev.cdm.core.constants._Coordinate
import dev.cdm.dataset.api.CdmDatasetCS
import dev.cdm.dataset.api.CoordinateAxis
import dev.cdm.dataset.api.CoordinateSystem
import dev.cdm.dataset.api.VariableDS
import dev.cdm.dataset.internal.CoordSystemBuilder.isCoordinateVariable
import dev.cdm.dataset.transform.horiz.ProjectionCTV
import java.util.*

abstract class CoordSysBuilder(datasetBuilder: CdmDatasetCS.Builder<*>) {
    internal val root: Group.Builder = datasetBuilder.rootGroup
    internal val coords: CoordinatesHelper.Builder = datasetBuilder.coords
    internal val varList = mutableListOf<VarProcess>()
    internal val coordVarsForDimension: Multimap<DimensionWithGroup, VarProcess> = ArrayListMultimap.create()
    internal var conventionName = _Coordinate.Convention
    internal val info = StringBuilder()

    abstract fun identifyAxisType(vds: VariableDS.Builder<*>): AxisType?

    open fun identifyCoordinateAxes() {
        varList.forEach { vp ->
            if (vp.coordinateAxes != null) {
                identifyCoordinateAxes(vp, vp.coordinateAxes!!)
            }
            if (vp.coordinates != null) {
                identifyCoordinateAxes(vp, vp.coordinates!!)
            }
        }
    }

    // Use coordinates string to mark variables as axes
    private fun identifyCoordinateAxes(vp: VarProcess, coordinates: String) {
        coordinates.split(" ").forEach { vname ->
            var ap = findVarProcess(vname, vp)
            if (ap == null) {
                val gb = vp.vb.parentGroupBuilder
                val vopt = gb.findVariableOrInParent(vname)
                if (vopt.isPresent) {
                    ap = findVarProcess(vopt.get().fullName, vp)
                } else {
                    info.appendLine("***Cant find coordAxis ${vname} referenced from var= ${vp}")
                }
            }
            if (ap != null) {
                if (!ap.isCoordinateAxis) {
                    info.appendLine(" CoordinateAxis = ${vname} added; referenced from var= ${vp}")
                }
                ap.isCoordinateAxis = true
            } else {
                info.appendLine("***Cant find coordAxis ${vname} referenced from var= ${vp}")
            }
        }
    }

    /** Identify coordinate systems, using _Coordinate.Systems attribute.  */
    open fun identifyCoordinateSystems() {
        varList.forEach { vp ->
            if (vp.coordinateSystems != null) {
                vp.coordinateSystems!!.split(" ").forEach { vname ->
                    val ap = findVarProcess(vname, vp)
                    if (ap != null) {
                        if (!ap.isCoordinateSystem) {
                            info.appendLine(" CoordinateSystem = ${vname} added; referenced from var= ${vp}")
                        }
                        ap.isCoordinateSystem = true
                    } else {
                        info.appendLine("***Cant find CoordinateSystem = ${vname}; referenced from var= ${vp}")
                    }
                }
            }
        }
    }

    /** Identify coordinate transforms, using _CoordinateTransforms attribute.  */
    open fun identifyCoordinateTransforms() {
        varList.forEach { vp ->
            if (vp.coordinateTransforms != null) {
                vp.coordinateTransforms!!.split(" ").forEach { vname ->
                    val ap = findVarProcess(vname, vp)
                    if (ap != null) {
                        if (!ap.isCoordinateTransform) {
                            info.appendLine(" CoordinateTransform = ${vname} added; referenced from var= ${vp}")
                        }
                        ap.isCoordinateTransform = true
                    } else {
                        info.appendLine("***Cant find CoordinateTransform = ${vname}; referenced from var= ${vp}")
                    }
                }
            }
        }
    }

    /**
     * Take previously identified Coordinate Axis and Coordinate Variables and make them into a CoordinateAxis.
     */
    open fun makeCoordinateAxes() {
        // The ones identified as coordinate variables or axes
        varList.forEach { vp ->
            if (vp.isCoordinateAxis || vp.isCoordinateVariable) {
                if (vp.axisType == null) {
                    vp.axisType = identifyAxisType(vp.vb)
                }
                if (vp.axisType == null) {
                    info.appendLine("Coordinate Axis ${vp} does not have an assigned AxisType")
                }
                vp.makeIntoCoordinateAxis()
            }
        }

        // The ones marked as Coordinate Systems, which will reference Coordinates
        varList.forEach { vp ->
            if (vp.isCoordinateSystem) {
                vp.makeCoordinatesFromCoordinateSystem()
            }
        }
    }

    open fun makeCoordinateSystems() {
        varList.forEach { vp ->
            if (vp.isCoordinateSystem) {
                vp.makeCoordinateSystem()
            }
        }
    }

    fun findVarProcess(name: String?, from: VarProcess?): VarProcess? {
        if (name == null) {
            return null
        }

        // search on vb full name
        varList.find { name == it.vb.fullName }?.let { return it }

        // prefer ones in the same group with short name
        if (from != null) {
            varList.find { name == it.vb.shortName && it.vb.parentGroupBuilder == from.vb.parentGroupBuilder }
                ?.let { return it }
        }

        // WAEF, use short name from anywhere
        varList.find { name == it.vb.shortName }?.let { return it }
        return null
    }

    internal data class DimensionWithGroup(val dim: Dimension, val group: Group.Builder)

    /** Classifications of Variables into axis, systems and transforms  */
    inner class VarProcess(val gb: Group.Builder, val vb: VariableDS.Builder<*>) {
        val coordSysNames = ArrayList<String>()

        // attributes
        var coordVarAlias: String? // _Coordinate.AliasForDimension
        var positive: String? // _Coordinate.ZisPositive or CF.POSITIVE
        var coordinateAxes: String? // _Coordinate.Axes
        var coordinateSystems: String? // _Coordinate.Systems
        var coordinateSystemsFor: String? // _Coordinate.SystemsFor
        var coordinateTransforms: String? // _Coordinate.Transforms
        var coordAxisTypes: String? // _Coordinate.AxisTypes
        var coordTransformType: String? // _Coordinate.TransformType
        var coordinates: String? = null // CF coordinates (set by subclasses)

        // coord axes
        var isCoordinateVariable = false
        var isCoordinateAxis = false
        var axisType: AxisType? = null
        var axis: CoordinateAxis.Builder<*>? = null

        // coord systems
        var isCoordinateSystem: Boolean
        var cs: CoordinateSystem.Builder<*>? = null

        // coord transform
        var isCoordinateTransform: Boolean
        var ctv: ProjectionCTV? = null

        /** Wrap the given variable. Identify Coordinate Variables. Process all _Coordinate attributes.  */
        init {
            if (vb.parentGroupBuilder == null) {
                if (vb.parentStructureBuilder != null) {
                    vb.parentGroupBuilder = vb.parentStructureBuilder.parentGroupBuilder // ??
                }
            }
            isCoordinateVariable = isCoordinateVariable(vb)
            if (isCoordinateVariable) {
                coordVarsForDimension.put(DimensionWithGroup(vb.dimensions[0], gb), this)
            }
            val att = vb.attributeContainer.findAttributeIgnoreCase(_Coordinate.AxisType)
            if (att != null) {
                val axisName = att.stringValue
                axisType = AxisType.getType(axisName)
                isCoordinateAxis = true
                info.appendLine("Coordinate Axis added = ${vb.fullName} type= ${axisName}")
            }
            coordVarAlias = vb.attributeContainer.findAttributeString(_Coordinate.AliasForDimension, null)
            if (coordVarAlias != null) {
                coordVarAlias = coordVarAlias!!.trim { it <= ' ' }
                if (vb.rank != 1) {
                    info.appendLine("**ERROR Coordinate Variable Alias ${vb.fullName} has rank ${vb.rank}%n")
                } else {
                    val coordDimOpt = gb.findDimension(coordVarAlias)
                    coordDimOpt.ifPresent { coordDim: Dimension ->
                        val vDim = vb.firstDimensionName
                        if (coordDim.shortName != vDim) {
                            info.appendLine("**ERROR Coordinate Variable Alias ${vb.fullName} names wrong dimension ${coordVarAlias}")
                        } else {
                            isCoordinateAxis = true
                            coordVarsForDimension.put(DimensionWithGroup(coordDim, gb), this)
                            info.appendLine(" Coordinate Variable Alias added = ${vb.fullName} for dimension= ${coordVarAlias}")
                        }
                    }
                }
            }
            positive = vb.attributeContainer.findAttributeString(_Coordinate.ZisPositive, null)
            if (positive == null) {
                positive = vb.attributeContainer.findAttributeString(CF.POSITIVE, null)
            } else {
                isCoordinateAxis = true
                positive = positive!!.trim { it <= ' ' }
                info.appendLine(" Coordinate Axis added(from positive attribute ) = ${vb.fullName} for dimension= ${coordVarAlias}")
            }
            coordinateAxes = vb.attributeContainer.findAttributeString(_Coordinate.Axes, null)
            coordinateSystems = vb.attributeContainer.findAttributeString(_Coordinate.Systems, null)
            coordinateSystemsFor = vb.attributeContainer.findAttributeString(_Coordinate.SystemFor, null)
            coordinateTransforms = vb.attributeContainer.findAttributeString(_Coordinate.Transforms, null)
            isCoordinateSystem = coordinateTransforms != null || coordinateSystemsFor != null
            coordAxisTypes = vb.attributeContainer.findAttributeString(_Coordinate.AxisTypes, null)
            coordTransformType = vb.attributeContainer.findAttributeString(_Coordinate.TransformType, null)
            isCoordinateTransform = coordTransformType != null || coordAxisTypes != null
        }

        fun isData(): Boolean =
            !isCoordinateVariable && !isCoordinateAxis && !isCoordinateSystem && !isCoordinateTransform

        fun maybeData(): Boolean = !isCoordinateVariable && !isCoordinateSystem && !isCoordinateTransform

        fun hasCoordinateSystem(): Boolean = !coordSysNames.isEmpty()

        override fun toString(): String = vb.shortName

        /**
         * Turn the variable into a coordinate axis.
         * Add to the dataset, replacing variable if needed.
         */
        fun makeIntoCoordinateAxis(): CoordinateAxis.Builder<*>? {
            if (axis != null) {
                return axis
            }
            if (vb is CoordinateAxis.Builder<*>) { // never true ??
                axis = vb as CoordinateAxis.Builder<*>?
            } else {
                // Create a CoordinateAxis out of this variable.
                axis = CoordinateAxis.fromVariableDS(vb).setParentGroupBuilder(gb)
            }
            if (axis != null) {
                axis!!.setAxisType(axisType)
                axis!!.addAttribute(Attribute(_Coordinate.AxisType, axisType.toString()))
                if (axisType!!.isVert && positive != null) {
                    axis!!.addAttribute(Attribute(_Coordinate.ZisPositive, positive))
                }
            }
            coords.replaceCoordinateAxis(axis)
            if (axis!!.parentStructureBuilder != null) {
                axis!!.parentStructureBuilder.replaceMemberVariable(axis)
            } else {
                gb.replaceVariable(axis)
            }
            return axis
        }

        /** For any variable listed in a coordinateAxes attribute, make into a coordinate.  */
        fun makeCoordinatesFromCoordinateSystem() {
            if (coordinateAxes != null) {
                coordinateAxes!!.split(" ").forEach { vname ->
                    val ap = findVarProcess(vname, this)
                    if (ap != null) {
                        ap.makeIntoCoordinateAxis()
                    } else {
                        info.appendLine(" Cant find axes ${vname} for Coordinate System ${vb.fullName}")
                    }
                }
            }
        }

        /** For explicit coordinate system variables, make a CoordinateSystem.  */
        fun makeCoordinateSystem() {
            if (coordinateAxes != null) {
                val sysName: String = coords.makeCanonicalName(vb, coordinateAxes)
                cs = CoordinateSystem.builder().setCoordAxesNames(sysName)
                info.appendLine(" Made Coordinate System '${sysName}'")
                coords.addCoordinateSystem(cs)
            }
        }

        /**
         * Create a list of coordinate axes for this data variable. Use the list of names in axes or
         * coordinates field.
         *
         * @param addCoordVariables if true, add any coordinate variables that are missing.
         * @return list of coordinate axes for this data variable.
         */
        fun findCoordinateAxes(addCoordVariables: Boolean): List<CoordinateAxis.Builder<*>?> {
            val axesList: MutableList<CoordinateAxis.Builder<*>?> = ArrayList()
            if (coordinateAxes != null) { // explicit axes
                coordinateAxes!!.split(" ").forEach { vname ->
                    val ap = findVarProcess(vname, this)
                    if (ap != null) {
                        val axis = ap.makeIntoCoordinateAxis()
                        if (!axesList.contains(axis)) {
                            axesList.add(axis)
                        }
                    }
                }
            } else if (coordinates != null) { // CF partial listing of axes
                coordinates!!.split(" ").forEach { vname ->
                    val ap = findVarProcess(vname, this)
                    if (ap != null) {
                        val axis = ap.makeIntoCoordinateAxis() // TODO check if its legal
                        if (!axesList.contains(axis)) {
                            axesList.add(axis)
                        }
                    }
                }
            }
            if (addCoordVariables) {
                for (d in vb!!.dimensions) {
                    for (vp in coordVarsForDimension.get(DimensionWithGroup(d, gb))) {
                        val axis = vp.makeIntoCoordinateAxis()
                        if (!axesList.contains(axis)) {
                            axesList.add(axis)
                        }
                    }
                }
            }
            return axesList
        }

        fun setCoordinateTransform(ct: ProjectionCTV) {
            if (cs == null) {
                info.appendLine("  ${vb.fullName}: no CoordinateSystem for CoordinateTransformVariable: ${ct.name}")
                return
            }
            cs!!.setCoordinateTransformName(ct.name)
        }
    } // VarProcess

}