package dev.ucdm.dataset.cdmdsl

import dev.ucdm.array.Indent
import dev.ucdm.array.PrintArray.printArray
import dev.ucdm.array.PrintArray.printArrayPlain
import dev.ucdm.core.api.*
import dev.ucdm.dataset.api.*
import java.util.*

fun CdmDataset.writeDsl(): String {
    val builder = StringBuilder()

    builder.appendLine("cdmdsl(\"${this.location}\") {")
    val indent = Indent(2)

    if (!this.rootGroup.attributes().isEmpty()) {
        this.rootGroup.attributes().forEach { it.writeDsl(builder, indent.incrNew()) }
        builder.appendLine()
    }

    if (!this.rootGroup.dimensions.isEmpty()) {
        this.rootGroup.dimensions.forEach { it.writeDsl(builder, indent.incrNew()) }
        builder.appendLine()
    }

    val datasetCs: CdmDatasetCS? = if (this is CdmDatasetCS) this else null

    if (!this.rootGroup.variables.isEmpty()) {
        this.rootGroup.variables.filter { it !is CoordinateAxis }
            .forEach { it.writeDsl(builder, indent.incrNew(), datasetCs) }
        builder.appendLine()
    }

    if (this is CdmDatasetCS) {
        this.coordinateAxes.forEach { it.writeDsl(builder, indent.incrNew()) }
        this.coordinateSystems.forEach { it.writeDsl(builder, indent.incrNew()) }
        this.coordinateTransforms.forEach { it.writeDsl(builder, indent.incrNew()) }
        builder.appendLine()
    }

    if (!this.rootGroup.groups.isEmpty()) {
        this.rootGroup.groups.forEach { it.writeDsl(builder, indent.incrNew(), datasetCs) }
        builder.appendLine()
    }

    builder.appendLine("${indent.decr()}}");
    return builder.toString()
}

fun CdmDatasetCS.writeCSDsl(): String {
    val builder = StringBuilder()

    builder.appendLine("cdmdsl(\"${this.location}\") {")
    val indent = Indent(2)
    this.coordinateAxes.forEach { it.writeDsl(builder, indent.incrNew()) }
    this.coordinateSystems.forEach { it.writeDsl(builder, indent.incrNew()) }
    this.coordinateTransforms.forEach { it.writeDsl(builder, indent.incrNew()) }
    builder.appendLine()
    builder.appendLine("${indent.decr()}}");
    return builder.toString()
}

fun Attribute.writeDsl(builder: StringBuilder, indent: Indent) {
    val values = this.arrayValues?.let { shortenPrintArray(it, 80) }
    builder.appendLine("${indent}attribute(\"${this.name.trim()}\").setValue(${values})");
}

fun <T> shortenPrintArray(arrayValues: dev.ucdm.array.Array<T>, len: Int): String {
    var values = printArrayPlain(arrayValues)
    if (values.length > len) values = values.take(len) + "...\""
    return values
}

fun <T> shortenPrintArrayOld(arrayValues: dev.ucdm.array.Array<T>, len: Int): String {
    val out = Formatter()
    printArray(out, arrayValues, Indent(0))
    var values = out.toString().replace("\n", "")
    if (values.length > len) values = values.take(len) + "...\""
    return values
}

fun Dimension.writeDsl(builder: StringBuilder, indent: Indent) {
    builder.appendLine("${indent}dimension(\"${this.shortName}\", ${this.getLength()})")
}

fun Variable.writeDsl(builder: StringBuilder, indent: Indent, dataset: CdmDatasetCS? = null) {
    builder.appendLine("${indent}variable(\"${this.getShortName()}\") {")
    indent.incr()
    builder.appendLine("${indent}setType(\"${this.arrayType.name}\")")
    builder.appendLine("${indent}setDimensions(\"${Dimensions.makeDimensionsString(this.dimensions)}\")")
    this.attributes().forEach { it.writeDsl(builder, indent) }
    if (dataset != null && this is VariableDS) {
        dataset.makeCoordinateSystemsFor(this).forEach {
            builder.appendLine("${indent}coordSystem(\"${it.name}\")")
        }
    }
    indent.decr()
    builder.appendLine("${indent}}");
}

fun Group.writeDsl(builder: StringBuilder, indent: Indent, dataset: CdmDatasetCS? = null) {
    builder.appendLine("${indent}group(\"${this.getShortName()}\") {")
    indent.incr()

    if (!this.attributes().isEmpty()) {
        this.attributes().forEach { it.writeDsl(builder, indent.incrNew()) }
        builder.appendLine()
    }

    if (!this.dimensions.isEmpty()) {
        this.dimensions.forEach { it.writeDsl(builder, indent.incrNew()) }
        builder.appendLine()
    }

    if (!this.variables.isEmpty()) {
        this.variables.filter { it !is CoordinateAxis }.forEach { it.writeDsl(builder, indent.incrNew(), dataset) }
        builder.appendLine()
    }

    if (!this.groups.isEmpty()) {
        this.groups.forEach { it.writeDsl(builder, indent.incrNew()) }
        builder.appendLine()
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

fun CoordinateSystem.writeDsl(builder: StringBuilder, indent: Indent) {
    builder.appendLine("${indent}coordSystem(\"${this.name}\") {")
    indent.incr()
    builder.appendLine("${indent}setAxes(\"${this.axesName}\")")
    builder.appendLine("${indent}setTransforms(\"${this.coordinateTransforms.map { it.name }}\")")
    if (this.projection != null) {
        builder.appendLine("${indent}setProjection(\"${this.projection!!.name}\")")
    }
    indent.decr()
    builder.appendLine("${indent}}");
}

fun CoordinateTransform.writeDsl(builder: StringBuilder, indent: Indent) {
    builder.appendLine("${indent}transform(\"${this.name}\") {")
    this.metadata.forEach { it.writeDsl(builder, indent.incrNew()) }
    builder.appendLine("${indent}}");
}

