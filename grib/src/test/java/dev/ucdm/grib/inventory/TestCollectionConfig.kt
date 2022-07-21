package dev.ucdm.grib.inventory

import dev.ucdm.test.util.oldTestDir
import org.junit.jupiter.api.Test
import java.nio.file.Path

class TestCollectionConfig {

    @Test
    fun testWalkDirectory() {
        val config = CollectionConfig("test", oldTestDir + "gribCollections/rdavm/ds627.0", "*.gbx9",
            { m -> processMCollection(m) },
            { p -> processMPartition(p) },
        )
        config.walkDirectory()
    }

    fun processMCollection(mcollection : MCollection ) : String {
        return "process mcollection ${mcollection.collectionName}"
    }

    fun processMPartition(mpartition : MPartition ) : String {
        return "process mpartition ${mpartition.collectionName}"
    }

}