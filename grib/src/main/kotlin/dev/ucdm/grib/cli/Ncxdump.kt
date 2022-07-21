package dev.ucdm.grib.cli

import dev.ucdm.array.Indent
import dev.ucdm.core.calendar.CalendarDate
import dev.ucdm.core.io.RandomAccessFile
import dev.ucdm.grib.collection.Grib1Collection
import dev.ucdm.grib.collection.Grib2Collection
import dev.ucdm.grib.common.GribCollectionIndex
import dev.ucdm.grib.common.GribConfig
import dev.ucdm.grib.common.GribTables
import dev.ucdm.grib.grib1.iosp.Grib1Parameter
import dev.ucdm.grib.grib1.record.Grib1SectionProductDefinition
import dev.ucdm.grib.grib1.table.Grib1Customizer
import dev.ucdm.grib.grib2.record.Grib2Pds
import dev.ucdm.grib.grib2.record.Grib2SectionProductDefinition
import dev.ucdm.grib.grib2.table.Grib2Tables
import dev.ucdm.grib.protoconvert.Grib1CollectionIndexReader
import dev.ucdm.grib.protoconvert.Grib2CollectionIndexReader
import dev.ucdm.grib.protoconvert.GribCollectionIndexReader
import dev.ucdm.grib.protoconvert.Streams
import dev.ucdm.grib.protogen.GribCollectionProto
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import kotlinx.cli.required

/*
Usage: ncxdump options_list
Options:
    --input, -in -> collection index (.ncx) to dump (always required) { String }
    --showSparseArrays, -sparse [false] -> show sparse arrays
    --help, -h -> Usage info
 */
fun main(args: Array<String>) {
    val parser = ArgParser("ncxdump")
    val input by parser.option(
        ArgType.String,
        shortName = "in",
        description = "collection index (.ncx) to dump"
    ).required()
    val showSparseArrays by parser.option(
        ArgType.Boolean,
        shortName = "sparse",
        description = "show sparse arrays"
    ).default(false)
    parser.parse(args)

    print("***************\nncxdump ")
    args.forEach{it -> print("$it ")}
    println("\n")

    RandomAccessFile(input, "r").use { raf ->
        val reader = NcxDump(raf, showSparseArrays)
        reader.dumpIndex()
    }
}

class NcxDump(val raf: RandomAccessFile, val showSparseArrays : Boolean) {
    val indexReader : GribCollectionIndexReader
    val isGrib1 : Boolean
    val custom : GribTables
    val sparseArrays = mutableListOf<GribCollectionProto.Variable>()

    init {
        val name = raf.getLocation()
        val config = GribConfig()

        val collectionType = GribCollectionIndex.getType(raf)
        this.isGrib1 = collectionType == GribCollectionIndex.Type.GRIB1 || collectionType == GribCollectionIndex.Type.Partition1

        indexReader = if (isGrib1) {
            val gc = Grib1Collection(name, null, config)
            Grib1CollectionIndexReader(gc, config)
        } else {
            val gc = Grib2Collection(name, null, config)
            Grib2CollectionIndexReader(gc, config)
        }
        indexReader.readIndex(raf)
        this.custom = indexReader.makeCustomizer()
    }

