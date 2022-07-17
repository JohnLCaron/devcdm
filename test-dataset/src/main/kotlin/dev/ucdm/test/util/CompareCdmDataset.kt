package dev.ucdm.test.util

import dev.ucdm.array.ArrayType
import dev.ucdm.core.api.*
import dev.ucdm.core.constants.CDM
import dev.ucdm.core.iosp.IospUtils
import dev.ucdm.core.testutil.CompareCdmFiles
import dev.ucdm.dataset.api.CdmDatasetCS
import dev.ucdm.dataset.api.CoordinateAxis
import dev.ucdm.dataset.api.CoordinateSystem
import dev.ucdm.dataset.api.SimpleUnit
import dev.ucdm.dataset.geoloc.Projection
import java.util.*

open class CdmObjFilter {
    // if true, compare attribute, else skip comparision.
    open fun attCheckOk(att: Attribute): Boolean {
        return true
    }

    // override attribute comparision if needed
    open fun attsAreEqual(att1: Attribute, att2: Attribute): Boolean {
        return try {
            att1 == att2
        } catch (e: Exception) {
            System.out.printf("att1 = %s != att2 = %s%n", att1, att2)
            false
        }
    }

    // override dimension comparision if needed
    open fun dimsAreEqual(dim1: Dimension, dim2: Dimension): Boolean {
        return dim1 == dim2
    }

    // override dimension comparision if needed
    open fun enumsAreEqual(enum1: EnumTypedef, enum2: EnumTypedef): Boolean {
        return enum1 == enum2
    }

    // if true, compare variable, else skip comparision
    open fun ignoreVariable(v: Variable): Boolean {
        return false
    }

    // if true, compare variable, else skip comparision
    open fun varDataTypeCheckOk(v: Variable): Boolean {
        return true
    }

    // if true, compare dimension, else skip comparision
    open fun checkDimensionsForFile(filename: String): Boolean {
        return true
    }

    // if true, compare transform, else skip comparision
    open fun compareProjection(ct1: Projection?, ct2: Projection?): Boolean {
        if (ct1 == null != (ct2 == null)) {
            return false
        }
        return if (ct1 == null) {
            true
        } else ct1 == ct2
    }
}

class Netcdf4ObjectFilter : CdmObjFilter() {

    override fun attCheckOk(att: Attribute): Boolean {
        // if (v != null && v.isMemberOfStructure()) return false;
        val name = att.shortName
        if (name == IospUtils.HDF5_DIMENSION_LIST) return false
        if (name == IospUtils.HDF5_DIMENSION_SCALE) return false
        if (name == IospUtils.HDF5_DIMENSION_LABELS) return false

        // added by cdm
        if (name == CDM.CHUNK_SIZES) return false
        if (name == CDM.FILL_VALUE) return false
        if (name == "_lastModified") return false
        if (CDM.NETCDF4_SPECIAL_ATTS.contains(name)) return false

        // hidden by nc4
        if (name == IospUtils.NETCDF4_DIMID) return false // preserve the order of the dimensions
        if (name == IospUtils.NETCDF4_COORDINATES) return false // ??
        return if (name == IospUtils.NETCDF4_STRICT) false else !name.startsWith("_")

        // not implemented yet
        // if (att.getDataType().isEnum()) return false;
    }

    override fun varDataTypeCheckOk(v: Variable): Boolean {
        return if (v.arrayType == ArrayType.CHAR) false else v.arrayType != ArrayType.STRING // temp workaround
    }

    // override att comparision if needed
    override fun attsAreEqual(att1: Attribute, att2: Attribute): Boolean {
        return if (att1.shortName.equals(CDM.UNITS, ignoreCase = true) && att2.shortName.equals(
                CDM.UNITS,
                ignoreCase = true
            )
        ) {
            att1.stringValue!!.trim { it <= ' ' } == att2.stringValue!!.trim { it <= ' ' }
        } else att1 == att2
    }

