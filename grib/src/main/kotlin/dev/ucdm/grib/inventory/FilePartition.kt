package dev.ucdm.grib.inventory

import dev.ucdm.core.calendar.CalendarDate
import java.nio.file.DirectoryStream
import java.nio.file.Path

// all file collections in a directory are a partition, dirName/collectionName-dirName.ncx
data class FilePartition(
    val topCollectionName: String,
    val collectionDir: Path,
    val isTop: Boolean,
    val glob: String?,
    val filter: DirectoryStream.Filter<Path>?,
    val olderThanMillis: Long?
) : AbstractMCollection(makeDirectoryCollectionName(topCollectionName, collectionDir)), MPartition {

    private val directoryMCollection = DirectoryMCollection(topCollectionName,
        collectionDir,
        isTop,
        glob,
        filter,
        olderThanMillis)

    override fun getLastModified(): CalendarDate {
        return directoryMCollection.lastModified
    }

    override fun getRoot(): String {
        return directoryMCollection.root
    }

    override fun isPartitionOfPartition() = false

    override fun iterateOverMCollections(visitor: MPartition.CVisitor) {
        return directoryMCollection.iterateOverMFiles {
            val mcollect: MCollection = SingleFileMCollection(it)
            visitor.visit(mcollect)
        }
    }

    override fun iterateOverMPartitions(visitor: MPartition.PVisitor?) {
        throw RuntimeException("Not a PartitionOfPartition")
    }
}