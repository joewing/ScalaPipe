package scalapipe

import scalapipe.dsl.Type
import scalapipe.dsl.Vector
import scalapipe.dsl.Struct
import scalapipe.dsl.Union
import scalapipe.dsl.Pointer
import scalapipe.dsl.TypeDef
import scalapipe.dsl.Fixed
import scalapipe.dsl.NativeType

private[scalapipe] object ValueType {

    private var valueTypes = Map[String, ValueType]()

    val unsigned8   = insert(new IntegerValueType("UNSIGNED8",   8, false))
    val signed8     = insert(new IntegerValueType("SIGNED8",     8, true))
    val unsigned16  = insert(new IntegerValueType("UNSIGNED16", 16, false))
    val signed16    = insert(new IntegerValueType("SIGNED16",   16, true))
    val unsigned32  = insert(new IntegerValueType("UNSIGNED32", 32, false))
    val signed32    = insert(new IntegerValueType("SIGNED32",   32, true))
    val unsigned64  = insert(new IntegerValueType("UNSIGNED64", 64, false))
    val signed64    = insert(new IntegerValueType("SIGNED64",   64, true))
    val float32     = insert(new FloatValueType(  "FLOAT32",    32))
    val float64     = insert(new FloatValueType(  "FLOAT64",    64))
    val float96     = insert(new FloatValueType(  "FLOAT96",    96))

    val any         = insert(new ValueType("any", 0))
    val string      = insert(new ValueType("STRING", 0))
    val void        = insert(new ValueType("void", 0))
    val bool        = signed8

    private def insert(vt: ValueType): ValueType = {
        valueTypes += (vt.name -> vt)
        vt
    }

    def create(t: Type, f: () => ValueType): ValueType = {
        if (valueTypes.contains(t.name)) {
            valueTypes(t.name)
        } else {
            val vt = f()
            valueTypes += (t.name -> vt)
            vt
        }
    }

    def valueType(name: String): ValueType = valueTypes(name)

    def pointer(vt: ValueType): ValueType = {
        val name = vt.name + "*"
        if (valueTypes.contains(name)) {
            valueTypes(name)
        } else {
            val pt = new PointerValueType(name, vt)
            valueTypes += (name -> pt)
            pt
        }
    }

}

private[scalapipe] class ValueType(
        val name: String,
        var bits: Int,
        val signed: Boolean = false
    ) {

    def baseType: ValueType = this

    def pure = true

    def bytes: Int = (bits + 7) / 8

    def flat = true

    override def toString: String = name

    override def equals(other: Any): Boolean = {
        if(other.isInstanceOf[ValueType]) {
            name == other.toString || name == "any" || other.toString == "any"
        } else {
            false
        }
    }

    def dependencies = Set[ValueType]()

}

private[scalapipe] class IntegerValueType(
        _name: String,
        _bits: Int,
        _signed: Boolean)
    extends ValueType(_name, _bits, _signed) {

}

private[scalapipe] class FloatValueType(_name: String, _bits: Int)
    extends ValueType(_name, _bits, true) {

}

private[scalapipe] class PointerValueType(
        _name: String,
        val itemType: ValueType
    ) extends ValueType(_name, 64, false) {

    override def pure = false

    def this(ptr: Pointer) = this(ptr.name, ptr.itemType.create())

}

private object ArrayValueType {

    def bits(v: Vector): Int = {
        v.itemType.create.bits * v.length
    }

}

private[scalapipe] class ArrayValueType(v: Vector)
    extends ValueType(v.name, ArrayValueType.bits(v)) {

    private[scalapipe] val itemType = v.itemType.create()
    private[scalapipe] val length = v.length

    override def pure = itemType.pure

    override def baseType = itemType

    override def flat = bits < 1024

    override def dependencies = Set[ValueType](this, itemType)

}

private object StructValueType {

    // TODO: This assumes 4-byte alignment
    val alignment = 4

    // Pad an offset for proper alignment.
    def pad(offset: Int, vt: ValueType): Int = {
        val align = math.min(alignment, vt.bytes)
        val left = offset % align
        if (left > 0) {
            offset + alignment - left
        } else {
            offset
        }
    }

    // Get the total size of the structure, including padding.
    def bits(struct: Struct): Int = {
        val fields = struct.fields.map(_._2.create)
        val bytes = fields.foldLeft(0) { (total, field) =>
            pad(total, field) + field.bytes
        }
        bytes * 8
    }

}

private[scalapipe] class StructValueType(
        struct: Struct
    ) extends ValueType(struct.name, StructValueType.bits(struct)) {

    private[scalapipe] val fields = struct.fields.map { case (k, v) =>
        (k.name, v.create())
    }

    // Get the byte offset of a field in the structure.
    def offset(name: String): Int = {
        val ftype = fields.find(_._1 == name) match {
            case Some(t) => t._2
            case None =>
                Error.raise(s"struct field not found: $name")
                null
        }
        val skip = fields.takeWhile(_._1 != name).foldLeft(0) { (t, f) =>
            StructValueType.pad(t, f._2) + f._2.bytes
        }
        StructValueType.pad(skip, ftype)
    }

    override def pure = fields.forall(_._2.pure)

    override def dependencies = fields.map(_._2).toSet

}

private[scalapipe] class NativeValueType(
        native: NativeType
    ) extends ValueType(native.name, 0) {

    override def pure = false

}

private object UnionValueType {

    def bits(union: Union) = union.fields.map(_._2.create.bits).max

}

private[scalapipe] class UnionValueType(
        union: Union
    ) extends ValueType(union.name, UnionValueType.bits(union)) {

    private[scalapipe] val fields = union.fields.map { case (k, v) =>
        (k.name, v.create())
    }

    override def pure = fields.forall(_._2.pure)

    override def dependencies = fields.map(_._2).toSet

}

private[scalapipe] class TypeDefValueType(
        typedef: TypeDef
    ) extends ValueType(typedef.name, ValueType.valueType(typedef.value).bits) {

    private[scalapipe] val value = typedef.value

}

private[scalapipe] class FixedValueType(fixed: Fixed)
    extends ValueType(fixed.name, fixed.bits, true) {

    private[scalapipe] val fraction = fixed.fraction

    override def baseType: ValueType = bits match {
        case 8  => ValueType.signed8
        case 16 => ValueType.signed16
        case 32 => ValueType.signed32
        case 64 => ValueType.signed64
        case _  =>
            Error.raise("invalid bit count for fixed type: " + bits)
            ValueType.void
    }

}
