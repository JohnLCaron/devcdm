package dev.ucdm.grib.collection

import dev.ucdm.grib.common.GribCollectionIndex
import dev.ucdm.grib.common.GribCollectionIndex.readCollectionFromIndex
import dev.ucdm.grib.common.GribConfig
import dev.ucdm.grib.inventory.MPartition
import java.io.File
import java.io.IOException
import java.util.*

/** create ncx4 file. The GribConfig is attached to the MCollection  */
@Throws(IOException::class)
fun createPartitionIndex(
    isGrib1: Boolean,
    dcm: MPartition,
    update: CollectionUpdateType?,
    config: GribConfig?,
    errlog: Formatter
): GribCollection? {
    dcm.setAuxInfo(GribConfig.AUX_CONFIG, config)
    val builder = GribPartitionBuilder(dcm.collectionName, File(dcm.root), dcm, isGrib1)
    if (builder.createPartitionedIndex(update, errlog)) {
        // read it back in
        return readCollectionFromIndex(dcm.getIndexFilename(GribCollectionIndex.NCX_SUFFIX), false)
    } else {
        errlog.format("failed")
        return null
    }
}