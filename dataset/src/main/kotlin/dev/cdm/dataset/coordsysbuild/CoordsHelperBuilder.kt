package dev.cdm.dataset.coordsysbuild

import com.google.common.base.Preconditions
import dev.cdm.core.api.Dimension
import dev.cdm.core.api.Group
import dev.cdm.core.api.Variable
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
    private var built = false

    fun addCoordinateAxis(axis: CoordinateAxis.Builder<*>?): CoordsHelperBuilder {
        if (axis != null) {
            coordAxes.add(axis)
        }
        return this
    }

    fun addCoordinateAxes(axes: Collection<CoordinateAxis.Builder<*>>): CoordsHelperBuilder {
        Preconditions.checkNotNull(axes)
        axes.forEach(Consumer { axis: CoordinateAxis.Builder<*>? ->
            addCoordinateAxis(
                axis
            )
        })
        return this
    }

    /* LOOK
    private fun findAxisByVerticalSearch(vb: VariableDS, shortName: String
    ): CoordinateAxis.Builder<*>? {
        val axis = vb.parentGroup.findVariableOrInParent(shortName)
        if (axis != null) {
            if (axis is CoordinateAxis.Builder<*>) {
                return axis as CoordinateAxis.Builder<*>
            }
        }
        return null
    } */

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

    fun addCoordinateSystems(systems: Collection<CoordinateSystem.Builder<*>>): CoordsHelperBuilder {
        Preconditions.checkNotNull(systems)
        coordSys.addAll(systems)
        return this
    }

    // this is used when making a copy, we've thrown away the TransformBuilder
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
        val stoker = StringTokenizer(cs.coordAxesNames)
        while (stoker.hasMoreTokens()) {
            val vname = stoker.nextToken()
            val vbOpt = findAxisByFullName(vname)
            if (vbOpt != null) {
                axes.add(vbOpt)
            } else {
                throw IllegalArgumentException("Cant find axis $vname")
            }
        }
        return axes
    }

    fun makeCanonicalName(vb: VariableDS, axesNames: String?): String {
        Preconditions.checkNotNull(axesNames)
        val axes: MutableList<CoordinateAxis.Builder<*>> = ArrayList()
        val stoker = StringTokenizer(axesNames)
        while (stoker.hasMoreTokens()) {
            val vname = stoker.nextToken()
            var vbOpt = findAxisByFullName(vname)
            //if (vbOpt != null) {
            //    vbOpt = findAxisByVerticalSearch(vb, vname)
            //}
            if (vbOpt != null) {
                axes.add(vbOpt)
            } else {
                throw IllegalArgumentException("Cant find axis $vname")
            }
        }
        return CoordinatesHelper.makeCanonicalName(axes)
    }

    // Check if this Coordinate System is complete for v, ie if v dimensions are a subset..
    fun isComplete(cs: CoordinateSystem.Builder<*>, vb: Variable.Builder<*>): Boolean {
        Preconditions.checkNotNull(cs)
        Preconditions.checkNotNull(vb)
        val csDomain = HashSet<Dimension>()
        getAxesForSystem(cs).forEach(Consumer { axis: CoordinateAxis.Builder<*> ->
            csDomain.addAll(
                axis.dimensions
            )
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

    // Note that only ncd.axes can be accessed, not coordsys or transforms.
    fun build(group : Group) : CoordinatesHelper? {
        check(!built) { "already built" }
        built = true
        return CoordinatesHelper(this, this.coordAxes.map{ it -> it.build(group) })
    }
}