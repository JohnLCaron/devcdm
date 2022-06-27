package dev.cdm.dataset.cdmdsl

import dev.cdm.array.ArrayType

class Cdml constructor(val location : String?) {
    var root = CdmlGroup("")

    // add or replace. TODO what about all references to it ??
    fun addDimension(dimName: String, length : Int): CdmlDimension {
        return root.addDimension(dimName, length)
    }
    fun addEnum(enumName: String, type: ArrayType, map : MutableMap<Int, String>?): CdmlEnum {
        return root.addEnum(enumName, type, map)
    }

    fun addAttribute(attName : String, attValue : String, type: ArrayType? = null) : CdmlAttribute {
        return root.addAttribute(attName, attValue, type)
    }
    fun removeAttribute(attName: String) : CdmlGroup {
        return root.removeAttribute(attName)
    }

    // add or replace.
    fun addVariable(varName: String, type: ArrayType, dimensions : String): CdmlVariable {
        return root.addVariable(varName, type, dimensions)
    }
    fun addVariable(varName: String, type: ArrayType, dimensions : String,
                    lambda: CdmlVariable.() -> Unit): CdmlVariable {
        return root.addVariable(varName, type, dimensions, lambda)
    }
    fun getVariable(varName: String): CdmlVariable {
        return root.getVariable(varName)
    }
    fun getVariable(varName: String, lambda: CdmlVariable.() -> Unit): CdmlVariable {
        return root.getVariable(varName, lambda)
    }
}

enum class Action2 {AddOrReplace, Modify, Remove}

class CdmlAttribute constructor(val attName : String) {
    var rename : String? = null
    var type : ArrayType? = null
    var values : String? = null
    var action : Action = Action.Modify

    constructor(attName: String, values : String, type: ArrayType? = ArrayType.STRING) : this(attName) {
        this.type = type
        this.values = values
    }

    fun setValue(attValue : String?) {
        this.values = attValue
    }
    fun rename(rename : String?) : CdmlAttribute {
        this.rename = rename
        return this
    }
}

class CdmlDimension constructor(val dimName : String, val length : Int) {
    var action : Action = Action.Modify
}

class CdmlEnum constructor(val enumName : String, val basetype: ArrayType? = null, val values : MutableMap<Int, String> = mutableMapOf()) {
    var action : Action = Action.Modify

    fun add(code: Int, name: String) {
        values[code] = name
    }
}

open class CdmlVariable constructor(val varName : String) {
    val attributes = mutableMapOf<String, CdmlAttribute>()
    var dimensions : String? = null
    var rename : String? = null
    var type : ArrayType? = null
    var values : String? = null
    var dvalues : DoubleArray? = null
    var action : Action = Action.Modify

    constructor(varName: String, type: ArrayType, dimensions : String) : this(varName) {
        this.type = type
        this.dimensions = dimensions
    }

    fun rename(rename : String?) : CdmlVariable {
        this.rename = rename
        return this
    }
    fun setValues(values : String) : CdmlVariable {
        this.values = values
        return this
    }
    fun setValues(vararg dvalues : Double) : CdmlVariable {
        this.dvalues = dvalues
        this.type = ArrayType.DOUBLE
        return this
    }
    fun setValues(vararg ivalues : Int) : CdmlVariable {
        this.dvalues = DoubleArray( ivalues.size) {idx -> ivalues[idx].toDouble() }
        this.type = ArrayType.INT
        return this
    }

    // add or replace
    fun addAttribute(attName : String, attValue : String, type: ArrayType? = null) : CdmlAttribute {
        val att = CdmlAttribute(attName, attValue, type)
        attributes[attName] = att
        return att
    }
    fun getAttribute(attName: String) : CdmlAttribute {
        return attributes.getOrPut(attName) { CdmlAttribute(attName) }
    }
}

class CdmlStructure constructor(varName : String) : CdmlVariable(varName) {
    val variables = mutableMapOf<String, CdmlVariable>()
    val structures = mutableMapOf<String, CdmlStructure>()

    // add or replace. TODO what about all references to it ??
    fun addVariable(varName: String, type: ArrayType, dimensions : String): CdmlVariable {
        val v = CdmlVariable(varName, type, dimensions)
        variables[varName] = v
        return v
    }
    fun addVariable(varName: String, type: ArrayType, dimensions : String,
                    lambda: CdmlVariable.() -> Unit): CdmlVariable {
        val v = addVariable(varName, type, dimensions)
        v.lambda()
        return v
    }
    fun getVariable(varName: String): CdmlVariable {
        return variables.getOrPut(varName) { CdmlVariable(varName) }
    }
    fun getVariable(varName: String, lambda: CdmlVariable.() -> Unit): CdmlVariable {
        val builder = getVariable(varName)
        builder.lambda()
        return builder
    }

    // add or replace.
    fun addStructure(varName: String): CdmlStructure {
        val v = CdmlStructure(varName)
        structures[varName] = v
        return v
    }
    fun addStructure(varName: String, lambda: CdmlVariable.() -> Unit): CdmlStructure {
        val v = addStructure(varName)
        v.lambda()
        return v
    }
    fun getStructure(varName: String): CdmlStructure {
        return structures.getOrPut(varName) { CdmlStructure(varName) }
    }
    fun getStructure(varName: String, lambda: CdmlVariable.() -> Unit): CdmlStructure {
        val builder = getStructure(varName)
        builder.lambda()
        return builder
    }
}

