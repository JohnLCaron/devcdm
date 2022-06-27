package dev.cdm.dataset.cdmdsl

import dev.cdm.array.Indent
import dev.cdm.array.PrintArray.printArray
import dev.cdm.core.api.Attribute
import dev.cdm.core.api.CdmFile
import dev.cdm.core.api.Dimension
import dev.cdm.core.api.Variable
import dev.cdm.dataset.api.CdmDataset
import dev.cdm.dataset.api.CdmDatasetCS
import dev.cdm.dataset.api.CoordinateAxis
import dev.cdm.dataset.api.VariableDS
import dev.cdm.dataset.transform.horiz.ProjectionCTV
import java.util.*

fun CdmDataset.write(): String {
    val builder = StringBuilder()

    builder.append("cdml ${this.location}\n")
    val indent = Indent(2)
    builder.append("${indent}attributes:\n")
    this.rootGroup.attributes().forEach { it.write(builder, indent.incrNew()) }

    builder.append("\n${indent}dimensions:\n")
    this.rootGroup.dimensions.forEach { it.write(builder, indent.incrNew()) }

    builder.append("\n${indent}variables:\n")

    if (this is CdmDatasetCS) {
        builder.append("\n${indent}variables:\n")
        this.rootGroup.variables.forEach { it.write(builder, indent.incrNew(), this) }

        builder.append("${indent}coordinateAxes:\n")
        this.coordinateAxes.forEach { it.write(builder, indent.incrNew()) }

        builder.append("${indent}transforms:\n")
        this.getCoordinateTransforms().forEach { it.write(builder, indent.incrNew()) }

    } else {
        builder.append("\n${indent}variables:\n")
        this.rootGroup.variables.forEach { it.write(builder, indent.incrNew()) }
    }

    builder.append("\n")
    builder.append("${indent}:conventionsUsed = ${this.getConventionUsed()}\n")
    builder.append("${indent}:enhance = ${this.enhanceMode}\n")
    val obj = this.sendIospMessage(CdmDataset.IOSP_MESSAGE_GET_REFERENCED_FILE)
    if (obj != null) {
        builder.append("${indent}:originalFile = \"${(obj as CdmFile).location}\"\n")
    }

    return builder.toString()
}

fun Attribute.write(builder: StringBuilder, indent: Indent) {
    val out = Formatter()
    printArray(out, this.arrayValues, null, Indent(0))
    builder.append("$indent${this.arrayType} ${this.name.trim()} = $out")
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
    builder.append("\n")
}

fun Variable.write(builder: StringBuilder, indent: Indent, dataset : CdmDatasetCS? = null) {
    builder.append("$indent${this.arrayType} ${this.getNameAndDimensions()}\n")
    this.attributes().forEach { it.write(builder, indent.incrNew()) }
    if (dataset != null) {
        val vds = this as VariableDS
        dataset.makeCoordinateSystemsFor(vds).forEach {
            builder.append("${indent}  :coordinateSystem = \"${it.name}\"\n")
        }
    }
    builder.append("\n")
}

fun CoordinateAxis.write(builder: StringBuilder, indent: Indent) {
    builder.append("$indent${this.arrayType} ${this.getNameAndDimensions()}\n")
    this.attributes().forEach { it.write(builder, indent.incrNew()) }
    builder.append("${indent}  :axisType = \"${this.axisType}\"\n")
    builder.append("\n")
}

// LOOK ProjectionCTV includes vert transforms wtf?
fun ProjectionCTV.write(builder: StringBuilder, indent: Indent) {
    builder.append("$indent${this.name}\n")
    this.ctvAttributes.forEach { it.write(builder, indent.incrNew()) }
    builder.append("\n")
}

