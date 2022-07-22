package dev.ucdm.grib.inventory

import dev.ucdm.core.calendar.CalendarDate
import java.io.IOException
import java.nio.file.DirectoryStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes

// all subdirs in a directory hold a collection
data class DirectoryPartition(
    val topCollectionName: String,
    val collectionDir: Path,
    val isTop: Boolean,
    val glob: String?,
    val filter: DirectoryStream.Filter<Path>?,
    val olderThanMillis: Long? // LOOK what does this reference?
) : AbstractMCollection(makeDirectoryCollectionName(topCollectionName, collectionDir)), MPartition {

    private var lastModified : Long? = null

    override fun getLastModified(): CalendarDate {
        if (lastModified == null) {
            iterateOverMCollections { } // this calculates lastModified
        }
        return CalendarDate.of(lastModified!!)
    }

    override fun getRoot(): String {
        return collectionDir.toAbsolutePath().toString();
    }

    override fun isPartitionOfPartition() = false

    @Throws(IOException::class)
    override fun iterateOverMCollections(visitor: MPartition.CVisitor) {
        val dirStream: DirectoryStream<Path> =  Files.newDirectoryStream(collectionDir)

        dirStream.use { ds ->
            val now = System.currentTimeMillis()
            for (p in ds) {
                val fileAttrs = Files.readAttributes(p, BasicFileAttributes::class.java)
                if (fileAttrs.isDirectory) {
                    val last = fileAttrs.lastModifiedTime().toMillis()
                    lastModified = if (lastModified == null) last else Math.max(lastModified!!, last)
                    if (olderThanMillis == null ||  (now - last) >= olderThanMillis) {
                        val mcollect = DirectoryMCollection(topCollectionName, p, false, glob, filter, olderThanMillis)
                        visitor.visit(mcollect)
                    }
                }
            }
        }
    }

    override fun iterateOverMPartitions(visitor: MPartition.PVisitor?) {
        throw RuntimeException("Not a PartitionOfPartition")
    }
}