package dev.ucdm.dataset.cli.ncdump

import dev.ucdm.array.Indent
import dev.ucdm.array.PrintArray
import dev.ucdm.core.api.Variable
import dev.ucdm.core.write.CDLWriter
import dev.ucdm.dataset.api.CdmDataset
import dev.ucdm.dataset.api.CdmDatasets
import dev.ucdm.dataset.ncml.NcmlWriter
import kotlinx.cli.*
import java.util.*
import java.util.function.Predicate

/*
Usage: ncdump options_list
Options:
    --input, -in -> input dataset to dump (always required) { String }
    --coordinates, -c [false] -> dump coordinate values
    --outputType, -type [cdl] -> output Type, strict means strict cdl { Value should be one of [cdl, ncml, strict] }
    --dumpAllData, -vall [false] -> dump data for all variables
    --dumpDataFor, -v -> dump data for listed variables [-v varName1;varName2;..] [-v varName(0:1,:,12)] { String }
    --help, -h -> Usage info
 */

enum class OutputType { cdl, ncml, strict }
enum class WantValues { none, coordsOnly, all }

fun main(args: Array<String>) {
    val parser = ArgParser("ncdump")
    val input by parser.option(
        ArgType.String,
        shortName = "in",
        description = "input dataset to dump"
    ).required()
    val coordinates by parser.option(
        ArgType.Boolean,
        shortName = "c",
        description = "dump coordinate values"
    ).default(false)
    val outputType by parser.option(
        ArgType.Choice<OutputType>(),
        shortName = "type",
        description = "type of output, strict means strict cdl"
    ).default(OutputType.cdl)
    val dumpAllData by parser.option(
        ArgType.Boolean,
        shortName = "vall",
        description = "dump data for all variables"
    ).default(false)
    val dumpDataFor by parser.option(
        ArgType.String,
        shortName = "v",
        description = "dump data for listed variables [-v varName1;varName2;..] [-v varName(0:1,:,12)]"
    ).multiple()
    val datasetLocation by parser.option(
        ArgType.String,
        shortName = "dataset",
        description = "override dataset location"
    )

    parser.parse(args)

    print("***************\nncdump ")
    args.forEach{it -> print("$it ")}
    println()

    CdmDatasets.openDataset(input).use { nc ->

        var wantValues = WantValues.none
        if (dumpAllData) {
            wantValues = WantValues.all
        }
        if (coordinates) {
            wantValues = WantValues.coordsOnly
        }

        val dump = Ncdump(nc, outputType, wantValues, dumpDataFor, datasetLocation)
        println(dump.print())
    }
}

data class Ncdump(
    val ncfile: CdmDataset, val type: OutputType, val wantValues: WantValues,
    val varNames: List<String>, val locationOverride: String?
) {

    fun print(): String {
        val headerOnly = wantValues == WantValues.none && varNames.isEmpty()
        val out = Formatter()
        val wantData = WriteVariableData(varNames)
        try {
            if (type == OutputType.ncml) {
                val ncmlWriter = NcmlWriter(null, null, wantData)
                val netcdfElement = ncmlWriter.makeNetcdfElement(ncfile, locationOverride)
                return ncmlWriter.writeToString(netcdfElement)

            } else if (headerOnly) {
                CDLWriter.writeCDL(ncfile, out, type == OutputType.strict, locationOverride)

            } else {
                val indent = Indent(2)
                val cdlWriter = CDLWriter(ncfile, out, type == OutputType.strict)
                cdlWriter.toStringStart(indent, type == OutputType.strict, locationOverride)
                indent.incr()
                out.format("%n%sdata:%n", indent)
                indent.incr()

                ncfile.getVariables().filter { it -> wantData.test(it) }.forEach { v ->
                    out.format("%s%s = ", indent, v.fullName)
                    wantData.printArray(out, v, indent)
                    out.format("%n")
                }
                indent.decr()
                indent.decr()
                cdlWriter.toStringEnd()
            }
        } catch (e: Exception) {
            out.format("%n%s%n", e.message)
        }
        return out.toString()
    }

    inner class WriteVariableData(variableNames: Iterable<String>) : Predicate<Variable> {
        private val variableSections: MutableMap<String, String> = mutableMapOf()

        init {
            variableNames.forEach { varName ->
                val stoke = StringTokenizer(varName, ";")
                while (stoke.hasMoreTokens()) {
                    val varAndSection = stoke.nextToken() // variable name and optionally a section
                    val pos = varAndSection.indexOf('(')
                    if (pos > 0) {
                        val name = varAndSection.substring(0, pos)
                        variableSections[name] = varAndSection
                    } else {
                        variableSections[varAndSection] = varAndSection
                    }
                }
            }
        }

        override fun test(t: Variable): Boolean {
            if (wantValues == WantValues.all) return true
            if (wantValues == WantValues.coordsOnly && t.isCoordinateVariable) return true
            if (variableSections.contains(t.fullName)) return true
            return false
        }

        fun printArray(out: Formatter, v: Variable, indent: Indent) {
            val section = variableSections[v.fullName]
            if (section != null) {
                val data: dev.ucdm.array.Array<*> = ncfile.readSectionArray(section)
                PrintArray.printArray(out, data, indent)
            } else {
                PrintArray.printArray(out, v.readArray(), indent)
            }
        }
    }
}