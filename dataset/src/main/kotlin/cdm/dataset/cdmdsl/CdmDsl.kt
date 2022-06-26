package cdm.dataset.cdmdsl

import dev.cdm.array.ArrayType

class CdmDsl constructor(val location : String?) {
    var root = CdmdslGroup("")

    /* add or replace. TODO what about all references to it ??
    fun dimension(dimName: String, length : Int): CdmdslDimension {
        return root.dimension(dimName, length)
    }
    fun enumTypedef(enumName: String, type: ArrayType, map : MutableMap<Int, String>?): CdmdslEnum {
        return root.enumTypedef(enumName, type, map)
    }
    fun attribute(attName : String, attValue : String, type: ArrayType? = null) : CdmdslAttribute {
        return root.attribute(attName, attValue, type)
    }
    fun attribute(attName : String) : CdmdslAttribute {
        return root.attribute(attName)
    }

    // add or replace.
    fun variable(varName: String, type: ArrayType, dimensions : String): CdmdslVariable {
        return root.variable(varName, type, dimensions)
    }
    fun variable(varName: String, type: ArrayType, dimensions : String,
                    lambda: CdmdslVariable.() -> Unit): CdmdslVariable {
        return root.variable(varName, type, dimensions, lambda)
    }
    fun variable(varName: String): CdmdslVariable {
        return root.variable(varName)
    }
    fun variable(varName: String, lambda: CdmdslVariable.() -> Unit): CdmdslVariable {
        return root.variable(varName, lambda)
    }

    fun coordSystem(dimNames: String): CdmdslCoordSystem {
        return root.coordSystem(dimNames)
    }
    fun projection(name: String): CdmdslProjection{
        return root.projection(name)
    }
    fun projection(name: String, lambda: CdmdslProjection.() -> Unit): CdmdslProjection {
        return root.projection(name, lambda)
    } */
}

enum class Action {AddOrReplace, Modify, Remove}

class CdmdslAttribute constructor(val attName : String) {
    var rename : String? = null
    var type : ArrayType? = null
    var values : String? = null
    var dvalue : Double? = null
    var action : Action = Action.Modify

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
    fun rename(rename : String?) : CdmdslAttribute {
        this.rename = rename
        return this
    }
    fun remove() : CdmdslAttribute {
        this.action = Action.Remove
        return this
    }
}

class CdmdslDimension constructor(val dimName : String, val length : Int) {
    var action : Action = Action.Modify
}

class CdmdslEnum constructor(val enumName : String, val basetype: ArrayType? = null, val values : MutableMap<Int, String> = mutableMapOf()) {
    var action : Action = Action.Modify

    fun add(code: Int, name: String) {
        values[code] = name
    }
}

open class CdmdslVariable constructor(val varName : String) {
    val attributes = mutableMapOf<String, CdmdslAttribute>()
    var dimensions : String? = null
    var rename : String? = null
    var type : ArrayType? = null
    var coordSysRef : String? = null
    var values : String? = null
    var dvalues : DoubleArray? = null
    var action : Action = Action.Modify

    constructor(varName: String, type: ArrayType, dimensions : String) : this(varName) {
        this.type = type
        this.dimensions = dimensions
    }

    fun rename(rename : String?) : CdmdslVariable {
        this.rename = rename
        return this
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
        return attributes.getOrPut(attName) {CdmdslAttribute(attName)}
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
        return variables.getOrPut(varName) {CdmdslVariable(varName)}
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
        return structures.getOrPut(varName) {CdmdslStructure(varName)}
    }
}

class CdmdslGroup constructor(val groupName : String) {
    val attributes = mutableMapOf<String, CdmdslAttribute>()
    val dimensions = mutableMapOf<String, CdmdslDimension>()
    var enumTypedefs= mutableMapOf<String, CdmdslEnum>()
    val variables = mutableMapOf<String, CdmdslVariable>()
    val structures = mutableMapOf<String, CdmdslStructure>()
    val groups = mutableMapOf<String, CdmdslGroup>()
    val coordSystems = mutableMapOf<String, CdmdslCoordSystem>()
    val projections = mutableMapOf<String, CdmdslProjection>()
    var rename : String? = null
    var action : Action = Action.Modify

    fun rename(rename : String?) : CdmdslGroup {
        this.rename = rename
        return this
    }

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
        return attributes.getOrPut(attName) {CdmdslAttribute(attName, attValue, type)}
    }
    fun attribute(attName: String) : CdmdslAttribute {
        return attributes.getOrPut(attName) {CdmdslAttribute(attName)}
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
        return variables.getOrPut(varName) {CdmdslVariable(varName)}
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
        return structures.getOrPut(varName) {CdmdslStructure(varName)}
    }

    fun group(groupName: String): CdmdslGroup {
        return groups.getOrPut(groupName) {CdmdslGroup(groupName)}
    }
    fun group(groupName: String, lambda: CdmdslGroup.() -> Unit): CdmdslGroup {
        val g = group(groupName)
        g.lambda()
        return g
    }

    fun coordSystem(dimNames: String): CdmdslCoordSystem {
        return coordSystems.getOrPut(dimNames) {CdmdslCoordSystem(dimNames)}
    }

    fun projection(name: String): CdmdslProjection {
        return projections.getOrPut(name) {CdmdslProjection(name)}
    }
    fun projection(name: String, lambda: CdmdslProjection.() -> Unit): CdmdslProjection {
        val proj = projection(name)
        proj.lambda()
        return proj
    }
}

class CdmdslCoordSystem constructor(val dimNames : String) {
    var projection : String? = null

    fun setProjection(projection : String) : CdmdslCoordSystem {
        this.projection = projection
        return this
    }
}
class CdmdslProjection constructor(val projName : String) {
    var varName : String? = null
    var parameters = mutableMapOf<String, CdmdslAttribute>()

    fun useVariable(varName : String) : CdmdslProjection {
        this.varName = varName
        return this
    }

    fun attribute(attName : String, attValue : String, type: ArrayType? = null) : CdmdslAttribute {
        val att = CdmdslAttribute(attName, attValue, type)
        parameters[attName] = att
        return att
    }
    fun attribute(attName: String) : CdmdslAttribute {
        return parameters.getOrPut(attName) {CdmdslAttribute(attName)}
    }
}

fun cdmdsl(location : String, lambda: CdmdslGroup.() -> Unit): CdmDsl {
    val builder = CdmDsl(location)
    builder.root.lambda()
    return builder
}
