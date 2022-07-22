package dev.ucdm.grib.inventory

import dev.ucdm.array.Indent
import java.nio.file.DirectoryStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import java.util.concurrent.atomic.AtomicInteger

/*
3)<collection spec="/data/ldm/pub/native/grid/NCEP/NAM/Alaska_11km/*.grib2"
4)            name="NAM_Alaska_11km"
5)            partition="file"
6)            olderThan="5 min"/>

3) The collection consists of all files ending with ".grib2". The top directory is "/data/ldm/pub/native/grid/NCEP/NAM/Alaska_11km/"
4) The collection name is "NAM_Alaska_11km"
5) Partitioning will happen at the file level. Default is the directory level.

6) Only include files whose lastModified date is more than 5 minutes old. This is to exclude files that are actively being created.
 */

 */

enum class PartitionType { file, directory }

data class CollectionConfig(
    val name: String,
    val topDir: String,
    val glob: String,
    val info: Map<String, Any>,
    val makeCollection : (MCollection) -> String,
    val makePartition : (MPartition) -> String,
    val olderThan: Int? = null,
    val partition: PartitionType = PartitionType.directory,
) {

    fun walkDirectory() {
        walkDirectory(Path.of(topDir), Indent(2))
    }

    fun walkDirectory(dirPath: Path, indent : Indent) : Int {
        println("$indent$dirPath")
        val dirStream: DirectoryStream<Path> = Files.newDirectoryStream(dirPath)
        val countDirs = AtomicInteger(0)
        val leafFiles = AtomicInteger(0)

        dirStream.use { ds ->
            for (p in ds) {
                val fileAttrs = Files.readAttributes(p, BasicFileAttributes::class.java)
                if (fileAttrs.isDirectory) {
                    countDirs.incrementAndGet();
                    leafFiles.addAndGet(walkDirectory(p, indent.incrNew()))
                }
            }
        }

        val dataFiles = processLeaf(dirPath, indent.incrNew())
        leafFiles.addAndGet(dataFiles)

        // if theres only one subdir, i dont think we need to build another index ?? ??
        if (countDirs.get() > 1) {
            // by checking there are leaf files, we eliminate spurious directories
            if (leafFiles.get() > 0) {
                if (partition == PartitionType.directory) {
                    val dirPartition = DirectoryPartition(name, dirPath, false, glob, null, null)
                    dirPartition.addAuxInfo(info)
                    val status = makePartition(dirPartition)
                    println("${indent}$status DirectoryPartition with ${countDirs.get()} directories, ${leafFiles.get()} leafFiles")
                } else if (partition == PartitionType.file) {
                    val dirPartition = DirectoryPofP(name, dirPath, false, glob, null, null)
                    dirPartition.addAuxInfo(info)
                    val status = makePartition(dirPartition)
                    println("${indent}$status DirectoryPofP with ${countDirs.get()} directories, ${leafFiles.get()} leafFiles")

                }
            } // else PofP
        }

        return leafFiles.get()
    }

    fun processLeaf(dirPath: Path, indent : Indent) : Int {
        val dirStream: DirectoryStream<Path> = Files.newDirectoryStream(dirPath, glob)
        val count = AtomicInteger(0)
        dirStream.use { ds ->
            for (p in ds) {
                val fileAttrs = Files.readAttributes(p, BasicFileAttributes::class.java)
                if (!fileAttrs.isDirectory) {
                    // println("$indent${p.fileName}")
                    count.incrementAndGet();
                    if (partition == PartitionType.file) {
                        val singleFile = SingleFileMCollection(MFileNio(p))
                        singleFile.addAuxInfo(info)
                        val status = makeCollection(singleFile)
                        println("${indent}$status SingleFileMCollection")
                    }
                }
            }
        }

        // a leaf directory has data files
        if (count.get() > 0) {
            if (partition == PartitionType.directory) {
                // look what about testing if the collection needs updating??
                val dirCollect = DirectoryMCollection(name, dirPath, false, glob, null, null)
                dirCollect.addAuxInfo(info)
                val status = makeCollection(dirCollect)
                println("${indent}$status DirectoryMCollection with ${count.get()} files")

            } else if (partition == PartitionType.file) {
                val filePartition = FilePartition(name, dirPath, false, glob, null, null)
                filePartition.addAuxInfo(info)
                val status = makePartition(filePartition)
                println("${indent}$status FilePartition with ${count.get()} files")
            }
        }
        return count.get()
    }

}


