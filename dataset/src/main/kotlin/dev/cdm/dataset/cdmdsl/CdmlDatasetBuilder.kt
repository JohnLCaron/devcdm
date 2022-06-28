package dev.cdm.dataset.internal

import dev.cdm.core.api.*
import dev.cdm.dataset.api.*
import dev.cdm.dataset.api.CdmDataset.Enhance
import dev.cdm.dataset.api.CdmDataset.IOSP_MESSAGE_GET_COORDS_HELPER
import dev.cdm.dataset.cdmdsl.*
import dev.cdm.dataset.transform.horiz.ProjectionCTV
import org.slf4j.LoggerFactory
import java.util.*

private val logger = LoggerFactory.getLogger("CdmDsl")

fun CdmdslDataset.build(): CdmDatasetCS {
    var builder = CdmDatasetCS.builder()

    // TODO open as dataset or file? with enhanced metadata or not?
    if (this.location != null) {
        // no coordinate systems ??
        val enhancemants = EnumSet.of(
            Enhance.ConvertEnums,
            Enhance.ConvertUnsigned,
            Enhance.ApplyScaleOffset,
            Enhance.ConvertMissing
        )
        // open with CS to use default parsing, then override as needed.
        val orgDataset = CdmDatasets.openDatasetCS(this.location)
        builder = orgDataset.toBuilder()
    }

    // pull in all the non-coord changes and build so we have finished variables
    buildGroup(this.root, builder.rootGroup)
    val result = builder.build()

    // make new changes to helperb
    val helper = result.sendIospMessage(IOSP_MESSAGE_GET_COORDS_HELPER) as CoordinatesHelper
    val helperb = helper.toBuilder()
    this.coordSystems.forEach {
        buildCoordSystem(it.value, helperb, builder)
    }

    this.transforms.forEach {
        buildCoordTransform(it.value, helperb, builder, result)
    }

    // build new coord systems and place into result
    val axes = helperb.coordAxes.map { it.build(result.rootGroup) } // LOOK
    // result.setCoordinatesHelper(helperb.build(axes))
    return result
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

    cgroup.variables.forEach {
        buildVariable(it.value, groupb.vbuilders)
    }
}

fun buildAtt(catt: CdmdslAttribute, atts: AttributeContainerMutable) {
    val oatt = atts.findAttribute(catt.name)
    if (oatt == null) {
        atts.addAttribute(Attribute(catt.name, catt.values))
    } else {
        if (catt.action == Action.Remove) {
            atts.remove(oatt)
            return
        }
        val builder = oatt.toBuilder()
        if (catt.rename != null) {
            builder.setName(catt.rename)
        }
        if (catt.values != null) {
            builder.setStringValue(catt.values)
        }
        atts.addAttribute(builder.build())
    }
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

fun buildVariable(cvar: CdmdslVariable, orgs: ArrayList<Variable.Builder<*>>) {
    val org = orgs.find { it.shortName == cvar.name }
    if (org == null) {
        val valb = Variable.builder()
        orgs.add(valb)
        return
    } else {
        if (cvar.action == Action.Remove) {
            orgs.remove(org)
            return
        }
        cvar.rename?.let { org.setName(cvar.rename) }
        cvar.type?.let { org.setArrayType(cvar.type) }
    }

    cvar.attributes.forEach {
        buildAtt(it.value, org.getAttributeContainer())
    }
}

fun buildCoordSystem(
    csys: CdmdslCoordSystem,
    helper: CoordinatesHelper.Builder,
    orgDataset: CdmDatasetCS.Builder<*>,
    // result: CdmDataset
) {
    val orgHelper: CoordinatesHelper.Builder = orgDataset.coords
    val orgs: ArrayList<CoordinateSystem.Builder<*>> = orgHelper.coordSys
    val org = orgs.find { it.coordAxesNames == csys.name }
    if (org == null) {
        val coordSysBuilder = findConvention(orgDataset)

        val builder = CoordinateSystem.builder()
        builder.setCoordAxesNames(csys.csysName)
        builder.setCoordinateTransformName(csys.projection)
        helper.addCoordinateSystem(builder)

        /* add named axes
        csys.name.split(" ").forEach {
            if (helper.coordAxes.find { axis -> axis.shortName == it } == null) {
                val v = orgDataset.findVariable(it)
                if (v != null) {
                    val vds = v as VariableDS
                    val axisb = CoordinateAxis.fromVariableDS(vds.toBuilder())
                    axisb.setAxisType(coordSysBuilder.identifyAxisType(vds))
                    helper.addCoordinateAxis(axisb)
                }
            }
        } */
        return
    } else { // LOOK wrong, already built
        if (csys.action == Action.Remove) {
            orgs.remove(org)
            return
        }
        csys.rename?.let { org.setCoordAxesNames(csys.rename) }
    }
}


fun buildCoordTransform(
    ctrans: CdmslTransform,
    coordb: CoordinatesHelper.Builder,
    orgDataset: CdmDatasetCS.Builder<*>,
    result: CdmDataset
) {
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
}