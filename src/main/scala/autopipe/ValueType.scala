package autopipe

import autopipe.dsl.AutoPipeType
import autopipe.dsl.AutoPipeArray
import autopipe.dsl.AutoPipeStruct
import autopipe.dsl.AutoPipeUnion
import autopipe.dsl.AutoPipePointer
import autopipe.dsl.AutoPipeTypeDef
import autopipe.dsl.AutoPipeFixed
import autopipe.dsl.AutoPipeNative

private[autopipe] object ValueType {

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

    def create(apt: AutoPipeType, f: () => ValueType): ValueType = {
        if (valueTypes.contains(apt.name)) {
            valueTypes(apt.name)
        } else {
            val vt = f()
            valueTypes += (apt.name -> vt)
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

private[autopipe] class ValueType(
        val name: String,
        var bits: Int,
        val signed: Boolean = false) {

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

private[autopipe] class IntegerValueType(
        _name: String,
        _bits: Int,
        _signed: Boolean)
    extends ValueType(_name, _bits, _signed) {

}

private[autopipe] class FloatValueType(_name: String, _bits: Int)
    extends ValueType(_name, _bits, true) {

}

private[autopipe] class PointerValueType(_name: String, val itemType: ValueType)
    extends ValueType(_name, 64, false) {

    override def pure = false

    def this(app: AutoPipePointer) = this(app.name, app.itemType.create())

}

private object ArrayValueType {

    def bits(apa: AutoPipeArray): Int = {
        apa.itemType.create.bits * apa.length
    }

}

private[autopipe] class ArrayValueType(apa: AutoPipeArray)
    extends ValueType(apa.name, ArrayValueType.bits(apa)) {

    private[autopipe] val itemType = apa.itemType.create()
    private[autopipe] val length = apa.length

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
    def bits(aps: AutoPipeStruct): Int = {
        val fields = aps.fields.map(_._2.create)
        val bytes = fields.foldLeft(0) { (total, field) =>
            pad(total, field) + field.bytes
        }
        bytes * 8
    }

}

private[autopipe] class StructValueType(aps: AutoPipeStruct)
    extends ValueType(aps.name, StructValueType.bits(aps)) {

    private[autopipe] val fields = aps.fields.map { case (k, v) =>
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

private[autopipe] class NativeValueType(aps: AutoPipeNative)
    extends ValueType(aps.name, 0) {

    override def pure = false

}

private object UnionValueType {

    def bits(apu: AutoPipeUnion) = apu.fields.map(_._2.create.bits).max

}

private[autopipe] class UnionValueType(apu: AutoPipeUnion)
    extends ValueType(apu.name, UnionValueType.bits(apu)) {

    private[autopipe] val fields = apu.fields.map { case (k, v) =>
        (k.name, v.create())
    }

    override def pure = fields.forall(_._2.pure)

    override def dependencies = fields.map(_._2).toSet

}

private[autopipe] class TypeDefValueType(aptd: AutoPipeTypeDef)
    extends ValueType(aptd.name, ValueType.valueType(aptd.value).bits) {

    private[autopipe] val value = aptd.value

}

private[autopipe] class FixedValueType(apf: AutoPipeFixed)
    extends ValueType(apf.name, apf.bits, true) {

    private[autopipe] val fraction = apf.fraction

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
