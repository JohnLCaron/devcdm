package dev.cdm.dataset.coordsysbuild

import com.google.common.collect.BiMap
import com.google.common.collect.HashBiMap
import dev.cdm.array.ArrayType
import dev.cdm.array.Arrays
import dev.cdm.core.api.Attribute
import dev.cdm.core.api.Dimension
import dev.cdm.core.api.Group
import dev.cdm.core.api.Variable
import dev.cdm.core.constants.AxisType
import dev.cdm.core.constants.CF
import dev.cdm.core.constants._Coordinate
import dev.cdm.dataset.api.*
import dev.cdm.dataset.internal.CoordinatesHelper
import dev.cdm.dataset.transform.vertical.VerticalTransformFactory

private val useMaximalCoordSys = true
private val requireCompleteCoordSys = true

open class CoordinatesBuilder(val conventionName: String = _Coordinate.Convention) {
    internal val varList = mutableListOf<VarProcess>()
    internal val coordVarsForDimension: BiMap<DimensionWithGroup, VarProcess> = HashBiMap.create();

    internal val coords = CoordsHelperBuilder(conventionName)
    internal val info = StringBuilder()

    private val helper = CoordAttrConvention(this)

    open fun augment(orgDataset: CdmDataset): CdmDataset {
        return orgDataset
    }

    // All these steps may be overriden by subclasses.
    open fun buildCoordinateSystems(dataset: CdmDataset): CoordsHelperBuilder {
        info.appendLine("Parsing with Convention '${conventionName}'")

        // Bookkeeping info for each variable is kept in the VarProcess inner class
        addVariables(dataset.rootGroup)

        // identify which variables are coordinate axes
        identifyCoordinateVariables()
        identifyCoordinateAxes()

        // identify which variables are used to describe coordinate systems
        identifyCoordinateSystems()
        // identify which variables are used to describe coordinate transforms
        identifyCoordinateTransforms()

        // turn Variables into CoordinateAxis objects
        makeCoordinateAxes()

        // make Coordinate Systems for all Coordinate Systems Variables
        makeCoordinateSystems()

        // assign explicit CoordinateSystem objects to variables
        assignCoordinateSystemsExplicit()

        // make and assign implicit CoordinateSystem objects to variables
        makeCoordinateSystemsImplicit()

        // make and assign implicit CoordinateSystem objects to variables that dont have one yet
        if (useMaximalCoordSys) {
            makeCoordinateSystemsMaximal()
        }

        // make Coordinate Transforms
        makeCoordinateTransforms()

        // assign Coordinate Transforms
        assignCoordinateTransforms()

        // set the coordinate systems for variables
        varList.forEach { vp ->
            coords.setCoordinateSystemForVariable(vp.vds.fullName, vp.coordSysNames)
        }

        return coords
    }

    private fun addVariables(group: Group) {
        // LOOK excluding Structures and Sequences for now
        group.variables.filter { it is VariableDS }.forEach { vb ->
            varList.add(VarProcess(group, vb as VariableDS))
        }
        for (nested in group.groups) {
            addVariables(nested)
        }
    }

    open fun identifyAxisType(vds: VariableDS): AxisType? {
        return helper.identifyAxisType(vds)
    }

    open fun identifyZIsPositive(vds: VariableDS): Boolean? {
        return helper.identifyZIsPositive(vds)
    }

    open fun identifyCoordinateVariables() {
        helper.identifyCoordinateVariables()
    }

    open fun identifyCoordinateAxes() {
        helper.identifyCoordinateAxes()
        varList.forEach { vp ->
            if (vp.coordinatesAll != null) {
                identifyCoordinateAxesFromList(vp, vp.coordinatesAll!!)
            }
        }
    }

    // coordinates is a list of space-delimited coordinate names
    private fun identifyCoordinateAxesFromList(vp: VarProcess, coordinates: String) {
        coordinates.split(" ").forEach { vname ->
            var ap = findVarProcess(vname, vp)
            if (ap == null) {
                val gb = vp.vds.parentGroup
                val vopt = gb.findVariableOrInParent(vname)
                if (vopt != null) {
                    ap = findVarProcess(vopt.fullName, vp)
                } else {
                    info.appendLine("***Cant find coordAxis '${vname}' referenced from var '${vp}'")
                }
            }
            if (ap != null) {
                ap.setIsCoordinateAxis("referenced from var '${vp}'")
            } else {
                info.appendLine("***Cant find coordAxis '${vname}' referenced from var '${vp}'")
            }
        }
    }

