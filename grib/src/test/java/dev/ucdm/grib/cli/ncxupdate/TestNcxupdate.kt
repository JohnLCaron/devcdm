package dev.ucdm.grib.cli.ncxupdate

import dev.ucdm.test.util.oldTestDir
import org.junit.jupiter.api.Test

class TestNcxupdate {

    @Test
    fun testGrib2Directory() {
        main(arrayOf(
            "-name", "test",
            "-topdir", oldTestDir + "gribCollections/namAlaska22",
            "-glob", "*.gbx9",
        ))
    }

    @Test
    fun testUpdateTest() {
        main(arrayOf(
            "-name", "test",
            "-topdir", oldTestDir + "gribCollections/anal",
            "-glob", "*.grib2",
            "-update", "test"
        ))
    }

    @Test
    fun testUpdateNever() {
        main(arrayOf(
            "-name", "test",
            "-topdir", oldTestDir + "gribCollections/anal",
            "-glob", "*.grib2",
            "-update", "never"
        ))
    }

    @Test
    fun testGrib2DirectoryAlways() {
        main(arrayOf(
            "-name", "test",
            "-topdir", oldTestDir + "gribCollections/anal",
            "-glob", "*.grib2",
            "-update", "always"
        ))
    }

    @Test
    fun testGrib2DirectoryNocheck() {
        main(arrayOf(
            "-name", "test",
            "-topdir", oldTestDir + "gribCollections/anal",
            "-glob", "*.grib2",
            "-update", "nocheck"
        ))
    }

    @Test
    fun testGrib1Directory() {
        main(arrayOf(
            "-name", "test",
            "-topdir", oldTestDir + "gribCollections/ecmwf/emd",
            "-glob", "*.grib",
            "-isGrib1"
        ))
    }

    @Test
    fun testDirectoryPartition() {
        main(arrayOf(
            "-name", "test",
            "-topdir", oldTestDir + "gribCollections/dgex",
            "-glob", "*.grib2",
        ))
    }

    @Test
    fun testFilePartition() {
        main(arrayOf(
            "-name", "test",
            "-topdir", oldTestDir + "gribCollections/dgex",
            "-glob", "*.grib2",
            "-collection", "file"
        ))
    }

    @Test
    fun testFilePartitionAnal() {
        main(arrayOf(
            "-name", "testUpdate",
            "-topdir", oldTestDir + "gribCollections/anal",
            "-glob", "*.grib2",
            "-collection", "file"
        ))
    }

}