    // override dimension comparision if needed
    override fun dimsAreEqual(dim1: Dimension, dim2: Dimension): Boolean {
        val unshared1 = dim1.shortName != null && dim1.shortName.contains("_Dim")
        val unshared2 = dim2.shortName != null && dim2.shortName.contains("_Dim")
        return if (unshared1 || unshared2) { // only test length
            dim1.length == dim2.length
        } else dim1 == dim2
    }

    override fun enumsAreEqual(enum1: EnumTypedef, enum2: EnumTypedef): Boolean {
        var name1 = enum1.shortName
        var name2 = enum2.shortName
        if (name1.endsWith("_t")) {
            name1 = name1.substring(0, name1.length - 2)
        }
        if (name2.endsWith("_t")) {
            name2 = name2.substring(0, name2.length - 2)
        }
        return name1 == name2 && enum1.map == enum2.map && enum1.baseArrayType == enum2.baseArrayType
    }
}

fun compareLists(org: List<*>, copy: List<*>, f: Formatter): Boolean {
    val ok1 = checkContains("first", org, copy, f)
    val ok2 = checkContains("second", copy, org, f)
    return ok1 && ok2
}

fun checkContains(what: String?, container: List<Any?>, wantList: List<Any?>, out: Formatter): Boolean {
    var ok = true
    for (want1 in wantList) {
        val index2 = container.indexOf(want1)
        if (index2 < 0) {
            out.format("  ** %s missing in %s %n", want1, what)
            ok = false
        }
    }
    return ok
}

private const val skipTransforms = true
private val IDENTITY_FILTER = CdmObjFilter()

