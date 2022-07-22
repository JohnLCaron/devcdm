package dev.ucdm.grib.collection

import dev.ucdm.core.calendar.CalendarDate
import dev.ucdm.grib.common.GribCollectionIndex
import dev.ucdm.grib.common.GribCollectionIndex.readCollectionFromIndex
import dev.ucdm.grib.common.GribConfig
import dev.ucdm.grib.common.util.GribIndexCache
import dev.ucdm.grib.inventory.CollectionUpdate
import dev.ucdm.grib.inventory.MPartition
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.util.*

private val logger = LoggerFactory.getLogger(GribCollectionIndex::class.java)

/** create ncx4 file. The GribConfig is attached to the MCollection  */
@Throws(IOException::class)
fun createPartitionIndex(
    isGrib1: Boolean, dcm: MPartition, config: GribConfig, errlog: Formatter
): GribCollection? {
    val builder = GribPartitionBuilder(dcm.collectionName, File(dcm.root), dcm, config, isGrib1)

    if (builder.createPartitionedIndex(errlog)) {
        // read it back in
        return readCollectionFromIndex(dcm.indexFilename, false, errlog)
    } else {
        errlog.format("failed")
        return null
    }
}

/**
 * The general case of a creating a collection from Grib data or gbx files.
 * If the corresponding ncx4 file exists, use it, update it, or create it.
 * Note we are only checking the top level index.
 */
@Throws(IOException::class)
fun updatePartitionIndex(
    isGrib1: Boolean, dcm: MPartition, update: CollectionUpdate, config: GribConfig, errlog: Formatter
): GribCollection? {
    val idxPath = dcm.indexFilename
    // look to see if the file is in some special cache (eg when cant write to data directory)
    val idxFile = GribIndexCache.getExistingFileOrCache(idxPath)
    val idxFileExists = idxFile != null
    var gribCollection: GribCollection? = null
    if (idxFileExists && update != CollectionUpdate.always) { // always create a new index
        // look to see if the index file is older than the collection
        val indexIsOlder = CalendarDate.of(idxFile!!.lastModified()).isBefore(dcm.lastModified)
        // dont read it if index is older
        if ((update == CollectionUpdate.nocheck) || (update == CollectionUpdate.never) || !indexIsOlder) {
            gribCollection = readCollectionFromIndex(dcm.indexFilename, false, errlog)
        }
    }

    if (gribCollection == null) {
        if (update == CollectionUpdate.never) {
            errlog.format("Failed to open existing index for '%s'", idxPath)
            return null
        }
        // may not exist, overwrite if it does.
        val idxFile2 = GribIndexCache.getFileOrCache(idxPath)
        if (idxFile2 == null) {
            errlog.format("Failed to find a place to write the index file for '%s'", idxPath)
            return null
        }
        val gc = createPartitionIndex(isGrib1, dcm, config, errlog)
        if (gc == null) {
            logger.warn("  Index writing failed on {} errlog = '{}'", idxFile2, errlog)
        } else {
            // read it back in
            gribCollection = readCollectionFromIndex(dcm.indexFilename, false, errlog)
            logger.debug("  Index written: {}", idxPath)
        }
    } else {
        logger.debug("  Index read: {}", idxPath)
    }

    return gribCollection
}