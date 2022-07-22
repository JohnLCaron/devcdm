package dev.ucdm.grib.inventory

import dev.ucdm.core.calendar.CalendarDate
import java.io.IOException
import java.nio.file.DirectoryStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.attribute.BasicFileAttributes

import dev.ucdm.grib.common.GribCollectionIndex.NCX_SUFFIX

// all files in a directory are a collection,
data class DirectoryMCollection(
    val topCollectionName: String,
    val collectionDir: Path,
    val isTop: Boolean,
    val glob: String?,
    val filter: DirectoryStream.Filter<Path>?,
    val olderThanMillis: Long?
) : AbstractMCollection(makeDirectoryCollectionName(topCollectionName, collectionDir)), MCollection {
    private var lastModified : Long? = null

    override fun getLastModified(): CalendarDate {
        if (lastModified == null) {
            iterateOverMFiles { } // this calculates lastModified
        }
        return CalendarDate.of(lastModified!!)
    }

    override fun getRoot(): String {
        return collectionDir.toString()
    }

    override fun getIndexFilename(): String {
        if (isTop) return super.getIndexFilename()
        val indexPath: Path = makeCollectionIndexPath(
            topCollectionName,
            collectionDir,
        )
        return indexPath.toString()
    }

    // prefer since the dirStream is always closed
    @Throws(IOException::class)
    override fun iterateOverMFiles(visitor: MCollection.Visitor) {
        val dirStream: DirectoryStream<Path> =  if (glob != null) Files.newDirectoryStream(collectionDir, glob)
        else if (filter != null) Files.newDirectoryStream(collectionDir, filter)
        else Files.newDirectoryStream(collectionDir)

        dirStream.use { ds ->
            val now = System.currentTimeMillis()
            for (p in ds) {
                val fileAttrs = Files.readAttributes(p, BasicFileAttributes::class.java)
                if (!fileAttrs.isDirectory) {
                    val last = fileAttrs.lastModifiedTime().toMillis()
                    lastModified = if (lastModified == null) last else Math.max(lastModified!!, last)
                    if (olderThanMillis == null ||  (now - last) >= olderThanMillis) {
                        visitor.visit(MFileNio(p))
                    }
                }
            }
        }
    }
}

fun makeDirectoryCollectionName(topCollectionName: String, dir: Path): String {
    val last = dir.nameCount - 1
    val lastDir = dir.getName(last)
    val lastDirName = lastDir.toString()
    return "$topCollectionName-$lastDirName"
}

fun makeCollectionIndexPath(topCollectionName: String, dir: Path): Path {
    val collectionName = makeDirectoryCollectionName(topCollectionName, dir)
    return Paths.get(dir.toString(), collectionName + NCX_SUFFIX)
}