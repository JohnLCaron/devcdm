package dev.ucdm.grib.inventory

import dev.ucdm.core.calendar.CalendarDate
import java.io.IOException
import java.nio.file.DirectoryStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.attribute.BasicFileAttributes

class DirectoryMCollection(
    val topCollectionName: String,
    val collectionDir: Path,
    val isTop: Boolean,
    val glob: String?,
    val filter: DirectoryStream.Filter<Path>?,
    val olderThanMillis: Long?
) : AbstractMCollection(makeDirectoryCollectionName(topCollectionName, collectionDir)) {
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

    override fun getIndexFilename(suffix: String): String {
        if (isTop) return super.getIndexFilename(suffix)
        val indexPath: Path = makeCollectionIndexPath(
            topCollectionName,
            collectionDir,
            suffix
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
                        visitor.visit(MFileOS7(p))
                    }
                }
            }
        }
    }

    /*
    override fun iterator(): MutableIterator<MFile> {
        return DirectoryStreamIterator()
    }

    private inner class DirectoryStreamIterator() : MutableIterator<MFile> {
        val dirStream: DirectoryStream<Path> =  if (glob != null) Files.newDirectoryStream(collectionDir, glob)
                                                else if (filter != null) Files.newDirectoryStream(collectionDir, filter)
                                                else Files.newDirectoryStream(collectionDir)
        val dirStreamIterator: Iterator<Path> = dirStream.iterator()
        var nextMFile: MFile? = null
        val now = System.currentTimeMillis()

        override fun hasNext(): Boolean {
            while (true) {
                if (!dirStreamIterator.hasNext()) {
                    nextMFile = null
                    close()
                    return false
                }
                return try {
                    val nextPath = dirStreamIterator.next()
                    val attr = Files.readAttributes(nextPath, BasicFileAttributes::class.java)
                    if (attr.isDirectory) continue
                    val last = attr.lastModifiedTime().toMillis()
                    lastModified = if (lastModified == null) last else Math.max(lastModified!!, last)
                    if (olderThanMillis == null || (now - last) >= olderThanMillis) {
                        nextMFile = MFileOS7(nextPath, attr)
                    }
                    true
                } catch (e: IOException) {
                    throw RuntimeException(e)
                }
            }
        }

        override fun next(): MFile {
            if (nextMFile == null) throw NoSuchElementException()
            return nextMFile!!
        }

        override fun remove() {
            throw NotImplementedError()
        }

        // better alternative is for caller to send in callback (Visitor pattern)
        // then we could use the try-with-resource
        fun close() {
            dirStream.close()
        }
    }

     */
}

fun makeDirectoryCollectionName(topCollectionName: String, dir: Path): String {
    val last = dir.nameCount - 1
    val lastDir = dir.getName(last)
    val lastDirName = lastDir.toString()
    return "$topCollectionName-$lastDirName"
}

fun makeCollectionIndexPath(topCollectionName: String, dir: Path, suffix: String): Path {
    val collectionName = makeDirectoryCollectionName(topCollectionName, dir)
    return Paths.get(dir.toString(), collectionName + suffix)
}