class CdmlGroup constructor(val groupName : String) {
    val attributes = mutableMapOf<String, CdmlAttribute>()
    val dimensions = mutableMapOf<String, CdmlDimension>()
    var enumTypedefs= mutableMapOf<String, CdmlEnum>()
    val variables = mutableMapOf<String, CdmlVariable>()
    val structures = mutableMapOf<String, CdmlStructure>()
    val groups = mutableMapOf<String, CdmlGroup>()
    var rename : String? = null
    var action : Action = Action.Modify

    fun rename(rename : String?) : CdmlGroup {
        this.rename = rename
        return this
    }

    // add or replace. TODO what about all references to it ??
    fun addDimension(dimName: String, length : Int): CdmlDimension {
        val dim = CdmlDimension(dimName, length)
        dim.action = Action.AddOrReplace
        dimensions[dimName] = dim
        return dim
    }
    // the intention is to add new or completely replace old
    fun addEnum(enumName: String, type: ArrayType, map : MutableMap<Int, String>? = null): CdmlEnum {
        val cdmEnum = CdmlEnum(enumName, type, map?: mutableMapOf())
        cdmEnum.action = Action.AddOrReplace
        enumTypedefs[enumName] = cdmEnum
        return cdmEnum
    }
    // the intention is to modify an existing enum
    fun modifyEnum(enumName: String, type: ArrayType, map : MutableMap<Int, String>? = null): CdmlEnum {
        val cdmEnum = enumTypedefs.getOrPut(enumName) { CdmlEnum(enumName, type,map?: mutableMapOf()) }
        cdmEnum.action = Action.Modify
        return cdmEnum
    }
    fun removeEnum(enumName: String) : CdmlGroup {
        val cdmEnum = enumTypedefs.getOrPut(enumName) { CdmlEnum(enumName) }
        cdmEnum.action = Action.Remove
        return this
    }

    // add or replace
    fun addAttribute(attName : String, attValue : String, type: ArrayType? = null) : CdmlAttribute {
        val att = CdmlAttribute(attName, attValue, type)
        att.action = Action.AddOrReplace
        attributes[attName] = att
        return att
    }
    fun getAttribute(attName: String) : CdmlAttribute {
        return attributes.getOrPut(attName) { CdmlAttribute(attName) }
    }
    fun removeAttribute(attName: String) : CdmlGroup {
        attributes.getOrPut(attName) { CdmlAttribute(attName) }.action = Action.Remove
        return this
    }

    // add or replace. TODO what about all references to it ??
    fun addVariable(varName: String, type: ArrayType, dimensions : String): CdmlVariable {
        val v = CdmlVariable(varName, type, dimensions)
        v.action = Action.AddOrReplace
        variables[varName] = v
        return v
    }
    fun addVariable(varName: String, type: ArrayType, dimensions : String,
                    lambda: CdmlVariable.() -> Unit): CdmlVariable {
        val v = addVariable(varName, type, dimensions)
        v.lambda()
        return v
    }
    fun getVariable(varName: String): CdmlVariable {
        return variables.getOrPut(varName) { CdmlVariable(varName) }
    }
    fun getVariable(varName: String, lambda: CdmlVariable.() -> Unit): CdmlVariable {
        val builder = getVariable(varName)
        builder.lambda()
        return builder
    }

    // add or replace.
    fun addStructure(varName: String): CdmlStructure {
        val v = CdmlStructure(varName)
        v.action = Action.AddOrReplace
        structures[varName] = v
        return v
    }
    fun addStructure(varName: String, lambda: CdmlVariable.() -> Unit): CdmlStructure {
        val v = addStructure(varName)
        v.lambda()
        return v
    }
    fun getStructure(varName: String): CdmlStructure {
        return structures.getOrPut(varName) { CdmlStructure(varName) }
    }
    fun getStructure(varName: String, lambda: CdmlVariable.() -> Unit): CdmlStructure {
        val builder = getStructure(varName)
        builder.lambda()
        return builder
    }

    // add or replace.
    fun addGroup(groupName: String): CdmlGroup {
        val g = CdmlGroup(groupName)
        groups[groupName] = g
        return g
    }
    fun addGroup(groupName: String, lambda: CdmlGroup.() -> Unit): CdmlGroup {
        val g = addGroup(groupName)
        g.lambda()
        return g
    }
    fun getGroup(groupName: String): CdmlGroup {
        return groups.getOrPut(groupName) { CdmlGroup(groupName) }
    }
    fun getGroup(groupName: String, lambda: CdmlGroup.() -> Unit): CdmlGroup {
        val g = getGroup(groupName)
        g.lambda()
        return g
    }
}

fun cdml(location : String, lambda: Cdml.() -> Unit): Cdml {
    val builder = Cdml(location)
    builder.lambda()
    return builder
}
