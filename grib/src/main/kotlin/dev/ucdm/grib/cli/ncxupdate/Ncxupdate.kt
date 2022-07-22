package dev.ucdm.grib.cli.ncxupdate

import dev.ucdm.dataset.cli.ncdump.OutputType
import dev.ucdm.grib.collection.createPartitionIndex
import dev.ucdm.grib.collection.updatePartitionIndex
import dev.ucdm.grib.common.GribCollectionIndex
import dev.ucdm.grib.common.GribConfig
import dev.ucdm.grib.inventory.*

import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import kotlinx.cli.required
import java.util.*

/*
Update directory trees of GRIB collection files.

Usage: ncxupdate options_list
Options:
    --name, -name -> collection name (always required) { String }
    --topdir, -topdir -> top directory (absolute path) (always required) { String }
    --glob, -glob -> data file glob (eg '*.grib2') (always required) { String }
    --isGrib1, -isGrib1 [false] -> is Grib1 collection
    --fileCollection, -collection [directory] -> file: each file is a collection; directory: all files in a directory make a collection { Value should be one of [file, directory] }
    --updateStrategy, -update [test] -> update strategy { Value should be one of [always, test, nocheck, never] }
    --help, -h -> Usage info
 */
fun main(args: Array<String>) {
    val parser = ArgParser("ncxupdate")
    val name by parser.option(
        ArgType.String,
        shortName = "name",
        description = "collection name"
    ).required()
    val topdir by parser.option(
        ArgType.String,
        shortName = "topdir",
        description = "top directory (absolute path)"
    ).required()
    val glob by parser.option(
        ArgType.String,
        shortName = "glob",
        description = "data file glob (eg '*.grib2')"
    ).required()
    val isGrib1 by parser.option(
        ArgType.Boolean,
        shortName = "isGrib1",
        description = "is Grib1 collection"
    ).default(false)
    val fileCollection by parser.option(
        ArgType.Choice<PartitionType>(),
        shortName = "collection",
        description = "file: each file is a collection; directory: all files in a directory make a collection"
    ).default(PartitionType.directory)
    val updateStrategy by parser.option(
        ArgType.Choice<CollectionUpdate>(),
        shortName = "update",
        description = "update strategy"
    ).default(CollectionUpdate.test)

    parser.parse(args)

    print("***************\nncxupdate ")
    args.forEach{it -> print("$it ")}
    println("\n")

    val reader = NcxUpdate(name, topdir, glob, isGrib1, fileCollection, updateStrategy)
    reader.update()
}

class NcxUpdate(val name: String, val topdir: String, val glob: String, val isGrib1 : Boolean,
                val partition : PartitionType, val collectionUpdate : CollectionUpdate) {

    fun update() {
        val info = mapOf("isGrib1" to isGrib1, "GribConfig" to GribConfig(), "CollectionUpdate" to collectionUpdate)
        val config = CollectionConfig(
            name,
            topdir,
            glob,
            info,
            { m -> processMCollection(m) },
            { p -> processMPartition(p) },
            null,
            partition,
        )
        config.walkDirectory()
        println()
    }

    fun processMCollection(mcollection: MCollection): String {
        //println("$mcollection")
        val errlog = Formatter()
        val isGrib1 = mcollection.getAuxInfo("isGrib1") as Boolean
        val gribConfig = mcollection.getAuxInfo("GribConfig") as GribConfig
        val update = mcollection.getAuxInfo("CollectionUpdate") as CollectionUpdate
        val gc = GribCollectionIndex.updateCollectionIndex(isGrib1, mcollection, update, gribConfig, errlog)
        if (gc == null) {
            println("errlog = '$errlog'")
            return "Failed to create mcollection ${mcollection.indexFilename}"
        } else {
            gc?.close();
            return "Updated ${mcollection.indexFilename}"
        }
    }

    fun processMPartition(mpartition: MPartition): String {
        // println("$mpartition")
        val errlog = Formatter()
        val isGrib1 = mpartition.getAuxInfo("isGrib1") as Boolean
        val gribConfig = mpartition.getAuxInfo("GribConfig") as GribConfig
        val update = mpartition.getAuxInfo("CollectionUpdate") as CollectionUpdate

        val gc = updatePartitionIndex(isGrib1, mpartition, update, gribConfig, errlog)
        if (gc == null) {
            println("errlog = '$errlog'")
            return "Failed to create mpartition ${mpartition.indexFilename}"
        } else {
            gc.close()
            return "Created ${mpartition.indexFilename}"
        }
    }

}