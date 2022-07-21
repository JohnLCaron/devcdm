package dev.ucdm.grib.inventory

import dev.ucdm.core.calendar.CalendarDate
import dev.ucdm.grib.collection.CollectionUpdateType
import java.io.IOException
import java.nio.file.DirectoryStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes

class DirectoryPartition(
    val topCollectionName: String,
    val collectionDir: Path,
    val isTop: Boolean,
    val glob: String?,
    val filter: DirectoryStream.Filter<Path>?,
    val olderThanMillis: Long? // LOOK what does this reference?
) : MPartition {
    private var lastModified : Long? = null

    override fun getCollectionName(): String {
        return makeDirectoryCollectionName(topCollectionName, collectionDir)
    }

    override fun getLastModified(): CalendarDate? {
        return null; // directoryMCollection.getLastModified();
    }

    override fun getRoot(): String {
        return "null"; // directoryMCollection.getRoot();
    }

    override fun getIndexFilename(suffix: String): String? {
        return "$root/$collectionName$suffix"
    }

    @Throws(IOException::class)
    override fun iterateOverMCollections(visitor: MPartition.Visitor) {
        val dirStream: DirectoryStream<Path> =  if (glob != null) Files.newDirectoryStream(collectionDir, glob)
        else if (filter != null) Files.newDirectoryStream(collectionDir, filter)
        else Files.newDirectoryStream(collectionDir)

        dirStream.use { ds ->
            val now = System.currentTimeMillis()
            for (p in ds) {
                val fileAttrs = Files.readAttributes(p, BasicFileAttributes::class.java)
                if (fileAttrs.isDirectory) {
                    val last = fileAttrs.lastModifiedTime().toMillis()
                    lastModified = if (lastModified == null) last else Math.max(lastModified!!, last)
                    if (olderThanMillis == null ||  (now - last) >= olderThanMillis) {
                        //     val topCollectionName: String,
                        //    val collectionDir: Path,
                        //    val isTop: Boolean,
                        //    val glob: String?,
                        //    val filter: DirectoryStream.Filter<Path>?,
                        //    val olderThanMillis: Long?
                        // LOOK do we want a directory collection or a file partition ??
                        val mcollect = DirectoryMCollection(topCollectionName, p, false, glob, filter, olderThanMillis)
                        visitor.visit(mcollect)
                    }
                }
            }
        }
    }

    /////////////////////////////////////////////////////////////
    // partitions can be removed (!) Why?
    private var removed = mutableListOf<String>()

    override fun removeCollection(child: MCollection) {
        removed.add(child.collectionName)
    }

    private fun wasRemoved(partition: MCollection): Boolean {
        return removed.contains(partition.collectionName)
    }
}