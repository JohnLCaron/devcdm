package dev.cdm.dataset.cdmdsl

import dev.cdm.array.ArrayType
import dev.cdm.core.api.*
import dev.cdm.core.constants._Coordinate
import dev.cdm.dataset.api.*
import dev.cdm.dataset.api.CdmDataset.IOSP_MESSAGE_GET_COORDS_HELPER
import dev.cdm.dataset.internal.CoordinatesHelper
import org.slf4j.LoggerFactory
import java.util.*

private val logger = LoggerFactory.getLogger("CdmDsl")

private val attsOnly = true

fun CdmdslDataset.build(): CdmDatasetCS {
    // TODO open as dataset or file? with enhanced metadata or not?
    if (this.location != null) {
        val orgDataset = CdmDatasets.openDataset(this.location, this.enhance, null)
        return build(orgDataset)
    }
    return build(null)
}

fun CdmdslDataset.build(orgDataset : CdmDataset?): CdmDatasetCS {
    val builder = CdmDatasetCS.builder()
    orgDataset?.let { builder.copyFrom(orgDataset) }

    // pull in all the non-coord changes and build so we have finished variables
    buildGroup(this.root, builder.rootGroup)
    if (attsOnly) {
        this.coordSystems.forEach {
            buildCoordSystem(it.value, builder.rootGroup)
        }

        return builder.build()
    } else {
        val result = builder.build()

        // make new changes to helperb
        val helper = result.sendIospMessage(IOSP_MESSAGE_GET_COORDS_HELPER) as CoordinatesHelper
        val helperb = helper.toBuilder()
        this.coordSystems.forEach {
            buildCoordSystem(it.value, helperb, builder, result)
        }

        this.transforms.forEach {
            buildCoordTransform(it.value, helperb, builder, result)
        }

        // build new coord systems and place into result
        val axes = helperb.coordAxes.map { it.build(result.rootGroup) } // LOOK
        // result.setCoordinatesHelper(helperb.build(axes))

        return builder.build()
    }
}

// TODO rearrange the group heirarchy? Cant use absolute paths
fun buildGroup(cgroup: CdmdslGroup, groupb: Group.Builder) {
    if (cgroup.rename != null) { // check if root
        groupb.setName(cgroup.rename)
    }

    cgroup.attributes.forEach {
        buildAtt(it.value, groupb.attributeContainer)
    }

    cgroup.dimensions.forEach {
        buildDimension(it.value, groupb)
    }

    cgroup.enumTypedefs.forEach {
        buildEnum(it.value, groupb.enumTypedefs)
    }

    // TODO relying on attributes. should be more direct ??
    cgroup.variables.forEach {
        val vb = buildVariable(it.value, groupb)
        it.value.coordSysRef?.let { t -> vb.addAttribute(Attribute(_Coordinate.Systems, t)) }
    }

    cgroup.axes.forEach {
        val vb = buildVariable(it.value, groupb)
        it.value.axisType?.let { t -> vb.addAttribute(Attribute(_Coordinate.AxisType, t)) }
    }
}

fun buildAtt(catt: CdmdslAttribute, atts: AttributeContainerMutable) {
    val oatt = atts.findAttribute(catt.name)
    if (oatt != null && catt.action == Action.Remove) {
        atts.remove(oatt)
        return
    }
    val builder : Attribute.Builder = if (oatt != null) oatt.toBuilder() else Attribute.builder(catt.name)
    if (catt.rename != null) {
        builder.setName(catt.rename)
    }
    if (catt.values != null) {
        builder.setStringValue(catt.values)
        builder.setArrayType(ArrayType.STRING)
    } else if (catt.dvalue != null) {
        builder.setNumericValue(catt.dvalue, catt.isUnsigned!!)
        builder.setArrayType(catt.type?: ArrayType.DOUBLE)
    }
    atts.addAttribute(builder.build())
}

fun buildDimension(cdim: CdmdslDimension, groupb: Group.Builder) {
    val odimo = groupb.findDimension(cdim.dimName)
    if (odimo.isEmpty()) {
        groupb.addDimension(Dimension(cdim.dimName, cdim.length))
    } else {
        val odim = odimo.get()
        if (cdim.action == Action.Remove) {
            groupb.removeDimension(cdim.dimName)
            return
        }
        val builder = odim.toBuilder()
        // if (cdim.rename != null) { // LOOK
        //     builder.setName(cdim.rename)
        //}
        if (cdim.length != null) {
            builder.setLength(cdim.length)
        }
        groupb.replaceDimension(builder.build())
    }
}

