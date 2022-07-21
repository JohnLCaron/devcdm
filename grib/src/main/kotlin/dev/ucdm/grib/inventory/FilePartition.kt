package dev.ucdm.grib.inventory

import dev.ucdm.core.calendar.CalendarDate
import java.nio.file.DirectoryStream
import java.nio.file.Path

// LOOK maybe it shouldnt be an MCollection? eg what use is iterateOverMFiles ?
class FilePartition(
    topCollectionName: String,
    collectionDir: Path,
    val isTop: Boolean,
    glob: String?,
    val filter: DirectoryStream.Filter<Path>?,
    olderThanMillis: Long?
) : AbstractMCollection(makeDirectoryCollectionName(topCollectionName, collectionDir)), MPartition {

    val directoryMCollection = DirectoryMCollection(topCollectionName,
        collectionDir,
        isTop,
        glob,
        filter,
        olderThanMillis)

    override fun getLastModified(): CalendarDate? {
        return directoryMCollection.getLastModified()
    }

    override fun getRoot(): String {
        return directoryMCollection.getRoot()
    }

    override fun iterateOverMFiles(visitor: MCollection.Visitor) {
        return directoryMCollection.iterateOverMFiles(visitor)
    }

    override fun iterateOverMCollections(visitor: MPartition.Visitor) {
        return directoryMCollection.iterateOverMFiles {
            val mcollect: MCollection = SingleFileMCollection(it)
            if (!wasRemoved(mcollect)) {
                visitor.visit(mcollect)
            }
        }
    }

    /////////////////////////////////////////////////////////////
    // partitions can be removed (!)
    private var removed = mutableListOf<String>()

    override fun removeCollection(child: MCollection) {
        removed.add(child.collectionName)
    }

    private fun wasRemoved(partition: MCollection): Boolean {
        return removed.contains(partition.collectionName)
    }
}