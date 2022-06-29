package dev.cdm.dataset.cdmdsl

import dev.cdm.array.Indent
import dev.cdm.array.PrintArray.printArray
import dev.cdm.core.api.*
import dev.cdm.dataset.api.CdmDataset
import dev.cdm.dataset.api.CdmDatasetCS
import dev.cdm.dataset.api.CoordinateAxis
import dev.cdm.dataset.api.VariableDS
import dev.cdm.dataset.transform.horiz.ProjectionCTV
import java.util.*

fun CdmDataset.writeDsl(): String {
    val builder = StringBuilder()

    builder.appendLine("cdmdsl(\"${this.location}\") {")
    val indent = Indent(2)
    this.rootGroup.attributes().forEach { it.writeDsl(builder, indent.incrNew()) }
    builder.appendLine()

    this.rootGroup.dimensions.forEach { it.writeDsl(builder, indent.incrNew()) }
    builder.appendLine()

    if (this is CdmDatasetCS) {
        this.rootGroup.variables.filter {it !is CoordinateAxis}.forEach { it.writeDsl(builder, indent.incrNew(), this) }
        builder.appendLine()

        this.coordinateAxes.forEach { it.writeDsl(builder, indent.incrNew()) }
        builder.appendLine()

        this.getCoordinateTransforms().forEach { it.writeDsl(builder, indent.incrNew()) }

    } else {
        this.rootGroup.variables.forEach { it.writeDsl(builder, indent.incrNew()) }
    }

    /*
    builder.appendLine("${indent}:conventionBuilder= ${this.conventionBuilder}")
    builder.appendLine("${indent}:enhance = ${this.enhanceMode}")
    val obj = this.sendIospMessage(CdmDataset.IOSP_MESSAGE_GET_REFERENCED_FILE)
    if (obj != null) {
        builder.appendLine("${indent}:originalFile = \"${(obj as CdmFile).location}\"")
    }

     */
    builder.appendLine("${indent.decr()}}");

    return builder.toString()
}

fun Attribute.writeDsl(builder: StringBuilder, indent: Indent) {
    val out = Formatter()
    printArray(out, this.arrayValues, null, Indent(0))
    builder.appendLine("${indent}attribute(\"${this.name.trim()}\").setValue(${out})");
}

fun Dimension.writeDsl(builder: StringBuilder, indent: Indent) {
    builder.appendLine("${indent}dimension(\"${this.shortName}\", ${this.getLength()})")
}

fun Variable.writeDsl(builder: StringBuilder, indent: Indent, dataset : CdmDatasetCS? = null) {
    builder.appendLine("${indent}variable(\"${this.getShortName()}\") {")
    indent.incr()
    builder.appendLine("${indent}setType(\"${this.arrayType.name}\")")
    builder.appendLine("${indent}setDimensions(\"${Dimensions.makeDimensionsString(this.dimensions)}\")")
    this.attributes().forEach { it.writeDsl(builder, indent) }
    if (dataset != null) {
        val vds = this as VariableDS
        dataset.makeCoordinateSystemsFor(vds).forEach {
            builder.appendLine("${indent}coordSystem(\"${it.name}\")")
        }
    }
    indent.decr()
    builder.appendLine("${indent}}");
}

fun CoordinateAxis.writeDsl(builder: StringBuilder, indent: Indent) {
    builder.appendLine("${indent}axis(\"${this.getShortName()}\") {")
    indent.incr()
    builder.appendLine("${indent}setType(\"${this.arrayType.name}\")")
    builder.appendLine("${indent}setDimensions(\"${Dimensions.makeDimensionsString(this.dimensions)}\")")
    builder.appendLine("${indent}setAxisType(\"${this.axisType?.name}\")")
    this.attributes().forEach { it.writeDsl(builder, indent) }
    indent.decr()
    builder.appendLine("${indent}}");
}

// LOOK ProjectionCTV includes vert transforms wtf?
fun ProjectionCTV.writeDsl(builder: StringBuilder, indent: Indent) {
    builder.appendLine("${indent}transform(${this.name}) {")
    this.ctvAttributes.forEach { it.writeDsl(builder, indent.incrNew()) }
    builder.appendLine("${indent}}");
}