fun buildEnum(cenum: CdmdslEnum, enums: ArrayList<EnumTypedef>) {
    val oenum = enums.find { it.shortName == cenum.enumName }
    if (oenum == null) {
        enums.add(EnumTypedef(cenum.enumName, cenum.values, cenum.basetype))
        return
    } else {
        if (cenum.action == Action.Remove) {
            enums.remove(oenum)
            return
        }
        enums.remove(oenum)
        val union = mutableMapOf<Int, String>()
        oenum.map.forEach { union[it.key] = it.value }
        cenum.values.forEach { union[it.key] = it.value }
        enums.add(
            EnumTypedef(
                cenum.enumName, union,
                cenum.basetype ?: oenum.baseArrayType
            )
        )
    }
}

fun buildVariable(cvar: CdmdslVariable, groupb : Group.Builder) : Variable.Builder<*> {
    var org = groupb.vbuilders.find { it.shortName == cvar.name }
    if (org == null) {
        org = VariableDS.builder()
        groupb.vbuilders.add(org)
        cvar.name?.let { org.setName(cvar.name) }
        if (cvar.type == null) {
            // default when not specified for new
            org.setArrayType(ArrayType.CHAR)
        }

    } else {
        if (cvar.action == Action.Remove) {
            groupb.vbuilders.remove(org)
            return org
        }
    }
    val orgv = org!!
    orgv.setParentGroupBuilder(groupb)
    cvar.rename?.let { orgv.setName(cvar.rename) }
    cvar.type?.let { orgv.setArrayType(cvar.type) }
    cvar.autoGen?.let { orgv.setAutoGen(cvar.autoGen!!.get(0), cvar.autoGen!!.get(1)) }
    cvar.dimensions?.let { org.setDimensionsByName(cvar.dimensions) }
    cvar.attributes.forEach {
        buildAtt(it.value, orgv.getAttributeContainer())
    }
    return orgv
}

fun buildCoordSystem(csys: CdmdslCoordSystem, groupb : Group.Builder) : Variable.Builder<*> {
    var csv = groupb.vbuilders.find { it.shortName == csys.name }
    if (csv == null) {
        csv = VariableDS.builder()
        groupb.vbuilders.add(csv)
        csys.csysName?.let { csv.setName(csys.csysName) }
    } else {
        if (csys.action == Action.Remove) {
            groupb.vbuilders.remove(csv)
            return csv
        }
    }
    val orgv = csv!!
    orgv.setParentGroupBuilder(groupb)
    orgv.setArrayType(ArrayType.CHAR)
    csys.axes?.let { t -> orgv.addAttribute(Attribute(_Coordinate.Axes, t)) }
    csys.projection?.let { t -> orgv.addAttribute(Attribute(_Coordinate.Transforms, t)) }
    return orgv
}

fun buildCoordSystem(
    csys: CdmdslCoordSystem,
    helper: CoordinatesHelper.Builder,
    orgDataset: CdmDatasetCS.Builder<*>,
    result: CdmDataset
) {
    /*
    val orgHelper: CoordinatesHelper.Builder = orgDataset.coords
    val orgs: ArrayList<CoordinateSystem.Builder<*>> = orgHelper.coordSys
    val org = orgs.find { it.coordAxesNames == csys.name }
    if (org == null) {

    } else { // LOOK wrong, already built
        if (csys.action == Action.Remove) {
            orgs.remove(org)
        }
        csys.rename?.let { org.setCoordAxesNames(csys.rename) }
    }

     */
}


fun buildCoordTransform(
    ctrans: CdmslTransform,
    coordb: CoordinatesHelper.Builder,
    orgDataset: CdmDatasetCS.Builder<*>,
    result: CdmDataset
) {
    /*
    val coordHelper: CoordinatesHelper.Builder = orgDataset.coords
    val orgs = coordHelper.coordTransforms
    val org = orgs.find { it.name == ctrans.name }
    if (org == null) {
        val atts = AttributeContainerMutable(ctrans.name)
        ctrans.parameters.forEach {
            buildAtt(it.value, atts)
        }
        coordb.addCoordinateTransform(ProjectionCTV(ctrans.name, atts, "")) // TODO geounits ?
        return
    } else {  // LOOK wrong, already built
        if (ctrans.action == Action.Remove) {
            orgs.remove(org)
            return
        }
        val name = ctrans.rename ?: org.name
        val atts = AttributeContainerMutable(name, org.getCtvAttributes())
        ctrans.parameters.forEach {
            buildAtt(it.value, atts)
        }
        coordb.replaceCoordinateTransform(ProjectionCTV(name, atts, org.geounits))
    }

     */
}