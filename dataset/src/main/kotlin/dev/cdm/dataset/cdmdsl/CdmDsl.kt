package dev.cdm.dataset.cdmdsl

import dev.cdm.array.ArrayType

enum class Action {AddOrReplace, Modify, Remove}

open class CdmdslBase constructor(val name : String) {
    var rename: String? = null
    var action: Action = Action.Modify

    fun rename(rename : String?) {
        this.rename = rename
    }
    fun remove() {
        this.action = Action.Remove
    }
}

class CdmdslAttribute constructor(attName : String) : CdmdslBase(attName) {
    var type : ArrayType? = null
    var values : String? = null
    var dvalue : Double? = null

    constructor(attName: String, values : String, type: ArrayType? = ArrayType.STRING) : this(attName) {
        this.type = type
        this.values = values
    }

    fun setValue(attValue : Double) {
        this.dvalue = attValue
    }
    fun setValue(attValue : String) {
        this.values = attValue
    }
}

class CdmdslDimension constructor(val dimName : String, val length : Int) : CdmdslBase(dimName) {
}

class CdmdslEnum constructor(val enumName : String, val basetype: ArrayType? = null,
                             val values : MutableMap<Int, String> = mutableMapOf()) : CdmdslBase(enumName) {
    fun add(code: Int, name: String) {
        values[code] = name
    }
}

open class CdmdslVariable constructor(val varName : String) : CdmdslBase(varName) {
    val attributes = mutableMapOf<String, CdmdslAttribute>()
    var dimensions : String? = null
    var type : ArrayType? = null
    var coordSysRef : String? = null
    var values : String? = null
    var dvalues : DoubleArray? = null

    constructor(varName: String, type: ArrayType, dimensions : String) : this(varName) {
        this.type = type
        this.dimensions = dimensions
    }

    fun setValues(values : String) : CdmdslVariable {
        this.values = values
        return this
    }
    fun setValues(vararg dvalues : Double) : CdmdslVariable {
        this.dvalues = dvalues
        this.type = ArrayType.DOUBLE
        return this
    }
    fun setValues(vararg ivalues : Int) : CdmdslVariable {
        this.dvalues = DoubleArray( ivalues.size) {idx -> ivalues[idx].toDouble() }
        this.type = ArrayType.INT
        return this
    }

    fun attribute(attName : String, attValue : String, type: ArrayType? = null) : CdmdslAttribute {
        val att = CdmdslAttribute(attName, attValue, type)
        attributes[attName] = att
        return att
    }
    fun attribute(attName: String) : CdmdslAttribute {
        return attributes.getOrPut(attName) { CdmdslAttribute(attName) }
    }

    fun coordSystemRef(name: String) {
        this.coordSysRef = name
    }
}

class CdmdslStructure constructor(varName : String) : CdmdslVariable(varName) {
    val variables = mutableMapOf<String, CdmdslVariable>()
    val structures = mutableMapOf<String, CdmdslStructure>()

    // add or replace. TODO what about all references to it ??
    fun variable(varName: String, type: ArrayType, dimensions : String): CdmdslVariable {
        val v = CdmdslVariable(varName, type, dimensions)
        variables[varName] = v
        return v
    }
    fun variable(varName: String, type: ArrayType, dimensions : String,
                    lambda: CdmdslVariable.() -> Unit): CdmdslVariable {
        val v = variable(varName, type, dimensions)
        v.lambda()
        return v
    }
    fun variable(varName: String): CdmdslVariable {
        return variables.getOrPut(varName) { CdmdslVariable(varName) }
    }
    fun variable(varName: String, lambda: CdmdslVariable.() -> Unit): CdmdslVariable {
        val builder = variable(varName)
        builder.lambda()
        return builder
    }

    fun structure(varName: String, lambda: CdmdslVariable.() -> Unit): CdmdslStructure {
        val v = structure(varName)
        v.lambda()
        return v
    }
    fun structure(varName: String): CdmdslStructure {
        return structures.getOrPut(varName) { CdmdslStructure(varName) }
    }
}

class CdmdslGroup constructor(val groupName : String, val dataset : CdmdslDataset) : CdmdslBase(groupName) {
    val attributes = mutableMapOf<String, CdmdslAttribute>()
    val dimensions = mutableMapOf<String, CdmdslDimension>()
    var enumTypedefs= mutableMapOf<String, CdmdslEnum>()
    val variables = mutableMapOf<String, CdmdslVariable>()
    val structures = mutableMapOf<String, CdmdslStructure>()
    val groups = mutableMapOf<String, CdmdslGroup>()

