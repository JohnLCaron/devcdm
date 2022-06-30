package dev.cdm.dataset.coordsysbuild

import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.Multimap
import dev.cdm.array.ArrayType
import dev.cdm.core.api.Attribute
import dev.cdm.core.api.Dimension
import dev.cdm.core.api.Group
import dev.cdm.core.api.Variable
import dev.cdm.core.constants.AxisType
import dev.cdm.core.constants.CF
import dev.cdm.core.constants._Coordinate
import dev.cdm.dataset.api.CdmDataset
import dev.cdm.dataset.api.CoordinateAxis
import dev.cdm.dataset.api.CoordinateSystem
import dev.cdm.dataset.api.VariableDS
import dev.cdm.dataset.internal.CoordinatesHelper
import dev.cdm.dataset.transform.horiz.ProjectionCTV

private val useMaximalCoordSys = true
private val requireCompleteCoordSys = true

open class CoordSysBuilder(val dataset: CdmDataset, val conventionName : String = _Coordinate.Convention) {
    internal val root: Group = dataset.rootGroup
    internal val varList = mutableListOf<VarProcess>()
    internal val coordVarsForDimension: Multimap<DimensionWithGroup, VarProcess> = ArrayListMultimap.create()

    internal val coords = CoordsHelperBuilder()
    internal val info = StringBuilder()
    private val helper = CoordAttrConvention(this)

    // All these steps may be overriden by subclasses.
    open fun buildCoordinateSystems() : CoordsHelperBuilder {
        info.appendLine("Parsing with Convention '${conventionName}'")

        // Bookkeeping info for each variable is kept in the VarProcess inner class
        addVariables(root)

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

        // assign implicit CoordinateSystem objects to variables
        makeCoordinateSystemsImplicit()

        // optionally assign implicit CoordinateSystem objects to variables that dont have one yet
        if (useMaximalCoordSys) {
            makeCoordinateSystemsMaximal()
        }

        // make Coordinate Transforms
        makeCoordinateTransforms()

        // assign Coordinate Transforms
        assignCoordinateTransforms()

        // set the coordinate systems for variables
        varList.forEach { vp ->
            coords.setCoordinateSystemFor(vp.vds.fullName, vp.coordSysNames)
        }

        return coords
    }

    private fun addVariables(group: Group) {
        group.variables.forEach { vb ->
                varList.add(VarProcess(group, vb as VariableDS))
        }
        for (nested in group.groups) {
            addVariables(nested)
        }
    }

    open fun identifyAxisType(vds: VariableDS): AxisType? {
        return helper.identifyAxisType(vds)
    }

    open fun identifyIsPositive(vds: VariableDS): Boolean? {
        return helper.identifyIsPositive(vds)
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

    /** Identify coordinate systems, using _Coordinate.Systems attribute.  */
    open fun identifyCoordinateSystems() {
        helper.identifyCoordinateSystems()
    }

    /** Identify coordinate transforms, using _CoordinateTransforms attribute.  */
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
        varList.forEach { vp ->
            if (vp.isCoordinateSystem) {
                vp.makeCoordinateSystem()
            }
        }
    }

