package dev.cdm.dataset.cdmdsl

import dev.cdm.array.Indent
import dev.cdm.array.PrintArray.printArray
import dev.cdm.core.api.*
import dev.cdm.dataset.api.*
import dev.cdm.dataset.transform.horiz.ProjectionCTV
import java.util.*

fun CdmDataset.write(): String {
    val builder = StringBuilder()

    builder.appendLine("cdml ${this.location}")
    val indent = Indent(2)
    builder.appendLine("${indent}attributes:")
    this.rootGroup.attributes().forEach { it.write(builder, indent.incrNew()) }

    builder.appendLine("\n${indent}dimensions:")
    this.rootGroup.dimensions.forEach { it.write(builder, indent.incrNew()) }

    if (this is CdmDatasetCS) {
        builder.appendLine("\n${indent}variables:")
        this.rootGroup.variables.filter {it !is CoordinateAxis}.forEach { it.write(builder, indent.incrNew(), this) }

        builder.appendLine("${indent}coordinateAxes:")
        this.coordinateAxes.forEach { it.write(builder, indent.incrNew()) }

        builder.appendLine("${indent}coordinateSystems:")
        this.coordinateSystems.forEach { it.write(builder, indent.incrNew()) }

        builder.appendLine("${indent}transforms:")
        this.getCoordinateTransforms().forEach { it.write(builder, indent.incrNew()) }

    } else {
        builder.appendLine("\n${indent}variables:")
        this.rootGroup.variables.filter {it !is CoordinateAxis}.forEach { it.write(builder, indent.incrNew()) }
    }

    builder.appendLine("")
    builder.appendLine("${indent}:conventionsUsed = ${this.conventionBuilder}")
    builder.appendLine("${indent}:enhance = ${this.enhanceMode}")
    val obj = this.sendIospMessage(CdmDataset.IOSP_MESSAGE_GET_REFERENCED_FILE)
    if (obj != null) {
        builder.appendLine("${indent}:originalFile = \"${(obj as CdmFile).location}\"")
    }

    return builder.toString()
}

fun Attribute.write(builder: StringBuilder, indent: Indent) {
    val out = Formatter()
    printArray(out, this.arrayValues, null, Indent(0))
    builder.appendLine("$indent${this.arrayType} ${this.name.trim()} = $out")
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

