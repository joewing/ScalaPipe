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

    val unsigned8   = insert(new IntegerValueType("UNSIGNED8",    8, false))
    val signed8     = insert(new IntegerValueType("SIGNED8",      8, true))
    val unsigned16  = insert(new IntegerValueType("UNSIGNED16", 16, false))
    val signed16    = insert(new IntegerValueType("SIGNED16",    16, true))
    val unsigned32  = insert(new IntegerValueType("UNSIGNED32", 32, false))
    val signed32    = insert(new IntegerValueType("SIGNED32",    32, true))
    val unsigned64  = insert(new IntegerValueType("UNSIGNED64", 64, false))
    val signed64    = insert(new IntegerValueType("SIGNED64",    64, true))
    val float32     = insert(new FloatValueType(  "FLOAT32",     32))
    val float64     = insert(new FloatValueType(  "FLOAT64",     64))
    val float96     = insert(new FloatValueType(  "FLOAT96",     96))

    val any         = insert(new ValueType("any"))
    val string      = insert(new ValueType("STRING"))
    val void        = insert(new ValueType("void"))
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

    def getPointer(vt: ValueType): ValueType = {
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
        var bits: Int = 0,
        val signed: Boolean = false) {

    def baseType: ValueType = this

    def isPure: Boolean = true

    def bytes: Int = (bits + 7) / 8

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

    override def isPure: Boolean = false

    def this(app: AutoPipePointer) = this(app.name, app.itemType.create())

}

private[autopipe] class ArrayValueType(apa: AutoPipeArray)
    extends ValueType(apa.name) {

    private[autopipe] val itemType = apa.itemType.create()
    private[autopipe] val length = apa.length

    bits = length * itemType.bits

    override def isPure: Boolean = itemType.isPure

    override def baseType = itemType

    override def dependencies = Set[ValueType](this) + itemType

}

private object StructValueType {

    def getBits(aps: AutoPipeStruct) =
        aps.fields.foldLeft(0) { (a, v) =>
            a + v._2.create().bits
        }

}

private[autopipe] class StructValueType(aps: AutoPipeStruct)
    extends ValueType(aps.name, StructValueType.getBits(aps)) {

    private[autopipe] val fields = aps.fields.map { case (k, v) =>
        (k.name, v.create())
    }

    override def isPure: Boolean = fields.forall(_._2.isPure)

    override def dependencies = Set(fields.map(_._2).toSeq: _*)

}

private[autopipe] class NativeValueType(aps: AutoPipeNative)
    extends ValueType(aps.name, 0) {

    override def isPure: Boolean = false

}

private object UnionValueType {

    def getBits(apu: AutoPipeUnion) =
        apu.fields.foldLeft(0) { (a, v) =>
            math.max(a, v._2.create().bits)
        }

}

private[autopipe] class UnionValueType(apu: AutoPipeUnion)
    extends ValueType(apu.name, UnionValueType.getBits(apu)) {

    private[autopipe] val fields = apu.fields.map { case (k, v) =>
        (k.name, v.create())
    }

    override def isPure: Boolean = fields.forall(_._2.isPure)

    override def dependencies = Set(fields.map(_._2).toSeq: _*)

}

private[autopipe] class TypeDefValueType(aptd: AutoPipeTypeDef)
    extends ValueType(aptd.name) {

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