class CompareCdmDataset(
    val out: Formatter = Formatter(System.out),
    val showCompare: Boolean = false,
    val showEach: Boolean = false,
    var compareData: Boolean = false,
) {

    // calling from Java has trouble with the optional arguments
    fun compare(org: CdmFile, copy: CdmFile): Boolean {
        return compare(org, copy, IDENTITY_FILTER)
    }

    fun compare(
        org: CdmFile,
        copy: CdmFile,
        CdmObjFilter: CdmObjFilter = IDENTITY_FILTER,
    ): Boolean {
        var ok = compareGroups(org.rootGroup, copy.rootGroup, CdmObjFilter)

        // coordinate systems
        if (org is CdmDatasetCS && copy is CdmDatasetCS) {
            out.format("Compare CdmDatasetCS conv1 = '%s' conv2 = '%s' %n", org.conventionBuilder, copy.conventionBuilder)

            // each one in copy must be in original, but not reverse
            val todo = mutableSetOf<CoordinateSystem>()
            todo.addAll(org.coordinateSystems)
            copy.coordinateSystems.forEach { csys2 ->
                var matchOne = false
                run breakout@{
                    todo.forEach { csys1 ->
                        if (compareCoordSys(csys1, csys2)) {
                            matchOne = true
                            todo.remove(csys2)
                            return@breakout
                        }
                    }
                }
                if (!matchOne) {
                    ok = false
                    out.format("cant find match for file2 coordsys '%s' %n", csys2)
                }
            }
        }
        return ok
    }

    // look for equivalent (not equal) coordinate systems
    fun compareCoordSys(csys1: CoordinateSystem, csys2: CoordinateSystem): Boolean {
        println(" csys '${csysSummary(csys1)}' with\n      '${csysSummary(csys2)}'")
        if (csys1.coordinateAxes.size != csys2.coordinateAxes.size) {
            return false
        }
        if (csys1.domain != csys2.domain) {
            return false
        }
        if (csys1.isLatLon != csys2.isLatLon) {
            return false
        }
        if (!skipTransforms && csys1.projection != csys2.projection) {
            return false
        }
        var matchAll = true
        val todo = mutableSetOf<CoordinateAxis>()
        todo.addAll(csys2.coordinateAxes)
        csys1.coordinateAxes.forEach { axis1 ->
            var matchOne = false
            run breakout@{
                todo.forEach { axis2 ->
                    if (compareCoordAxis(axis1, axis2)) {
                        matchOne = true
                        todo.remove(axis2)
                        return@breakout
                    }
                }
            }
            if (!matchOne) {
                matchAll = false
                out.format("cant find match for file1 coordaxis '%s' %n", axis1.fullName)
            }
        }
        if (!todo.isEmpty()) {
            out.format("cant find match for file2 coordaxis '%s' %n", todo)
        }
        return matchAll
    }

    fun csysSummary(csys : CoordinateSystem) : String {
        return "${csys.name},${csys.projection?.name},${csys.coordinateAxes.map { it.shortName } }"
    }

    // look for equivalent (not equal) coordinate axis
    fun compareCoordAxis(axis1: CoordinateAxis, axis2: CoordinateAxis): Boolean {
        println(" axes '${axisSummary(axis1)}' with\n      '${axisSummary(axis2)}'")
        if (axis1.axisType != axis2.axisType) {
            return false
        }
        if (!compatibleUnits(axis1.unitsString, axis2.unitsString)) {
            return false
        }
        if (axis1.dimensionSet != axis2.dimensionSet) {
            return false
        }
        return true
    }

    fun compatibleUnits(units1: String?, units2: String?): Boolean {
        val unit1 = SimpleUnit.factory(units1)
        val unit2 = SimpleUnit.factory(units2)
        if (unit1 == null && unit2 == null) {
            return true
        }
        if (unit1 == null || unit2 == null) {
            return false
        }
        return unit1.isCompatible(unit2)
    }

    fun axisSummary(axis : CoordinateAxis) : String {
        return "${axis.shortName},${axis.axisType},${axis.unitsString},${axis.dimensionSet.map { it.shortName } }"
    }

    fun compareVariables(org: CdmFile, copy: CdmFile): Boolean {
        out.format("Original = %s%n", org.location)
        out.format("CompareTo= %s%n", copy.location)
        var ok = true
        for (orgV in org.variables) {
            // if (orgV.isCoordinateVariable()) continue;
            val copyVar = copy.findVariable(orgV.shortName)
            ok = if (copyVar == null) {
                out.format(" MISSING '%s' in 2nd file%n", orgV.fullName)
                false
            } else {
                ok and compareVariable(orgV, copyVar, null, compareData)
            }
        }
        out.format("%n")
        for (orgV in copy.variables) {
            // if (orgV.isCoordinateVariable()) continue;
            val copyVar = org.findVariable(orgV.shortName)
            if (copyVar == null) {
                out.format(" MISSING '%s' in 1st file%n", orgV.fullName)
                ok = false
            }
        }
        return ok
    }

    private fun compareGroups(org: Group?, copy: Group?, filter: CdmObjFilter?): Boolean {
        if (showCompare) out.format("compare Group '%s' to '%s' %n", org!!.shortName, copy!!.shortName)
        var ok = true
        if (org!!.shortName != copy!!.shortName) {
            out.format(" ** names are different %s != %s %n", org.shortName, copy.shortName)
            ok = false
        }

        // dimensions
        if (filter!!.checkDimensionsForFile(org.cdmFile.location)) {
            ok = ok and checkGroupDimensions(org, copy, "copy", filter)
            ok = ok and checkGroupDimensions(copy, org, "org", filter)
        }

        // attributes
        ok = ok and checkAttributes(org.fullName, org.attributes(), copy.attributes(), filter)

        // enums
        ok = ok and checkEnums(org, copy, filter)

        // variables
        // cant use object equality, just match on short name
        org.variables.filter { !filter.ignoreVariable(it) }.forEach { orgV ->
            val copyVar = copy.findVariableLocal(orgV.shortName)
            ok = if (copyVar == null) {
                out.format(" ** cant find variable %s in 2nd file%n", orgV.fullName)
                false
            } else {
                ok and compareVariable(orgV, copyVar, filter, compareData)
            }
        }
        for (copyV in copy.variables) {
            val orgV = org.findVariableLocal(copyV.shortName)
            if (orgV == null) {
                out.format(" ** cant find variable %s in 1st file%n", copyV.fullName)
                ok = false
            }
        }

        // nested groups
        val groups = mutableListOf<Any>()
        val name = if (org.isRoot) "root group" else org.fullName
        ok = ok and checkAll(name, org.groups, copy.groups, groups)
        var i = 0
        while (i < groups.size) {
            val orgGroup = groups[i] as Group
            val copyGroup = groups[i + 1] as Group
            ok = ok and compareGroups(orgGroup, copyGroup, filter)
            i += 2
        }
        return ok
    }


    fun compareVariable(org: Variable, copy: Variable, filter: CdmObjFilter?): Boolean {
        return compareVariable(org, copy, filter, compareData)
    }

    private fun compareVariable(org: Variable, copy: Variable, filter: CdmObjFilter?, compareData: Boolean): Boolean {
        var ok = true
        if (showCompare) out.format("compare Variable %s to %s %n", org.fullName, copy.fullName)
        if (org.fullName != copy.fullName) {
            out.format(" ** names are different %s != %s %n", org.fullName, copy.fullName)
            ok = false
        }
        if (filter!!.varDataTypeCheckOk(org) && org.arrayType != copy.arrayType) {
            out.format(
                " ** %s dataTypes are different %s != %s %n", org.fullName, org.arrayType,
                copy.arrayType
            )
            ok = false
        }

        // dimensions
        ok = ok and checkDimensions(org.dimensions, copy.dimensions, copy.fullName + " copy", filter)
        ok = ok and checkDimensions(copy.dimensions, org.dimensions, org.fullName + " org", filter)

        // attributes
        ok = ok and checkAttributes(org.fullName, org.attributes(), copy.attributes(), filter)

        // data
        if (compareData) {
            ok = ok and CompareCdmFiles.compareVariableData(out, org, copy, false)
            if (showCompare) out.format("  compare variable data ok = %s%n", ok)
        }

        /* coordinate systems
    if (org instanceof VariableEnhanced && copy instanceof VariableEnhanced) {
      VariableEnhanced orge = (VariableEnhanced) org;
      VariableEnhanced copye = (VariableEnhanced) copy;

      for (CoordinateSystem cs1 : orge.getCoordinateSystems()) {
        CoordinateSystem cs2 = copye.getCoordinateSystems().stream().filter(cs -> cs.getName().equals(cs1.getName()))
            .findFirst().orElse(null);
        if (cs2 == null) {
          ok = false;
          out.format("  ** Cant find CoordinateSystem '%s' in file2 for var %s %n", cs1.getName(), org.getShortName());
        } else {
          ok &= compareCoordinateSystem(cs1, cs2, filter);
        }
      }
    } */

        // out.format(" Variable '%s' ok %s %n", org.getName(), ok);
        return ok
    }

    private fun compareCoordinateSystem(cs1: CoordinateSystem, cs2: CoordinateSystem, filter: CdmObjFilter): Boolean {
        if (showCompare) out.format("compare CoordinateSystem '%s' to '%s' %n", cs1.name, cs2.name)
        var ok = true
        for (ct1 in cs1.coordinateAxes) {
            val ct2 = cs2.coordinateAxes.stream().filter { ct: CoordinateAxis -> ct.fullName == ct1.fullName }
                .findFirst().orElse(null)
            if (ct2 == null) {
                ok = false
                out.format("  ** Cant find coordinateAxis %s in file2 %n", ct1.fullName)
            } else {
                ok = ok and compareCoordinateAxis(ct1, ct2, filter)
            }
        }
        val cp1 = cs1.projection
        val cp2 = cs2.projection
        val ctOk = filter.compareProjection(cp1, cp2)
        ok = ok && ctOk
        return ok
    }

    private fun compareCoordinateAxis(a1: CoordinateAxis, a2: CoordinateAxis, filter: CdmObjFilter?): Boolean {
        if (showCompare) out.format("  compare CoordinateAxis '%s' to '%s' %n", a1.shortName, a2.shortName)
        compareVariable(a1, a2, filter)
        return true
    }


    // make sure each object in wantList is contained in container, using equals().

    // make sure each object in each list are in the other list, using equals().
    // return an arrayList of paired objects.

    // make sure each object in wantList is contained in container, using equals().
    // make sure each object in each list are in the other list, using equals().
    // return an arrayList of paired objects.
    private fun checkAttributes(
        name: String, list1: AttributeContainer, list2: AttributeContainer,
        CdmObjFilter: CdmObjFilter?
    ): Boolean {
        var ok = true
        for (att1 in list1) {
            if (CdmObjFilter!!.attCheckOk(att1)) {
                ok = ok and checkAtt(name, att1, "file1","file2", list2, CdmObjFilter)
            }
        }
        for (att2 in list2) {
            if (CdmObjFilter!!.attCheckOk(att2)) {
                ok = ok and checkAtt(name, att2, "file2","file1", list1, CdmObjFilter)
            }
        }
        return ok
    }

    // Theres a bug in old HDF4 (eg "MOD021KM.A2004328.1735.004.2004329164007.hdf) where dimensions
    // are not properly moved up (eg dim BAND_250M is in both root and Data_Fields).
    // So we are going to allow that to be ok (until proven otherwise) but we have to adjust
    // dimension comparision. Currently Dimension.equals() checks the Group.
    private fun checkDimensions(
        list1: List<Dimension>,
        list2: List<Dimension>,
        where: String,
        filter: CdmObjFilter?
    ): Boolean {
        var ok = true
        for (d1 in list1) {
            if (d1.isShared) {
                val hasit = listContains(list2, d1, filter)
                if (!hasit) {
                    out.format("  ** Missing Variable dim '%s' not in %s %n", d1, where)
                }
                ok = ok and hasit
            }
        }
        return ok
    }

    // Check contains not using Group
    private fun listContains(list: List<Dimension>, d2: Dimension, filter: CdmObjFilter?): Boolean {
        for (d1 in list) {
            if (equalInValue(d1, d2, filter)) {
                return true
            }
        }
        return false
    }

    fun findDimension(g: Group?, dim: Dimension?, filter: CdmObjFilter?): Dimension? {
        if (dim == null) {
            return null
        }
        for (d in g!!.dimensions) {
            if (equalInValue(d, dim, filter)) {
                return d
            }
        }
        val parent = g.parentGroup
        return parent?.let { findDimension(it, dim, filter) }
    }

    fun findEnum(g: Group?, typedef: EnumTypedef?, filter: CdmObjFilter?): EnumTypedef? {
        if (typedef == null) {
            return null
        }
        for (other in g!!.enumTypedefs) {
            if (filter!!.enumsAreEqual(typedef, other)) {
                return other
            }
        }
        val parent = g.parentGroup
        return parent?.let { findEnum(it, typedef, filter) }
    }

    private fun equalInValue(d1: Dimension, other: Dimension, filter: CdmObjFilter?): Boolean {
        return filter!!.dimsAreEqual(d1, other)
    }

    private fun checkGroupDimensions(group1: Group?, group2: Group?, where: String, filter: CdmObjFilter?): Boolean {
        var ok = true
        for (d1 in group1!!.dimensions) {
            if (d1.isShared) {
                if (!group2!!.dimensions.contains(d1)) {
                    // not in local, is it in a parent?
                    if (findDimension(group2, d1, filter) != null) {
                        out.format("  ** Dimension '%s' found in parent group of %s %s%n", d1, where, group2.fullName)
                    } else {
                        val unshared1 = d1.shortName != null && d1.shortName.contains("_Dim")
                        if (!unshared1) {
                            out.format("  ** Missing Group dim '%s' not in %s %s%n", d1, where, group2.fullName)
                            ok = false
                        }
                    }
                }
            }
        }
        return ok
    }

    private fun checkEnums(org: Group?, copy: Group?, filter: CdmObjFilter?): Boolean {
        var ok = true
        for (enum1 in org!!.enumTypedefs) {
            if (showCompare) out.format("compare Enum %s%n", enum1.shortName)
            val enum2 = findEnum(copy, enum1, filter)
            if (enum2 == null) {
                findEnum(org, enum1, filter)
                out.format("  ** Enum %s not in file2 %n", enum1.shortName)
                ok = false
            }
        }
        for (enum2 in copy!!.enumTypedefs) {
            val enum1 = findEnum(org, enum2, filter)
            if (enum1 == null) {
                findEnum(org, enum2, filter)
                out.format("  ** Enum %s not in file1 %n", enum2.shortName)
                ok = false
            }
        }
        return ok
    }

    private fun checkAll(what: String, list1: List<Any>, list2: List<Any>, result: MutableList<Any>): Boolean {
        var ok = true
        for (aList1 in list1) {
            ok = ok and checkEach(what, aList1, "file1", list1, "file2", list2, result)
        }
        for (aList2 in list2) {
            ok = ok and checkEach(what, aList2, "file2", list2, "file1", list1, null)
        }
        return ok
    }

    // check that want is in both list1 and list2, using object.equals()
    private fun checkEach(
        what: String, want1: Any, name1: String, list1: List<*>, name2: String, list2: List<*>,
        result: MutableList<Any>?
    ): Boolean {
        var ok = true
        try {
            val index2 = list2.indexOf(want1)
            if (index2 < 0) {
                out.format("  ** %s: %s 0x%x (%s) not in %s %n", what, want1, want1.hashCode(), name1, name2)
                ok = false
            } else { // found it in second list
                val want2 = list2[index2]!!
                val index1 = list1.indexOf(want2)
                if (index1 < 0) { // can this happen ??
                    out.format("  ** %s: %s 0x%x (%s) not in %s %n", what, want2, want2.hashCode(), name2, name1)
                    ok = false
                } else { // found it in both lists
                    val want = list1[index1]!!
                    if (want != want1) {
                        out.format("  ** ${what}: ${want1} 0x${want1.hashCode()} (${name1}) not equal to ${want2} 0x${want2.hashCode()} (${name2})\n")
                        ok = false
                    } else {
                        if (showEach) out.format("  OK <%s> equals <%s>%n", want1, want2)
                        if (result != null) {
                            result.add(want1)
                            result.add(want2)
                        }
                    }
                }
            }
        } catch (t: Throwable) {
            t.printStackTrace()
            out.format(" *** Throwable= %s %n", t.message)
            ok = false
        }
        return ok
    }

    // check that want is in both list1 and list2, using object.equals()
    private fun checkAtt(
        what: String, want: Attribute, name1: String, name2: String, list2: AttributeContainer, CdmObjFilter: CdmObjFilter?
    ): Boolean {
        if (CdmObjFilter != null && !CdmObjFilter.attCheckOk(want)) {
            return true
        }

        var ok = true
        val found = list2.findAttributeIgnoreCase(want.shortName)
        if (found == null) {
            out.format("  ** Attribute %s: %s (%s) not in %s %n", what, want, name1, name2)
            ok = false
        } else {
            if (!CdmObjFilter!!.attsAreEqual(want, found)) {
                out.format("  ** Attribute %s: %s 0x%x (%s) not equal to %s 0x%x (%s) %n",
                    what, want, want.hashCode(), name1, found, found.hashCode(), name2
                )
                ok = false
            } else if (showEach) {
                out.format("  OK <%s> equals <%s>%n", want, found)
            }
        }
        return ok
    }
}