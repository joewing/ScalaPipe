package scalapipe

import scala.reflect.runtime.universe.{runtimeMirror, typeOf}
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

private object RecordValueType {

    def fields(obj: Type): Seq[(String, ValueType)] = {
        val typeMirror = runtimeMirror(obj.getClass.getClassLoader)
        val instanceMirror = typeMirror.reflect(obj)
        val members = instanceMirror.symbol.typeSignature.members
        val fields = members.filter(_.typeSignature <:< typeOf[Type])
        fields.map { f =>
            val value = instanceMirror.reflectField(f.asTerm).get
            (f.name.toString, value.asInstanceOf[Type].create)
        }.toSeq
    }

}

/** Base value type for types with fields (structs and unions). */
private[scalapipe] abstract class RecordValueType(
        val t: Type,
        _bits: Int
    ) extends ValueType(t.name, _bits) {

    protected val fields = RecordValueType.fields(t)

    def fieldNames: Seq[String] = fields.map(_._1)

    def fieldTypes: Seq[ValueType] = fields.map(_._2)

    protected def fieldName(node: ASTNode): Option[String] = node match {
        case sl: SymbolLiteral if fieldNames.contains(sl.symbol) =>
            Some(sl.symbol)
        case sl: SymbolLiteral =>
            Error.raise(s"invalid field: ${sl.symbol}", node)
            None
        case _ =>
            Error.raise(s"invalid field specifier", node)
            None
    }

    def fieldType(node: ASTNode): ValueType = {
        fieldName(node) match {
            case Some(name) => fields.find(_._1 == name).get._2
            case None       => ValueType.void
        }
    }

    /** Get the byte offset of a field. */
    def offset(node: ASTNode): Int

    override def pure = fieldTypes.forall(_.pure)

    override def dependencies = fieldTypes.toSet

}

private object StructValueType {

    // TODO: This assumes 4-byte alignment
    val alignment = 4

    // Pad an offset for proper alignment.
    def pad(offset: Int, vt: ValueType): Int = {
        val align = math.max(math.min(alignment, vt.bytes), 1)
        val left = offset % align
        if (left > 0) {
            offset + alignment - left
        } else {
            offset
        }
    }

    // Get the total size of the structure, including padding.
    def bits(struct: Struct): Int = {
        val fields = RecordValueType.fields(struct).map(_._2)
        val bytes = fields.foldLeft(0) { (total, field) =>
            pad(total, field) + field.bytes
        }
        bytes * 8
    }

}

private[scalapipe] class StructValueType(
        struct: Struct
    ) extends RecordValueType(struct, StructValueType.bits(struct)) {

    def offset(node: ASTNode): Int = fieldName(node) match {
        case Some(name) =>
            val skip = fields.takeWhile(_._1 != name).foldLeft(0) { (t, f) =>
                StructValueType.pad(t, f._2) + f._2.bytes
            }
            return StructValueType.pad(skip, fieldType(node))
        case None =>
            return 0
    }

}

private[scalapipe] class NativeValueType(
        native: NativeType
    ) extends ValueType(native.name, 0) {

    override def pure = false

}

private object UnionValueType {

    def bits(union: Union) = {
        RecordValueType.fields(union).map(_._2.bits).max
    }

}

private[scalapipe] class UnionValueType(
        union: Union
    ) extends RecordValueType(union, UnionValueType.bits(union)) {

    def offset(node: ASTNode): Int = 0

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