    /** Assign explicit CoordinateSystem objects to variables. */
    open fun assignCoordinateSystemsExplicit() {
        helper.assignCoordinateSystemsExplicit()

        // look for explicit listings of coordinate axes in _Coordinate.Axes, add to the data variable
        varList.forEach { vp ->
            if (vp.coordinatesAll != null && !vp.hasCoordinateSystem() && vp.isData()) {
                val coordSysName = coords.makeCanonicalName(vp.vds, vp.coordinatesAll!!)
                val cso = coords.findCoordinateSystem(coordSysName)
                if (cso != null) {
                    vp.assignCoordinateSystem(coordSysName, "(explicit)")
                } else {
                    val csnew = CoordinateSystem.builder(coordSysName).setCoordAxesNames(coordSysName)
                    coords.addCoordinateSystem(csnew)
                    vp.assignCoordinateSystem(coordSysName, "(explicit)")
                }
            }
        }
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
                if (axesList.size < 2) {
                    return
                }
                val csName = CoordinatesHelper.makeCanonicalName(axesList)
                val csb = coords.findCoordinateSystem(csName)
                if (csb != null && coords.isComplete(csb, vp.vds)) {
                    vp.assignCoordinateSystem(csName, "(implicit)")
                } else {
                    val csnew = CoordinateSystem.builder(csName).setCoordAxesNames(csName).setImplicit(true)
                    if (coords.isComplete(csnew, vp.vds)) {
                        vp.assignCoordinateSystem(csName, "(implicit)")
                        coords.addCoordinateSystem(csnew)
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
                return
            }

            // look through all axes that fit
            val axisList = mutableListOf<CoordinateAxis.Builder<*>>()
            varList.filter { it.axis != null }.forEach { vpAxis ->
                if (vpAxis.axis!!.dimensions.isEmpty()) {
                    return  // scalar coords must be explicitly added.
                }
                if (hasCompatibleDimensions(vp.vds, vpAxis.vds)) {
                    axisList.add(vpAxis.axis!!)
                }
            }
            if (axisList.size < 2) {
                return
            }
            val csName = CoordinatesHelper.makeCanonicalName(axisList)
            val csb = coords.findCoordinateSystem(csName)
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
                vp.assignCoordinateSystem(csName, "(maximal)")
            } else {
                val csnew = CoordinateSystem.builder(csName).setCoordAxesNames(csName)
                if (requireCompleteCoordSys) {
                    okToBuild = coords.isComplete(csnew, vp.vds)
                }
                if (okToBuild) {
                    csnew.setImplicit(true)
                    vp.assignCoordinateSystem(csName, "(maximal)")
                    coords.addCoordinateSystem(csnew)
                }
            }
        }
    }

    /** Take previously identified Coordinate Transforms and create a CoordinateTransform for it */
    open fun makeCoordinateTransforms() {
        varList.forEach { vp ->
            if (vp.isCoordinateTransform && vp.ctv == null) {
                vp.ctv = makeTransformBuilder(vp.vds)
            }
            if (vp.ctv != null) {
                coords.addCoordinateTransform(vp.ctv!!)
            }
        }
    }

    internal fun makeTransformBuilder(vb: VariableDS): ProjectionCTV? {
        // at this point dont know if its a Projection or a VerticalTransform
        return ProjectionCTV(vb.fullName, vb.attributes(), null)
    }

    /** Assign CoordinateTransform objects to Variables and Coordinate Systems.  */
    open fun assignCoordinateTransforms() {
        helper.assignCoordinateTransforms()

        // look for _CoordinateAxes on the CTV, apply to any Coordinate Systems that contain all these axes
        varList.forEach { vp ->
            if (vp.coordinatesAll != null && vp.isCoordinateTransform && vp.ctv != null) {
                //  look for Coordinate Systems that contain all these axes
                varList.forEach { csv ->
                    if (csv.isCoordinateSystem && csv.cs != null) {
                        if (csv.cs!!.containsAxesNamed(vp.coordinatesAll)) {
                            csv.cs!!.addTransformName(vp.transformName) // TODO
                            info.appendLine("Assign (implicit coordAxes) coordTransform '${vp.transformName}' to CoordSys '${vp.cs!!.coordAxesNames}'")
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
        var transformName: String? = null
        var ctv: ProjectionCTV? = null

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

        fun setIsCoordinateAxis(extra : String = "") {
            if (!isCoordinateAxis) {
                info.appendLine("Identify CoordinateAxis '${this}' $extra")
            }
            isCoordinateAxis = true
        }

        fun setIsCoordinateSystem(extra : String = "") {
            if (!isCoordinateSystem) {
                info.appendLine("Identify CoordinateSystem '${this}' $extra")
            }
            isCoordinateSystem = true
        }

        fun assignCoordinateSystem(csysName: String, extra : String = "") {
            if (!isCoordinateSystem) {
                info.appendLine("Assign CoordinateSystem $csysName to '${this}' $extra")
            }
            coordSysNames.add(csysName)
        }

        fun setIsCoordinateTransform(extra : String = "") {
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
            vds.dimensions.forEach { dim->
                coordVarsForDimension.get(DimensionWithGroup(dim, group)).forEach { vp ->
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
            val axis = CoordinateAxis.fromVariableDS(vds.toBuilder())
            if (axisType == null) {
                axisType = identifyAxisType(vds)
            }
            if (axisType != null) {
                axis.setAxisType(axisType)
                axis.addAttribute(Attribute(_Coordinate.AxisType, axisType.toString()))
                if (axisType!!.isVert) {
                    val positive = identifyIsPositive(vds)
                    if (positive != null) {
                        axis.addAttribute(Attribute(_Coordinate.ZisPositive, if (positive) CF.POSITIVE_UP else CF.POSITIVE_DOWN))
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
                val coordNames: String = coords.makeCanonicalName(vds, coordinatesAll!!)
                val cs = CoordinateSystem.builder(vds.shortName).setCoordAxesNames(coordNames)
                info.appendLine("Made Coordinate System '${vds.shortName}' on axes '${coordNames}'")
                coords.addCoordinateSystem(cs)
                this.cs = cs
            }
        }

        /**
         * Create a list of coordinate axes for this data variable, from names in coordinatesAll
         * @return list of coordinate axes for this data variable.
         */
        fun findAllCoordinateAxes(): List<CoordinateAxis.Builder<*>?> {
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
        // The dimension must be in the common parent group
        if (groupa !== groupv && commonGroup.findDimension(axisDim) == null) {
            return false
        }
    }
    return true
}