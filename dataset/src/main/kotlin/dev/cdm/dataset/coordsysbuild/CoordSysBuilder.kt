package dev.cdm.dataset.coordsysbuild

import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.Multimap
import dev.cdm.array.ArrayType
import dev.cdm.core.api.Attribute
import dev.cdm.core.api.Dimension
import dev.cdm.core.api.Group
import dev.cdm.core.constants.AxisType
import dev.cdm.core.constants._Coordinate
import dev.cdm.dataset.api.CdmDataset
import dev.cdm.dataset.api.CoordinateAxis
import dev.cdm.dataset.api.CoordinateSystem
import dev.cdm.dataset.api.VariableDS
import dev.cdm.dataset.transform.horiz.ProjectionCTV

private val useMaximalCoordSys = true

open class CoordSysBuilder(val dataset: CdmDataset, val conventionName : String = _Coordinate.Convention) {

    /**
     * Calculate if this is a classic coordinate variable: has same name as its first dimension.
     * If type char, must be 2D, else must be 1D.
     *
     * @return true if a coordinate variable.
     */
    fun isCoordinateVariable(vb: VariableDS): Boolean {
        // Structures and StructureMembers cant be coordinate variables
        if (vb.arrayType == ArrayType.STRUCTURE || vb.parentStructure != null) return false
        val rank = vb.rank
        if (rank == 1) {
            val firstd = vb.dimensions[0]
            if (firstd != null && vb.shortName == firstd.shortName) {
                return true
            }
        }
        if (rank == 2) { // two dimensional
            val firstd = vb.dimensions[0]
            // must be char valued (then its really a String)
            return firstd != null && vb.shortName == firstd.shortName && vb.arrayType == ArrayType.CHAR
        }
        return false
    }

    ////////////////////////////////////////////////////////////////////////////////////

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

        /* assign implicit CoordinateSystem objects to variables
        makeCoordinateSystemsImplicit()

        // optionally assign implicit CoordinateSystem objects to variables that dont have one yet
        if (useMaximalCoordSys) {
            makeCoordinateSystemsMaximal()
        }
        */

        // make Coordinate Transforms
        makeCoordinateTransforms()

        // assign Coordinate Transforms
        assignCoordinateTransforms()

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

    open fun identifyIsPositive(vds: VariableDS): Boolean? {
        return helper.identifyIsPositive(vds)
    }

    open fun identifyAxisType(vds: VariableDS): AxisType? {
        return helper.identifyAxisType(vds)
    }

    open fun identifyCoordinateAxes() {
        helper.identifyCoordinateAxes()
        varList.forEach { vp ->
            if (vp.coordinates != null) {
                identifyCoordinateAxesFromList(vp, vp.coordinates!!)
            }
        }
    }

    open fun identifyCoordinateVariables() {
        helper.identifyCoordinateVariables()
    }