    /** Identify coordinate systems.  */
    open fun identifyCoordinateSystems() {
        helper.identifyCoordinateSystems()
    }

    /** Identify coordinate transforms.  */
    open fun identifyCoordinateTransforms() {
        helper.identifyCoordinateTransforms()
    }

    /**
     * Take previously identified Coordinate Axis and Coordinate Variables and make them into CoordinateAxes.
     */
    open fun makeCoordinateAxes() {
        // The ones identified as coordinate variables or axes
        varList.forEach { vp ->
            if (vp.isCoordinateAxis || vp.isCoordinateVariable) {
                if (vp.axisType == null) {
                    vp.axisType = identifyAxisType(vp.vds)
                }
                if (vp.axisType == null) {
                    vp.axisType = desperateAxisType(vp.vds)
                }
                if (vp.axisType == null) {
                    info.appendLine("Coordinate Axis '${vp}' does not have an assigned AxisType")
                }
                vp.makeCoordinateAxis()
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
        // explicit identified coordinate systems
        varList.forEach { vp ->
            if (vp.isCoordinateSystem) {
                vp.makeCoordinateSystem()
            }
        }

        // indirectly identified coordinate systems
        varList.forEach { vp ->
            if (vp.coordinatesAll != null && vp.isData()) {
                val coordAxesName = coords.makeCanonicalName(vp.vds, vp.coordinatesAll!!)
                if (coordAxesName == null) {
                    return@forEach
                }
                val cso = coords.findCoordinateSystem(coordAxesName)
                if (cso != null) {
                    vp.assignCoordinateSystem(cso.name, "(indirect)")
                } else {
                    val csnew = CoordinateSystem.builder(coordAxesName).setCoordAxesNames(coordAxesName)
                    coords.addCoordinateSystem(csnew)
                    vp.assignCoordinateSystem(coordAxesName, "(indirect)")
                }
            }
        }
    }

    /** Assign explicit CoordinateSystem objects to variables. */
    open fun assignCoordinateSystemsExplicit() {
        helper.assignCoordinateSystemsExplicit()
    }

    /**
     * Make implicit CoordinateSystem objects for variables that dont already have one, by using the
     * variables' list of coordinate axes, and any coordinateVariables for it. Must be at least 2
     * axes. All of a variable's _Coordinate Variables_ plus any variables listed in a
     * *__CoordinateAxes_* or *_coordinates_* attribute will be made into an *_implicit_* Coordinate
     * System. If there are at least two axes, and the coordinate system uses all of the variable's
     * dimensions, it will be assigned to the data variable.
     */
    open fun makeCoordinateSystemsImplicit() {
        varList.forEach { vp ->
            if (!vp.hasCoordinateSystem() && vp.maybeData()) {
                val axesList = vp.findAllCoordinateAxes()
                if (axesList.size >= 2) {
                    val coordAxesName = CoordinatesHelper.makeCanonicalName(axesList)
                    val csb = coords.findCoordinateSystem(coordAxesName)
                    if (csb != null && coords.isComplete(csb, vp.vds)) {
                        vp.assignCoordinateSystem(csb.name, "(implicit)")
                    } else {
                        val csnew =
                            CoordinateSystem.builder(coordAxesName).setCoordAxesNames(coordAxesName).setImplicit(true)
                        if (coords.isComplete(csnew, vp.vds)) {
                            vp.assignCoordinateSystem(coordAxesName, "(implicit)")
                            coords.addCoordinateSystem(csnew)
                        }
                    }
                }
            }
        }
    }

    /**
     * If a variable still doesnt have a coordinate system, use hueristics to try to find one that was
     * probably forgotten. Examine existing axes, create the maximal set of axes that fits the variable.
     */
    open fun makeCoordinateSystemsMaximal() {
        varList.forEach { vp ->
            if (vp.hasCoordinateSystem() || !vp.isData() || vp.vds.dimensions.isEmpty()) {
                return@forEach
            }

            // look through all axes that fit
            val axisList = mutableListOf<CoordinateAxis.Builder<*>>()
            varList.filter { it.axis != null }.forEach axisloop@ { vpAxis ->
                if (vpAxis.axis!!.dimensions.isEmpty()) {
                    return@axisloop  // skip scalar coords; must be explicitly added.
                }
                if (hasCompatibleDimensions(vp.vds, vpAxis.vds)) {
                    axisList.add(vpAxis.axis!!)
                }
            }
            if (axisList.size < 2) {
                return@forEach
            }
            val coordAxesName = CoordinatesHelper.makeCanonicalName(axisList)
            val csb = coords.findCoordinateSystem(coordAxesName)
            var okToBuild = false

            // do coordinate systems need to be complete?
            if (requireCompleteCoordSys) {
                if (csb != null) {
                    okToBuild = coords.isComplete(csb, vp.vds)
                }
            } else {
                // coordinate system can be incomplete, so we're ok to build if we find something
                okToBuild = true
            }
            if (csb != null && okToBuild) {
                vp.assignCoordinateSystem(csb.name, "(maximal)")
            } else {
                val csnew = CoordinateSystem.builder(coordAxesName).setCoordAxesNames(coordAxesName)
                if (requireCompleteCoordSys) {
                    okToBuild = coords.isComplete(csnew, vp.vds)
                }
                if (okToBuild) {
                    csnew.setImplicit(true)
                    vp.assignCoordinateSystem(coordAxesName, "(maximal)")
                    coords.addCoordinateSystem(csnew)
                }
            }
        }
    }

    open fun makeCoordinateTransforms() {
        // create CoordinateTransform from variables already identifies as transforms
        varList.forEach { vp ->
            if (vp.isCoordinateTransform && vp.ctv == null) {
                val isProjection = (vp.gridMapping != null) || vp.vds.attributes().hasAttribute(CF.GRID_MAPPING_NAME)
                vp.ctv = CoordinateTransform(vp.vds.shortName, vp.vds.attributes(), isProjection)
            }
            if (vp.ctv != null) {
                coords.addCoordinateTransform(vp.ctv!!)
            }
        }

        // look at z axis of existing coordinate systems to see if they are transforms
        coords.coordSys.forEach { csys ->
            val vertCoord = coords.findVertAxis(csys);
            if (vertCoord != null) {
                val transformName = vertCoord.attributeContainer.findAttributeString("units", "none")!!
                // is it a vertical transform name?
                if (VerticalTransformFactory.hasVerticalTransformFor(transformName)) {
                    coords.addCoordinateTransform(CoordinateTransform(transformName, vertCoord.attributeContainer, false))
                    csys.addTransformName(transformName)
                }

            }
        }

    }

    /** Assign CoordinateTransform objects to Variables and Coordinate Systems.  */
    open fun assignCoordinateTransforms() {
        helper.assignCoordinateTransforms()

        // If gridMapping is set, assign that to variable's coordSystems
        varList.forEach { vp ->
            if (vp.isData() && vp.gridMapping != null) {
                vp.coordSysNames.forEach {
                    coords.addTransformTo(vp.gridMapping!!, it)
                    info.appendLine("Assign coordTransform '${vp.gridMapping}' to CoordSys '${it}'")
                }
            }
        }

        // If variable is a coordTransform and a coordVariable, assign it to any coordsys that uses the coordVariable
        varList.forEach { vp ->
            if (vp.isCoordinateTransform && vp.isCoordinateVariable && vp.ctv != null) {
                coords.setCoordinateTransformFor(vp.ctv!!.name, listOf(vp.vds.shortName))
                //  look for Coordinate Systems that contain all these axes
                info.appendLine("Assign CoordinateTransform '${vp.ctv!!.name}' for axis '${vp.vds.shortName}'")
            }
        }

        // look for already set coordinatesAll, apply to any Coordinate Systems that contain all these axes
        // TODO do we need to do this?
        varList.forEach { vp ->
            if (vp.coordinatesAll != null && vp.isCoordinateTransform && vp.ctv != null) {
                //  look for Coordinate Systems that contain all these axes
                varList.forEach { csv ->
                    if (csv.isCoordinateSystem && csv.cs != null) {
                        if (coords.containsAxes(csv.cs!!, vp.coordinatesAll!!)) {
                            if (csv.cs!!.addTransformName(vp.ctv!!.name)) {
                                info.appendLine("Assign (implicit coordAxes) coordTransform '${vp.ctv!!.name}' to CoordSys '${vp.cs!!.coordAxesNames}'")
                            }
                        }
                    }
                }
            }
        }
    }

    fun findVarProcess(name: String?, from: VarProcess?): VarProcess? {
        if (name == null) {
            return null
        }

        // search on vb full name
        varList.find { name == it.vds.fullName }?.let { return it }

        // prefer ones in the same group with short name
        if (from != null) {
            varList.find { name == it.vds.shortName && it.vds.parentGroup == from.vds.parentGroup }
                ?.let { return it }
        }

        // WAEF, use short name from anywhere
        varList.find { name == it.vds.shortName }?.let { return it }
        return null
    }

    internal data class DimensionWithGroup(val dim: Dimension, val group: Group) {
        override fun toString(): String {
            val groupName = if (group.shortName.isEmpty()) "root" else group.shortName
            return "$dim, $groupName)"
        }
    }

    /** Classifications of Variables into axis, systems and transforms  */
    inner class VarProcess(val group: Group, val vds: VariableDS) {
        // data variable
        val coordSysNames = mutableListOf<String>() // LOOK where used ??

        // coord axes
        var isCoordinateVariable = false
        var isCoordinateAxis = false
        var axisType: AxisType? = null
        var axis: CoordinateAxis.Builder<*>? = null

        // coord system
        var isCoordinateSystem: Boolean = false
        var coordinatesAll: String? = null // complete list of coordinates, eg from _Coordinate.Axes
        var cs: CoordinateSystem.Builder<*>? = null

        // coord transform
        var isCoordinateTransform: Boolean = false
        var gridMapping: String? = null
        var ctv: CoordinateTransform? = null // LOOK needed?

        /** Wrap the given variable. Identify Coordinate Variables. Process all _Coordinate attributes.  */
        init {
            isCoordinateVariable = vds.isCoordinateVariable
            if (isCoordinateVariable) {
                info.appendLine("Identify Coordinate Variable '${this}'")
                coordVarsForDimension.put(DimensionWithGroup(vds.dimensions[0], group), this)
            }
        }

        fun isData(): Boolean =
            !isCoordinateVariable && !isCoordinateAxis && !isCoordinateSystem && !isCoordinateTransform

        fun maybeData(): Boolean = !isCoordinateVariable && !isCoordinateSystem && !isCoordinateTransform

        fun hasCoordinateSystem(): Boolean = !coordSysNames.isEmpty()

        override fun toString(): String = vds.shortName

        fun setIsCoordinateAxis(extra: String = "") {
            if (!isCoordinateAxis) {
                info.appendLine("Identify CoordinateAxis '${this}' $extra")
            }
            isCoordinateAxis = true
        }

        fun setIsCoordinateSystem(extra: String = "") {
            if (!isCoordinateSystem) {
                info.appendLine("Identify CoordinateSystem '${this}' $extra")
            }
            isCoordinateSystem = true
        }

        fun assignCoordinateSystem(csysName: String, extra: String = "") {
            if (!isCoordinateSystem) {
                info.appendLine("Assign CoordinateSystem '$csysName' to '${this}' $extra")
            }
            coordSysNames.add(csysName)
        }

        fun setIsCoordinateTransform(extra: String = "") {
            if (!isCoordinateTransform) {
                info.appendLine("Identify CoordinateTransform '${this}' $extra")
            }
            isCoordinateTransform = true
        }

        // note that you could pass the empty string to add coordinate variables
        fun setPartialCoordinates(partialCoordinates: String) {
            val axes = mutableListOf<String>()
            axes.addAll(partialCoordinates.split(" "))
            // add missing coord vars
            vds.dimensions.forEach { dim ->
                coordVarsForDimension.get(DimensionWithGroup(dim, group))?.let { vp ->
                    val axis = vp.vds.shortName
                    if (axis != vds.shortName && !axes.contains(axis)) {
                        axes.add(axis)
                    }
                }
            }
            this.coordinatesAll = axes.joinToString(" ")
        }

        /** Make a coordinate axis from the variable. */
        fun makeCoordinateAxis(): CoordinateAxis.Builder<*>? {
            if (this.axis != null) {
                return this.axis
            }

            // Create a CoordinateAxis out of this variable.
            val axis = CoordinateAxis.fromVariableDS(vds)
            if (axisType == null) {
                axisType = identifyAxisType(vds)
            }
            if (axisType != null) {
                axis.setAxisType(axisType)
                axis.addAttribute(Attribute(_Coordinate.AxisType, axisType.toString()))
                if (axisType!!.isVert) {
                    val positive = identifyZIsPositive(vds)
                    if (positive != null) {
                        axis.addAttribute(
                            Attribute(_Coordinate.ZisPositive, if (positive) CF.POSITIVE_UP else CF.POSITIVE_DOWN)
                        )
                    }
                }
            }
            // add to CoordsHelper, note the original dataset has not been changed
            coords.addCoordinateAxis(axis)
            this.axis = axis
            return axis
        }

        /** For any variable listed in coordinatesAll or coordinates, make into a coordinate.  */
        fun makeCoordinatesFromCoordinateSystem() {
            if (coordinatesAll != null) {
                coordinatesAll!!.split(" ").forEach { vname ->
                    val ap = findVarProcess(vname, this)
                    if (ap != null) {
                        ap.makeCoordinateAxis()
                    } else {
                        info.appendLine("*** Cant find axes '${vname}' for Coordinate System '${vds.fullName}'")
                    }
                }
            }
        }

        /** For explicit coordinate system variables, make a CoordinateSystem.  */
        fun makeCoordinateSystem() {
            if (coordinatesAll != null) {
                val coordAxesName = coords.makeCanonicalName(vds, coordinatesAll!!)
                if (coordAxesName == null) {
                    return
                }
                val cs = CoordinateSystem.builder(vds.shortName).setCoordAxesNames(coordAxesName)
                coords.addCoordinateSystem(cs)
                this.cs = cs
                info.appendLine("Made Coordinate System '${vds.shortName}' on axes '${coordAxesName}'")
            }
        }

        /**
         * Create a list of coordinate axes for this data variable, from coordinateVariables and names in coordinatesAll
         * @return list of coordinate axes for this data variable.
         */
        fun findAllCoordinateAxes(): List<CoordinateAxis.Builder<*>?> {
            if (coordinatesAll == null) {
                // this will set coordinates to be the coordinate variables
                setPartialCoordinates("")
            }
            val axesList: MutableList<CoordinateAxis.Builder<*>?> = ArrayList()
            if (coordinatesAll != null) { // explicit axes
                coordinatesAll!!.split(" ").forEach { vname ->
                    val ap = findVarProcess(vname, this)
                    if (ap != null) {
                        val axis = ap.makeCoordinateAxis()
                        if (!axesList.contains(axis)) {
                            axesList.add(axis)
                        }
                    }
                }
            }
            return axesList
        }
    } // VarProcess

}

/**
 * Does this axis "fit" this variable. True if all of the dimensions in the axis also appear in
 * the variable.
 *
 * @param v the given variable
 * @param axis check if this axis is ok for the given variable
 * @return true if all of the dimensions in the axis also appear in the variable.
 */
fun hasCompatibleDimensions(v: Variable, axis: Variable): Boolean {
    val varDims = HashSet(v.dimensions)
    val groupv: Group = v.getParentGroup()
    val groupa = axis.parentGroup
    val commonGroup = groupv.commonParent(groupa)

    // a CHAR variable must really be a STRING, so leave out the last (string length) dimension
    var checkDims = axis.rank
    if (axis.arrayType == ArrayType.CHAR) checkDims--
    for (i in 0 until checkDims) {
        val axisDim = axis.getDimension(i)
        if (!axisDim.isShared) { // anon dimensions dont count.
            continue
        }
        if (!varDims.contains(axisDim)) {
            return false
        }
        // The dimension must be in the common parent group.
        // LOOK this could be - as long as the Dimensions are equal, not counting Group.
        if (groupa !== groupv && commonGroup.findDimension(axisDim) == null) {
            return false
        }
    }
    return true
}

/**
 * Create a "dummy" Coordinate Transform Variable based on the given ProjectionCTV.
 * This creates a scalar Variable with dummy data, which is just a container for the transform
 * attributes.
 *
 * @param ctv ProjectionCTV with Coordinate Transform Variable attributes set.
 * @return the Coordinate Transform Variable. You must add it to the dataset.
 */
fun makeCoordinateTransformVariable(ctv: CoordinateTransform): VariableDS.Builder<*> {
    val v = VariableDS.builder().setName(ctv.name).setArrayType(ArrayType.CHAR)
    v.addAttributes(ctv.metadata)
    v.addAttribute(Attribute(_Coordinate.TransformType, "Projection"))

    // fake data
    v.setSourceData(Arrays.factory<Any>(ArrayType.CHAR, intArrayOf(), charArrayOf(' ')))
    return v
}