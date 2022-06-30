package dev.cdm.dataset.coordsysbuild

import com.google.common.base.Preconditions
import com.google.common.collect.ImmutableMap
import dev.cdm.core.api.Dimension
import dev.cdm.core.api.Group
import dev.cdm.core.constants.AxisType
import dev.cdm.dataset.api.CoordinateAxis
import dev.cdm.dataset.api.CoordinateSystem
import dev.cdm.dataset.api.VariableDS
import dev.cdm.dataset.internal.CoordinatesHelper
import dev.cdm.dataset.transform.horiz.ProjectionCTV
import java.util.*
import java.util.function.Consumer

class CoordsHelperBuilder {
    val coordAxes = mutableListOf<CoordinateAxis.Builder<*>>()
    val coordSys = mutableListOf<CoordinateSystem.Builder<*>>()
    val coordTransforms = mutableListOf<ProjectionCTV>()
    val coordSysForVar = mutableMapOf<String, List<String>>()
    private var built = false

    fun getCoordinateSystemFor() : Map<String, List<String>> {
        return coordSysForVar.toMap()
    }

    fun setCoordinateSystemFor(varName : String, coordSys : List<String>): CoordsHelperBuilder {
        Preconditions.checkNotNull(varName)
        coordSysForVar[varName] = coordSys
        return this
    }

    fun addCoordinateAxis(axis: CoordinateAxis.Builder<*>?): CoordsHelperBuilder {
        if (axis != null) {
            coordAxes.add(axis)
        }
        return this
    }

    fun findAxisByFullName(fullName: String): CoordinateAxis.Builder<*>? {
        return coordAxes.find { it.fullName == fullName }
    }

    fun findAxisByType(csys: CoordinateSystem.Builder<*>, type: AxisType): CoordinateAxis.Builder<*>? {
        for (axis in getAxesForSystem(csys)) {
            if (axis.axisType == type) {
                return axis
            }
        }
        return null
    }

    fun replaceCoordinateAxis(axis: CoordinateAxis.Builder<*>): Boolean {
        val want = findAxisByFullName(axis.fullName)
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

    fun addCoordinateTransform(ct: ProjectionCTV): CoordsHelperBuilder {
        Preconditions.checkNotNull(ct)
        if (coordTransforms.stream().noneMatch { old: ProjectionCTV -> old.name == ct.name }) {
            coordTransforms.add(ct)
        }
        return this
    }

    fun replaceCoordinateTransform(ct: ProjectionCTV) {
        coordTransforms.stream().filter { t: ProjectionCTV -> t.name == ct.name }.findFirst()
            .ifPresent { o: ProjectionCTV ->
                coordTransforms.remove(
                    o
                )
            }
        addCoordinateTransform(ct)
    }

    private fun getAxesForSystem(cs: CoordinateSystem.Builder<*>): List<CoordinateAxis.Builder<*>> {
        Preconditions.checkNotNull(cs)
        val axes: MutableList<CoordinateAxis.Builder<*>> = ArrayList()
        cs.coordAxesNames!!.split(" ").forEach { vname ->
            val vbOpt = findAxisByFullName(vname)
            if (vbOpt != null) {
                axes.add(vbOpt)
            } else {
                throw IllegalArgumentException("Cant find axis $vname")
            }
        }
        return axes
    }

    fun makeCanonicalName(vb: VariableDS, axesNames : String) : String {
        Preconditions.checkNotNull(axesNames)
        val axes = mutableListOf<CoordinateAxis.Builder<*>>()
        axesNames!!.split(" ").forEach { vname ->
            var vbOpt = findAxisByFullName(vname)
            if (vbOpt == null) {
                vbOpt = findAxisByVerticalSearch(vb, vname)
            }
            if (vbOpt != null) {
                axes.add(vbOpt)
            } else {
                throw IllegalArgumentException("Cant find axis $vname")
            }
        }
        return CoordinatesHelper.makeCanonicalName(axes)
    }

    // dealing with axes in parent group. should be handled by fullName search ??
    private fun findAxisByVerticalSearch(vb: VariableDS, shortName: String): CoordinateAxis.Builder<*>? {
        val axis = vb.parentGroup.findVariableOrInParent(shortName)
        if (axis != null) {
            if (axis is CoordinateAxis.Builder<*>) { // LOOK always false
                return axis as CoordinateAxis.Builder<*>
            }
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

    fun containsAxes(cs: CoordinateSystem.Builder<*>, dataAxes: List<CoordinateAxis.Builder<*>>): Boolean {
        Preconditions.checkNotNull(cs)
        Preconditions.checkNotNull(dataAxes)
        val csAxes = getAxesForSystem(cs)
        return csAxes.containsAll(dataAxes)
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

    fun build(group : Group) : CoordinatesHelper? {
        check(!built) { "already built" }
        built = true
        return CoordinatesHelper(this, this.coordAxes.map{ it -> it.build(group) })
    }
}