    // coordinates is a list of space-delimited coordinate names
    fun identifyCoordinateAxesFromList(vp: VarProcess, coordinates: String) {
        coordinates.split(" ").forEach { vname ->
            var ap = findVarProcess(vname, vp)
            if (ap == null) {
                val gb = vp.vb.parentGroup
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
                    vp.axisType = identifyAxisType(vp.vb)
                }
                if (vp.axisType == null) {
                    info.appendLine("Coordinate Axis '${vp}' does not have an assigned AxisType")
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

    /** Assign explicit CoordinateSystem objects to variables. */
    open fun assignCoordinateSystemsExplicit() {
        helper.assignCoordinateSystemsExplicit()
    }

    /**
     * Take all previously identified Coordinate Transforms and create a CoordinateTransform object by
     * calling CoordTransBuilder.makeCoordinateTransform().
     */
    open fun makeCoordinateTransforms() {
        varList.forEach { vp ->
            if (vp.isCoordinateTransform && vp.ctv == null) {
                vp.ctv = makeTransformBuilder(vp.vb)
            }
            if (vp.ctv != null) {
                coords.addCoordinateTransform(vp.ctv!!)
            }
        }
    }

    protected open fun makeTransformBuilder(vb: VariableDS): ProjectionCTV? {
        // at this point dont know if its a Projection or a VerticalTransform
        return ProjectionCTV(vb.fullName, vb.attributes(), null)
    }

    open fun assignCoordinateTransforms() {
        helper.assignCoordinateTransforms()
    }

    fun findVarProcess(name: String?, from: VarProcess?): VarProcess? {
        if (name == null) {
            return null
        }

        // search on vb full name
        varList.find { name == it.vb.fullName }?.let { return it }

        // prefer ones in the same group with short name
        if (from != null) {
            varList.find { name == it.vb.shortName && it.vb.parentGroup == from.vb.parentGroup }
                ?.let { return it }
        }

        // WAEF, use short name from anywhere
        varList.find { name == it.vb.shortName }?.let { return it }
        return null
    }

    internal data class DimensionWithGroup(val dim: Dimension, val group: Group) {
        override fun toString(): String {
            val groupName = if (group.shortName.isEmpty()) "root" else group.shortName
            return "$dim, $groupName)"
        }
    }

    /** Classifications of Variables into axis, systems and transforms  */
    inner class VarProcess(val gb: Group, val vb: VariableDS) {
        val coordSysNames = mutableListOf<String>()

        // attributes
        var positive: String? = null // _Coordinate.ZisPositive or CF.POSITIVE
        var coordinateAxes: String? // _Coordinate.Axes
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
        var transformName: String? = null
        var isCoordinateTransform: Boolean
        var ctv: ProjectionCTV? = null

        /** Wrap the given variable. Identify Coordinate Variables. Process all _Coordinate attributes.  */
        init {
            isCoordinateVariable = isCoordinateVariable(vb)
            if (isCoordinateVariable) {
                info.appendLine("Identify Coordinate Variable '${this}'")
                coordVarsForDimension.put(DimensionWithGroup(vb.dimensions[0], gb), this)
            }

            coordinateAxes = vb.findAttributeString(_Coordinate.Axes, null)
            coordinateTransforms = vb.findAttributeString(_Coordinate.Transforms, null)
            isCoordinateSystem = coordinateTransforms != null
            coordAxisTypes = vb.findAttributeString(_Coordinate.AxisTypes, null)
            coordTransformType = vb.findAttributeString(_Coordinate.TransformType, null)
            isCoordinateTransform = coordTransformType != null || coordAxisTypes != null
        }

        fun isData(): Boolean =
            !isCoordinateVariable && !isCoordinateAxis && !isCoordinateSystem && !isCoordinateTransform

        fun maybeData(): Boolean = !isCoordinateVariable && !isCoordinateSystem && !isCoordinateTransform

        fun hasCoordinateSystem(): Boolean = !coordSysNames.isEmpty()

        override fun toString(): String = vb.shortName

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

        fun setIsTransform(extra : String = "") {
            if (!isCoordinateTransform) {
                info.appendLine("Identify CoordinateTransform '${this}' $extra")
            }
            isCoordinateTransform = true
        }

        /**
         * Turn the variable into a coordinate axis.
         * Add to the dataset, replacing variable if needed.
         */
        fun makeIntoCoordinateAxis(): CoordinateAxis.Builder<*>? {
            if (this.axis != null) {
                return this.axis
            }

            // Create a CoordinateAxis out of this variable.
            val axis = CoordinateAxis.fromVariableDS(vb.toBuilder())
            if (axisType != null) {
                axis.setAxisType(axisType)
                axis.addAttribute(Attribute(_Coordinate.AxisType, axisType.toString()))
                if (axisType!!.isVert && positive != null) {
                    axis.addAttribute(Attribute(_Coordinate.ZisPositive, positive))
                }
            }
            coords.addCoordinateAxis(axis)
            this.axis = axis
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
                        info.appendLine("*** Cant find axes '${vname}' for Coordinate System '${vb.fullName}'")
                    }
                }
            }
        }

        /** For explicit coordinate system variables, make a CoordinateSystem.  */
        fun makeCoordinateSystem() {
            if (coordinateAxes != null) {
                val coordNames: String = coords.makeCanonicalName(vb, coordinateAxes)
                cs = CoordinateSystem.builder(vb.shortName).setCoordAxesNames(coordNames)
                info.appendLine("Made Coordinate System '${vb.shortName}' on axes '${coordNames}'")
                coords.addCoordinateSystem(cs!!)
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
    } // VarProcess

}