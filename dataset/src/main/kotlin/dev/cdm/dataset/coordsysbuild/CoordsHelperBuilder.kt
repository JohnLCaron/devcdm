package dev.cdm.dataset.coordsysbuild

import com.google.common.base.Preconditions
import dev.cdm.core.api.CdmFile
import dev.cdm.core.api.Dimension
import dev.cdm.core.constants.AxisType
import dev.cdm.dataset.api.CoordinateAxis
import dev.cdm.dataset.api.CoordinateSystem
import dev.cdm.dataset.api.CoordinateTransform
import dev.cdm.dataset.api.VariableDS
import dev.cdm.dataset.internal.CoordinatesHelper
import java.util.*
import java.util.function.Consumer

class CoordsHelperBuilder(val conventionName : String) {
    val coordAxes = mutableListOf<CoordinateAxis.Builder<*>>()
    val coordSys = mutableListOf<CoordinateSystem.Builder<*>>()
    val coordTransforms = mutableListOf<CoordinateTransform>()
    val coordSysForVar = mutableMapOf<String, List<String>>()
    private var built = false

    fun getCoordinateSystemFor() : Map<String, List<String>> {
        return coordSysForVar.toMap()
    }

    // set a variable's list of coordSys
    fun setCoordinateSystemForVariable(varName : String, coordSys : List<String>): CoordsHelperBuilder {
        Preconditions.checkNotNull(varName)
        coordSysForVar[varName] = coordSys
        return this
    }

    // For every coordsys that uses the named axis, add the CoordinateTransform to it
    fun setCoordinateTransformFor(ctvName : String, axisNames: List<String>): CoordsHelperBuilder {
        coordSys.filter { it.containsAxes(axisNames)}.forEach { it.addTransformName(ctvName)}
        return this
    }

    fun addCoordinateAxis(axis: CoordinateAxis.Builder<*>?): CoordsHelperBuilder {
        if (axis != null) {
            coordAxes.add(axis)
        }
        return this
    }

    fun findAxisByName(name: String): CoordinateAxis.Builder<*>? {
        val found = coordAxes.find { it.fullName == name }
        return found?.let { coordAxes.find { it.shortName == name } }
    }

    fun findAxisByType(csys: CoordinateSystem.Builder<*>, type: AxisType): CoordinateAxis.Builder<*>? {
        for (axis in getAxesForSystem(csys)) {
            if (axis.axisType == type) {
                return axis
            }
        }
        return null
    }

    fun findVertAxis(csys: CoordinateSystem.Builder<*>): CoordinateAxis.Builder<*>? {
        for (axis in getAxesForSystem(csys)) {
            if (axis.axisType != null && axis.axisType.isVert) {
                return axis
            }
        }
        return null
    }

    fun replaceCoordinateAxis(axis: CoordinateAxis.Builder<*>): Boolean {
        val want = findAxisByName(axis.fullName)
        if (want != null) {
            coordAxes.remove(want)
        }
        addCoordinateAxis(axis)
        return want != null
    }

    fun addCoordinateSystem(cs: CoordinateSystem.Builder<*>): CoordsHelperBuilder {
        Preconditions.checkNotNull(cs)
        if (!coordSys.contains(cs)) {
            coordSys.add(cs)
        }
        return this
    }

    fun findCoordinateSystem(coordAxesNames: String): CoordinateSystem.Builder<*>? {
        return coordSys.find { it.coordAxesNames == coordAxesNames }
    }

    fun addCoordinateTransform(ct: CoordinateTransform): CoordsHelperBuilder {
        Preconditions.checkNotNull(ct)
        if (coordTransforms.stream().noneMatch { old -> old.name == ct.name }) {
            coordTransforms.add(ct)
        }
        return this
    }

    fun addTransformTo(coordTransName : String, coordSysName : String) {
        val cs = findCoordinateSystem(coordSysName)
        if (cs != null) {
            cs.addTransformName(coordTransName)
        }
    }

