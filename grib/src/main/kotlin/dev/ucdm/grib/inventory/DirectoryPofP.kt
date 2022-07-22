package dev.ucdm.grib.inventory

import dev.ucdm.core.calendar.CalendarDate
import java.io.IOException
import java.nio.file.DirectoryStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes

// all subdirs in a directory hold a partition
data class DirectoryPofP(
    val topCollectionName: String,
    val collectionDir: Path,
    val isTop: Boolean,
    val glob: String?,
    val filter: DirectoryStream.Filter<Path>?,
    val olderThanMillis: Long? // LOOK what does this reference?
) : AbstractMCollection(makeDirectoryCollectionName(topCollectionName, collectionDir)), MPartition  {

    private var lastModified : Long? = null

    override fun getLastModified(): CalendarDate {
        if (lastModified == null) {
            iterateOverMPartitions { } // this calculates lastModified
        }
        return CalendarDate.of(lastModified!!)
    }

    override fun getRoot(): String {
        return collectionDir.toAbsolutePath().toString()
    }

    override fun isPartitionOfPartition() = true

    override fun iterateOverMCollections(visitor: MPartition.CVisitor?) {
        throw RuntimeException("Is a PartitionOfPartition")
    }

    @Throws(IOException::class)
    override fun iterateOverMPartitions(visitor: MPartition.PVisitor) {
        val dirStream: DirectoryStream<Path> =  Files.newDirectoryStream(collectionDir)

        dirStream.use { ds ->
            val now = System.currentTimeMillis()
            for (p in ds) {
                val fileAttrs = Files.readAttributes(p, BasicFileAttributes::class.java)
                if (fileAttrs.isDirectory) {
                    val last = fileAttrs.lastModifiedTime().toMillis()
                    lastModified = if (lastModified == null) last else Math.max(lastModified!!, last)
                    if (olderThanMillis == null ||  (now - last) >= olderThanMillis) {
                        val mcollect = DirectoryPartition(topCollectionName, p, false, glob, filter, olderThanMillis)
                        visitor.visit(mcollect)
                    }
                }
            }
        }
    }
}