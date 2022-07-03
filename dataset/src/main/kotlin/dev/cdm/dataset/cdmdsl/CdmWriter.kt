package dev.cdm.dataset.cdmdsl

import dev.cdm.array.Indent
import dev.cdm.array.PrintArray.printArray
import dev.cdm.core.api.*
import dev.cdm.dataset.api.*
import dev.cdm.dataset.transform.horiz.ProjectionCTV
import java.util.*

fun CdmDataset.write(): String {
    val out = StringBuilder()

    out.appendLine("cdml ${this.location}")
    val indent = Indent(2)
    out.appendLine("${indent}attributes:")
    this.rootGroup.attributes().forEach { it.write(out, indent.incrNew()) }

    if (this.rootGroup.dimensions.isNotEmpty()) {
        out.appendLine("\n${indent}dimensions:")
        this.rootGroup.dimensions.forEach { it.write(out, indent.incrNew()) }
    }

    if (rootGroup.variables.isNotEmpty()) {
        out.appendLine("\n${indent}variables:")
        this.rootGroup.variables.filter { it !is CoordinateAxis }.forEach { it.write(out, indent.incrNew()) }
    }

    if (this is CdmDatasetCS) {
        out.appendLine("${indent}coordinateAxes:")
        this.coordinateAxes.forEach { it.write(out, indent.incrNew()) }

        out.appendLine("${indent}coordinateSystems:")
        this.coordinateSystems.forEach { it.write(out, indent.incrNew()) }

        out.appendLine("${indent}transforms:")
        this.getCoordinateTransforms().forEach { it.write(out, indent.incrNew()) }

    }

    if (rootGroup.groups.isNotEmpty()) {
        out.appendLine("\n${indent}groups:")
        rootGroup.groups.forEach { it.write(out, indent.incrNew()) }
    }

    out.appendLine("")
    out.appendLine("${indent}:conventionsUsed = ${this.conventionBuilder}")
    out.appendLine("${indent}:enhance = ${this.enhanceMode}")
    val obj = this.sendIospMessage(CdmDataset.IOSP_MESSAGE_GET_REFERENCED_FILE)
    if (obj != null) {
        out.appendLine("${indent}:originalFile = \"${(obj as CdmFile).location}\"")
    }

    return out.toString()
}

fun Attribute.write(builder: StringBuilder, indent: Indent) {
    val values = this.arrayValues?.let { shortenPrintArray(it, 80) }
    builder.appendLine("$indent${this.arrayType} ${this.name.trim()} = $values")
}

fun Dimension.write(builder: StringBuilder, indent: Indent) {
    builder.append("$indent${this.shortName}")
    if (this.isUnlimited()) {
        builder.append(" = UNLIMITED;   // (${this.length} currently)")
    } else if (this.isVariableLength()) {
        builder.append(" = UNKNOWN;")
    } else {
        builder.append(" = ${this.getLength()}")
    }
    builder.appendLine()
}

fun Variable.write(builder: StringBuilder, indent: Indent, dataset : CdmDatasetCS? = null) {
    builder.appendLine("$indent${this.arrayType} ${this.getNameAndDimensions()}")
    this.attributes().forEach { it.write(builder, indent.incrNew()) }
    if (dataset != null) {
        val vds = this as VariableDS
        dataset.makeCoordinateSystemsFor(vds).forEach {
            builder.appendLine("${indent}  :coordinateSystem = \"${it.name}\"")
        }
    }
    builder.appendLine("")
}

fun Group.write(out: StringBuilder, indent: Indent, dataset : CdmDatasetCS? = null) {
    out.appendLine("${indent}group '${this.shortName}'")
    indent.incr()

    if (!this.attributes().isEmpty()) {
        out.appendLine("${indent}attributes:")
        this.attributes().forEach { it.write(out, indent.incrNew()) }
    }

    if (this.dimensions.isNotEmpty()) {
        out.appendLine("\n${indent}dimensions:")
        this.dimensions.forEach { it.write(out, indent.incrNew()) }
    }

    if (this.variables.isNotEmpty()) {
        out.appendLine("\n${indent}variables:")
        this.variables.filter { it !is CoordinateAxis }.forEach { it.write(out, indent.incrNew()) }
    }

    if (this.groups.isNotEmpty()) {
        out.appendLine("\n${indent}groups:")
        this.groups.forEach { it.write(out, indent.incrNew()) }
    }
}

fun CoordinateSystem.write(builder: StringBuilder, indent: Indent) {
    builder.appendLine("${indent}name = '${this.name}'")
    builder.appendLine("${indent}coords = \"${this.coordinateAxes.map {it.shortName}}\"")
    builder.appendLine("${indent}domain = \"${Dimensions.makeDimensionsString(this.domain)}\"")
    builder.appendLine("")
}

fun CoordinateAxis.write(builder: StringBuilder, indent: Indent) {
    builder.appendLine("$indent${this.arrayType} ${this.getNameAndDimensions()}")
    this.attributes().forEach { it.write(builder, indent.incrNew()) }
    builder.appendLine("${indent}  :axisType = \"${this.axisType}\"")
    builder.appendLine("")
}

// LOOK ProjectionCTV includes vert transforms wtf?
fun ProjectionCTV.write(builder: StringBuilder, indent: Indent) {
    builder.appendLine("$indent${this.name}")
    this.ctvAttributes.forEach { it.write(builder, indent.incrNew()) }
    builder.appendLine("")
}

