package dev.ucdm.grib.inventory

import dev.ucdm.grib.common.GribCollectionIndex

abstract class AbstractMCollection(val name : String) {
    private var auxMap = mutableMapOf<String, Any>()

    fun getCollectionName() = name

    open fun getIndexFilename(): String {
        return "${getRoot()}/$name${GribCollectionIndex.NCX_SUFFIX}"
    }

    abstract fun getRoot(): String

    open fun getAuxInfo(key: String): Any? {
        return auxMap[key]
    }

    open fun setAuxInfo(key: String, value: Any) {
        auxMap[key] = value
    }

    open fun addAuxInfo(info: Map<String, Any>) {
        auxMap.putAll(info)
    }
}