    // add or replace. TODO what about all references to it ??
    fun dimension(dimName: String, length : Int): CdmdslDimension {
        val dim = CdmdslDimension(dimName, length)
        dim.action = Action.AddOrReplace
        dimensions[dimName] = dim
        return dim
    }
    // the intention is to add new or completely replace old
    fun enumTypedef(enumName: String, type: ArrayType, map : MutableMap<Int, String>? = null): CdmdslEnum {
        val cdmEnum = CdmdslEnum(enumName, type, map?: mutableMapOf())
        cdmEnum.action = Action.AddOrReplace
        enumTypedefs[enumName] = cdmEnum
        return cdmEnum
    }
    // the intention is to modify an existing enum
    fun modifyEnum(enumName: String, type: ArrayType, map : MutableMap<Int, String>? = null): CdmdslEnum {
        val cdmEnum = enumTypedefs.getOrPut(enumName) { CdmdslEnum(enumName, type,map?: mutableMapOf()) }
        cdmEnum.action = Action.Modify
        return cdmEnum
    }
    fun removeEnum(enumName: String) : CdmdslGroup {
        val cdmEnum = enumTypedefs.getOrPut(enumName) { CdmdslEnum(enumName) }
        cdmEnum.action = Action.Remove
        return this
    }

    // add or replace
    fun attribute(attName : String, attValue : String = "", type: ArrayType? = null) : CdmdslAttribute {
        return attributes.getOrPut(attName) { CdmdslAttribute(attName, attValue, type) }
    }
    fun attribute(attName: String) : CdmdslAttribute {
        return attributes.getOrPut(attName) { CdmdslAttribute(attName) }
    }

    // add or replace. TODO what about all references to it ??
    fun variable(varName: String, type: ArrayType, dimensions : String): CdmdslVariable {
        val v = CdmdslVariable(varName, type, dimensions)
        v.action = Action.AddOrReplace
        variables[varName] = v
        return v
    }
    fun variable(varName: String, type: ArrayType, dimensions : String,
                    lambda: CdmdslVariable.() -> Unit): CdmdslVariable {
        val v = variable(varName, type, dimensions)
        v.lambda()
        return v
    }
    fun variable(varName: String): CdmdslVariable {
        return variables.getOrPut(varName) { CdmdslVariable(varName) }
    }
    fun variable(varName: String, lambda: CdmdslVariable.() -> Unit): CdmdslVariable {
        val builder = variable(varName)
        builder.lambda()
        return builder
    }

    fun structure(varName: String, lambda: CdmdslVariable.() -> Unit): CdmdslStructure {
        val v = structure(varName)
        v.lambda()
        return v
    }
    fun structure(varName: String): CdmdslStructure {
        return structures.getOrPut(varName) { CdmdslStructure(varName) }
    }

    fun group(groupName: String): CdmdslGroup {
        return groups.getOrPut(groupName) { CdmdslGroup(groupName, this.dataset) }
    }
    fun group(groupName: String, lambda: CdmdslGroup.() -> Unit): CdmdslGroup {
        val g = group(groupName)
        g.lambda()
        return g
    }

    fun coordSystem(dimNames: String): CdmdslCoordSystem {
        return dataset.coordSystem(dimNames)
    }

    fun transform(name: String): CdmslTransform {
        return dataset.transform(name)
    }
    fun transform(name: String, lambda: CdmslTransform.() -> Unit): CdmslTransform {
        return dataset.transform(name, lambda)
    }
}

class CdmdslCoordSystem constructor(val csysName : String) : CdmdslBase(csysName){
    var projection : String? = null

    fun setProjection(projection : String) : CdmdslCoordSystem {
        this.projection = projection
        return this
    }
}
class CdmslTransform constructor(val transformName : String) : CdmdslBase(transformName){
    var varName : String? = null
    var parameters = mutableMapOf<String, CdmdslAttribute>()

    fun useVariable(varName : String) : CdmslTransform {
        this.varName = varName
        return this
    }

    fun attribute(attName : String, attValue : String, type: ArrayType? = null) : CdmdslAttribute {
        val att = CdmdslAttribute(attName, attValue, type)
        parameters[attName] = att
        return att
    }
    fun attribute(attName: String) : CdmdslAttribute {
        return parameters.getOrPut(attName) { CdmdslAttribute(attName) }
    }
}

class CdmdslDataset constructor(val location : String?, val enhance : Boolean = true) {
    var root = CdmdslGroup("", this)
    val coordSystems = mutableMapOf<String, CdmdslCoordSystem>()
    val transforms = mutableMapOf<String, CdmslTransform>()

    fun coordSystem(dimNames: String): CdmdslCoordSystem {
        return coordSystems.getOrPut(dimNames) { CdmdslCoordSystem(dimNames) }
    }

    fun transform(name: String): CdmslTransform {
        return transforms.getOrPut(name) { CdmslTransform(name) }
    }
    fun transform(name: String, lambda: CdmslTransform.() -> Unit): CdmslTransform {
        val proj = transform(name)
        proj.lambda()
        return proj
    }
}

fun cdmdsl(location : String, enhance : Boolean = true, lambda: CdmdslGroup.() -> Unit): CdmdslDataset {
    val builder = CdmdslDataset(location, enhance)
    builder.root.lambda()
    return builder
}