    private fun getAxesForSystem(cs: CoordinateSystem.Builder<*>): List<CoordinateAxis.Builder<*>> {
        Preconditions.checkNotNull(cs)
        val axes: MutableList<CoordinateAxis.Builder<*>> = ArrayList()
        cs.coordAxesNames!!.split(" ").forEach { vname ->
            val vbOpt = findAxisByName(vname)
            if (vbOpt != null) {
                axes.add(vbOpt)
            } else {
                throw IllegalArgumentException("Cant find axis $vname")
            }
        }
        return axes
    }

    // return null if axie
    fun makeCanonicalName(vb: VariableDS, axesNames : String) : String? {
        Preconditions.checkNotNull(axesNames)
        val axes = mutableListOf<CoordinateAxis.Builder<*>>()
        axesNames.trim().split(" ").forEach { vname ->
            var vbOpt = findAxisByName(vname)
            if (vbOpt == null) {
                vbOpt = findAxisByVerticalSearch(vb, vname)
            }
            if (vbOpt != null) {
                axes.add(vbOpt)
            } else {
                return null
            }
        }
        return CoordinatesHelper.makeCanonicalName(axes)
    }

    // dealing with axes in parent group. should be handled by fullName search ??
    private fun findAxisByVerticalSearch(vb: VariableDS, shortName: String): CoordinateAxis.Builder<*>? {
        val vaxis = vb.parentGroup.findVariableOrInParent(shortName)
        if (vaxis != null) {
            return findAxisByName(vaxis.getFullName())
        }
        return null
    }

    // Check if this Coordinate System is complete for v, ie if v dimensions are a subset..
    fun isComplete(cs: CoordinateSystem.Builder<*>, vb: VariableDS): Boolean {
        Preconditions.checkNotNull(cs)
        Preconditions.checkNotNull(vb)
        val csDomain = HashSet<Dimension>()
        getAxesForSystem(cs).forEach(Consumer { axis: CoordinateAxis.Builder<*> ->
            csDomain.addAll(axis.dimensions)
        })
        return CoordinateSystem.isComplete(vb.dimensions, csDomain)
    }

    fun containsAxes(csys: CoordinateSystem.Builder<*>, axisNames: String): Boolean {
        val axes = mutableListOf<CoordinateAxis.Builder<*>>()
        axisNames.split(" ").forEach { name ->
            val axis = findAxisByName(name)
            if (axis != null) {
                axes.add(axis)
            }
        }
        return containsAxes(csys, axes)
    }

    fun containsAxes(csys: CoordinateSystem.Builder<*>, dataAxes: List<CoordinateAxis.Builder<*>>): Boolean {
        Preconditions.checkNotNull(csys)
        Preconditions.checkNotNull(dataAxes)
        val csAxes = getAxesForSystem(csys)
        return csAxes.containsAll(dataAxes)
    }

    fun containsAxisTypes(csys: CoordinateSystem.Builder<*>, axisTypes: String): Boolean {
        val list = axisTypes.split(" ").map { AxisType.valueOf(it) }
        return containsAxisTypes(csys, list)
    }

    fun containsAxisTypes(cs: CoordinateSystem.Builder<*>, axisTypes: List<AxisType>): Boolean {
        Preconditions.checkNotNull(cs)
        Preconditions.checkNotNull(axisTypes)
        val csAxes = getAxesForSystem(cs)
        for (axisType in axisTypes) {
            if (!containsAxisTypes(csAxes, axisType)) return false
        }
        return true
    }

    private fun containsAxisTypes(axes: List<CoordinateAxis.Builder<*>>, want: AxisType): Boolean {
        return axes.find { it.axisType == want} != null
    }

    fun build(cdmFile : CdmFile) : CoordinatesHelper {
        check(!built) { "already built" }
        built = true
        return CoordinatesHelper(this, this.coordAxes.map{ it ->
            val useGroup = cdmFile.findGroup(it.getParentGroupName())
            check(useGroup != null)
            it.build(useGroup)
        })
    }
}