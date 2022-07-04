package dev.cdm.test.util

import org.junit.jupiter.params.provider.Arguments
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.stream.Stream

fun testFilesIn(dirPath: String): TestFiles.StreamBuilder {
    return TestFiles.StreamBuilder(dirPath)
}

// list of suffixes to include
class FileFilterIncludeSuffixes(suffixes: String) : (String) -> Boolean {
    var suffixes: Array<String>

    init {
        this.suffixes = suffixes.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
    }

    override fun invoke(filename: String): Boolean {
        suffixes.forEach { suffix ->
            if (filename.endsWith(suffix)) {
                return true
            }
        }
        return false
    }
}

// list of suffixes to exclude
class FileFilterSkipSuffixes(suffixes: String) : (String) -> Boolean {
    var suffixes: Array<String>

    init {
        this.suffixes = suffixes.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
    }

    override fun invoke(filename: String): Boolean {
        suffixes.forEach { suffix ->
            if (filename.endsWith(suffix)) {
                return false
            }
        }
        return true
    }
}

class NameFilterAnd(val filters : List<(String) -> Boolean>) : (String) -> Boolean {
    override fun invoke(filename: String): Boolean {
        filters.forEach {
            if (!it.invoke(filename)) {
                return false
            }
        }
        return true
    }
}

class NameFilterOr(val filters : List<(String) -> Boolean>) : (String) -> Boolean {
    override fun invoke(filename: String): Boolean {
        filters.forEach {
            if (it.invoke(filename)) {
                return true
            }
        }
        return false
    }
}

class TestFiles {

    companion object {
        val datasetLocalDir = "../dataset/src/test/data/"
        val datasetLocalNcmlDir = "../dataset/src/test/data/ncml/"
        val coreLocalDir = "../core/src/test/data/"
        val coreNetcdf3Dir = "../core/src/test/data/netcdf3/"
        val extraTestDir = "/home/snake/tmp/testData/"
    }

    class StreamBuilder(var dirPath: String) {
        var nameFilters = mutableListOf<(String) -> Boolean>()
        var pathFilter : (Path) -> Boolean = {  true }
        var recursion = false

        fun addNameFilter(filter : (String) -> Boolean): StreamBuilder {
            this.nameFilters.add(filter)
            return this
        }

        fun withPathFilter(filter : (Path) -> Boolean): StreamBuilder {
            this.pathFilter = filter
            return this
        }

        fun withRecursion(): StreamBuilder {
            this.recursion = true
            return this
        }

        fun build() : Stream<Arguments> {
            return if (recursion) all(dirPath) else one(dirPath)
        }

        @Throws(IOException::class)
        fun one(dirName : String): Stream<Arguments> {
            return Files.list(Paths.get(dirName))
                .filter { file: Path -> !Files.isDirectory(file) }
                .filter { this.pathFilter(it) }
                .filter { NameFilterAnd(nameFilters).invoke(it.fileName.toString()) }
                .map { obj: Path -> obj.toString() }
                .map { arguments: String? -> Arguments.of(arguments) }
        }

        @Throws(IOException::class)
        fun all(dirName : String): Stream<Arguments> {
            return Stream.concat(one(dirName), subdirs(dirName))
        }

        @Throws(IOException::class)
        fun subdirs(dirName : String): Stream<Arguments> {
            return Files.list(Paths.get(dirName))
                .filter { file: Path -> Files.isDirectory(file) }
                .flatMap { obj: Path -> all(obj.toString()) }
        }
    }
}