    fun dumpIndex() {
        val indent = Indent(2)
        println("raf = '${raf.getLocation()}'")
        raf.order(RandomAccessFile.BIG_ENDIAN)
        raf.seek(0)

        //// header message
        val magic = raf.readString(20)
        val version = raf.readInt()

        // these are the variable records
        val sparseArraySize = raf.readLong()
        raf.skipBytes(sparseArraySize)

        val size = Streams.readVInt(raf)
        println("${indent}'${magic}' version=$version sparseArraySize=$sparseArraySize GribCollectionProtoSize=$size")

        // The GribCollectionProto
        val m = ByteArray(size)
        raf.readFully(m)
        val proto = GribCollectionProto.GribCollection.parseFrom(m)

        println("${indent}name '${proto.name}'")
        println("${indent}center=${proto.center} subcenter=${proto.subcenter} master=${proto.master} local=${proto.local}")
        println("${indent}genProcessType=${proto.genProcessType} genProcessId=${proto.genProcessId} backProcessId=${proto.backProcessId} local=${proto.local}")
        println("${indent}startTime=${CalendarDate.of(proto.startTime)} endTime=${CalendarDate.of(proto.endTime)}")
        showCoord(indent, proto.masterRuntime)
        println()

        // directory always taken from proto, since ncx2 file may be moved, or in cache, etc
        println("${indent}topdir = '${proto.topDir}'")
        indent.incr()
        proto.mfilesList.forEach { mf ->
            println("${indent}${mf.index} filename '${mf.filename}' lastModified=${CalendarDate.of(mf.lastModified)} length=${mf.length}")
        }
        indent.decr()
        println()

        // val masterRuntime = importCoord(proto.masterRuntime) as CoordinateRuntime

        proto.datasetList.forEach {
            println("${indent}dataset type ${it.type}")
            it.groupsList.forEach { g ->
                showGroup(indent.incrNew(), g)
            }
        }
        println()

        if (proto.partitionsCount > 0) {
            println("${indent}isPartitionOfPartitions=${proto.isPartitionOfPartitions}")
            println("${indent}partition ")
            proto.partitionsList.forEach {
                showPartition(indent.incrNew(), it)
            }
        }

        if (showSparseArrays) {
            println("${indent}sparseArrays")
            indent.incr()
            var totalRecords = 0
            var totalExpected = 0
            var saSize = 0L
            sparseArrays.forEach{ vp ->
                val b = ByteArray(vp.recordsLen)
                raf.seek(vp.recordsPos)
                raf.readFully(b)
                val saProto = GribCollectionProto.SparseArray.parseFrom(b)

                val pds2 = Grib2SectionProductDefinition(vp.pds.toByteArray()).pds
                val p = Parameter(vp.discipline, pds2)
                println("${indent}'${p.name}' shape=${saProto.sizeList} " +
                        "uniqueRecords=${saProto.trackCount} allRecords=${saProto.recordsCount} ndups=${saProto.ndups}" +
                        " protoSize=${vp.recordsLen}")

                totalRecords += saProto.trackCount
                totalExpected += saProto.recordsCount
                saSize += vp.recordsLen
            }
            indent.decr()
            println()
            println("${indent}totalUnique=${totalRecords} totalRecords=${totalExpected}  sparseArraySize=${saSize} avgRecordSize = ${saSize/totalRecords}")
        }
        indent.decr()
    }

    fun showGroup(indent: Indent, proto: GribCollectionProto.Group) {
        val gds = indexReader.importGribHorizCoordSystem(proto.gds)
        println("${indent}group ${gds.description} id '${gds.id}' template=${gds.hcs.template} ")
        indent.incr()

        println("${indent}coords ")
        proto.coordsList.forEach {
            showCoord(indent.incrNew(), it)
        }

        println("${indent}variables ")
        proto.variablesList.forEach {
            showVariable(indent.incrNew(), it)
            sparseArrays.add(it)
        }
        println("${indent}files=${proto.filenoList}")
        println()
        indent.decr()
    }

    fun showCoord(indent: Indent, proto: GribCollectionProto.Coord) {
        val regular = if (proto.isOrthogonal) "isOrthogonal" else if (proto.isRegular) "isRegular" else ""
        print("${indent}${proto.axisType} '${proto.unit}' code=${proto.code} $regular")
        if (proto.axisType == GribCollectionProto.GribAxisType.runtime || proto.axisType == GribCollectionProto.GribAxisType.time2D) {
            println(" ${proto.msecsList.map { CalendarDate.of(it)}}")
        } else {
            println(" ${proto.valuesList}")
        }
        proto.timesList.forEach {
            showCoord(indent.incrNew(), it)
        }
    }

    fun showVariable(indent: Indent, proto: GribCollectionProto.Variable) {
        if (isGrib1) {
            val pds1 = Grib1SectionProductDefinition(proto.pds.toByteArray())
            val p = pds1.getParameter()
            println("${indent}'${p.name}' '${p.description}' '${p.unit}' ${pds1} coordIdx=${proto.coordIdxList}")
        } else {
            val pds2 = Grib2SectionProductDefinition(proto.pds.toByteArray()).pds
            val p = Parameter(proto.discipline, pds2)
            println("${indent}'${p.name}' ${p.id} ${pds2} coordIdx=${proto.coordIdxList}")
        }
        println("${indent}  ndups=${proto.ndups} nrecords=${proto.nrecords} missing=${proto.missing}")
    }

    fun showPartition(indent: Indent, proto: GribCollectionProto.Partition) {
        println("${indent}name '${proto.name}' filename '${proto.filename}'")
    }

    fun Grib1SectionProductDefinition.getParameter() : Grib1Parameter {
        val custom1 = custom as Grib1Customizer
        return custom1.getParameter(this.center, this.subCenter, this.tableVersion, this.parameterNumber)
    }

    inner class Parameter(val discipline : Int, pds : Grib2Pds) {
        val custom2 : Grib2Tables
        val param : GribTables.Parameter?
        init {
            this.custom2 = custom as Grib2Tables
            if (custom2.getParameter(discipline, pds) == null) {
                custom2.getParameter(discipline, pds)
            }
            this.param =  custom2.getParameter(discipline, pds)
        }

        val id : String = param?.id ?: "N/A"
        val name : String =
            if (param != null) custom2.getVariableName(param.discipline, param.category, param.number)
            else "N/A"
    }

}