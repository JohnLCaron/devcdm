package cdm.dataset.cdmdsl

import dev.cdm.core.api.*
import dev.cdm.dataset.api.CdmDataset
import dev.cdm.dataset.api.CdmDatasets
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("CdmDsl")

fun build(cdmdsl : CdmDsl) : CdmDataset {
    val builder = CdmDataset.builder()

    if (cdmdsl.location != null) {
        val orgFile = CdmDatasets.openDataset(cdmdsl.location)
        builder.setOrgFile(orgFile)
        builder.copyFrom(orgFile)
    }

    modifyGroup(builder.rootGroup, cdmdsl.root)

    return builder.build()
}

// TODO rearrange the group heirarchy? Cant use absolute paths
fun modifyGroup(group : Group.Builder, cgroup : CdmdslGroup) {
    if (cgroup.rename != null) { // check if root
        group.setName(cgroup.rename)
    }

    cgroup.groups.values.forEach {
        val existingGroup = group.gbuilders.find {g -> g.shortName.equals(it.groupName)}
        when (it.action) {
            Action.Remove -> existingGroup?.let { group.gbuilders.remove(existingGroup) }
            Action.AddOrReplace -> {
                existingGroup?.let { group.gbuilders.remove(existingGroup) }
                //group.gbuilders.add(createGroup(it))
            }
            Action.Modify -> {
                if (existingGroup != null) {
                    modifyGroup(existingGroup, it)
                } else {
                    logger.warn("no group named")
                }
            }
        }
    }

    cgroup.dimensions.values.forEach {
        val existingDim = group.dimensions.find {g -> g.shortName.equals(it.dimName)}
        when (it.action) {
            Action.Remove -> existingDim?.let { group.dimensions.remove(existingDim) }
            Action.AddOrReplace -> {
                existingDim?.let { group.dimensions.remove(existingDim) }
                //group.dimensions.add(createDimension(it))
            }
            Action.Modify -> {
                if (existingDim != null) {
                    modifyDimension(existingDim, it)
                } else {
                    logger.warn("no dimension named ${it.dimName}")
                }
            }
        }
    }

    group.getEnums().forEach {
        val nenum = cgroup.enumTypedefs[it.shortName]
        if (nenum != null) {
            modifyEnums(it, nenum, group.enumTypedefs)
        }
    }

    group.getVariables().forEach {
        val nvar = cgroup.variables[it.shortName]
        if (nvar != null) {
            modifyVariable(it, nvar, group.vbuilders)
        }
    }

    cgroup.dimensions.values.forEach {
        val existingDim = group.dimensions.find {g -> g.shortName.equals(it.dimName)}
        when (it.action) {
            Action.Remove -> existingDim?.let { group.dimensions.remove(existingDim) }
            Action.AddOrReplace -> {
                existingDim?.let { group.dimensions.remove(existingDim) }
                //group.dimensions.add(createDimension(it))
            }
            Action.Modify -> {
                if (existingDim != null) {
                    modifyDimension(existingDim, it)
                } else {
                    logger.warn("no dimension named ${it.dimName}")
                }
            }
        }
    }

    group.getAttributes().forEach {
        val natt = cgroup.attributes[it.shortName]
        if (natt != null) {
            modifyAtt(it, natt, group.attributeContainer)
        }
    }
}

fun modifyAtt(att : Attribute, catt : CdmdslAttribute, atts: AttributeContainerMutable) : Attribute {
    val builder = att.toBuilder()
    if (catt.rename != null) {
        builder.setName(catt.rename )
    }
    return builder.build()
}

fun modifyDimension(dim : Dimension, cdim : CdmdslDimension) : Dimension {
    val builder = dim.toBuilder()
    builder.setLength(cdim.length)
    return builder.build()
}

fun modifyEnums(en : EnumTypedef, cdmlEnum : CdmdslEnum, enumTypedefs : ArrayList<EnumTypedef>) {
    if (cdmlEnum.action == Action.Remove) {
        enumTypedefs.remove(en)
    } else if (cdmlEnum.action == Action.AddOrReplace) {
        enumTypedefs.remove(en)
        enumTypedefs.add( EnumTypedef(cdmlEnum.enumName, cdmlEnum.values, cdmlEnum.basetype))
    } else if (cdmlEnum.action == Action.Modify) {
        enumTypedefs.remove(en)
        val values = en.map.toMutableMap()
        values.putAll(cdmlEnum.values)
        enumTypedefs.add( EnumTypedef(en.shortName, values, cdmlEnum.basetype?: en.baseArrayType))
    } else {
        throw java.lang.IllegalArgumentException("CdmdslEnum action not set")
    }
}

fun modifyVariable(varb : Variable.Builder<*>, cvar : CdmdslVariable, variables : ArrayList<Variable.Builder<*>>) {

}