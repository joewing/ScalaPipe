package autopipe

import autopipe.dsl.AutoPipeBlock

object Literal {

    def get(v: Any, apb: AutoPipeBlock = null): Literal = v match {
        case b: Boolean => IntLiteral(ValueType.bool, if (b) 1 else 0, apb)
        case i: Int     => IntLiteral(ValueType.signed32, i, apb)
        case l: Long    => IntLiteral(ValueType.signed64, l, apb)
        case f: Float   => FloatLiteral(ValueType.float32, f, apb)
        case d: Double  => FloatLiteral(ValueType.float64, d, apb)
        case s: Symbol  => SymbolLiteral(ValueType.any, s.name, apb)
        case s: String  => StringLiteral(s, apb)
        case null       => null
        case _          =>
            Error.raise("invalid literal: " + v, apb)
            IntLiteral(ValueType.signed32, 0, apb)
    }

}

abstract class Literal(
        _t: ValueType,
        _apb: AutoPipeBlock
    ) extends ASTNode(NodeType.literal, _apb) {

    valueType = _t

    private[autopipe] override def children = List()

    private[autopipe] override def isPure = true

    def long: Long = 0

    def double: Double = 0.0

    def isTrue: Boolean = false

    def apply(index: Literal): Literal = this

    def set(index: Literal, value: Literal): Literal = null

    override def equals(other: Any): Boolean = false

    override def hashCode(): Int = long.toInt

}

object IntLiteral {

    def apply(t: ValueType, v: Long, apb: AutoPipeBlock) =
        new IntLiteral(t, v, apb)

    def apply(v: Boolean, apb: AutoPipeBlock) =
        new IntLiteral(ValueType.bool, if (v) 1 else 0, apb)

    def apply(v: Byte, apb: AutoPipeBlock) =
        new IntLiteral(ValueType.signed8, v.toLong, apb)

    def apply(v: Char, apb: AutoPipeBlock) =
        new IntLiteral(ValueType.signed16, v.toLong, apb)

    def apply(v: Short, apb: AutoPipeBlock) =
        new IntLiteral(ValueType.signed16, v.toLong, apb)

    def apply(v: Int, apb: AutoPipeBlock) =
        new IntLiteral(ValueType.signed32, v.toLong, apb)

    def apply(v: Long, apb: AutoPipeBlock) =
        new IntLiteral(ValueType.signed64, v, apb)

}

class IntLiteral(
        _t: ValueType,
        val value: Long,
        _apb: AutoPipeBlock
    ) extends Literal(_t, _apb) {

    override def long: Long = value

    override def double: Double = value.toDouble

    override def toString = value.toString

    override def isTrue: Boolean = value != 0

    override def set(index: Literal, value: Literal): Literal =
        IntLiteral(valueType, value.long, apb)

    override def equals(other: Any): Boolean = other match {
        case l: Literal => value == l.long
        case _ => false
    }

}

object FloatLiteral {

    def apply(t: ValueType, v: Double, apb: AutoPipeBlock) =
        new FloatLiteral(t, v, apb)

    def apply(v: Float, apb: AutoPipeBlock) =
        new FloatLiteral(ValueType.float32, v.toDouble, apb)

    def apply(v: Double, apb: AutoPipeBlock) =
        new FloatLiteral(ValueType.float64, v.toDouble, apb)

}

class FloatLiteral(
        _t: ValueType,
        val value: Double,
        _apb: AutoPipeBlock
    ) extends Literal(_t, _apb) {

    def rawFloat: Int = {
        import java.nio.ByteBuffer
        val buffer = ByteBuffer.allocate(4)
        buffer.putFloat(value.toFloat)
        buffer.rewind
        buffer.getInt
    }

    def rawDouble: Long = {
        import java.nio.ByteBuffer
        val buffer = ByteBuffer.allocate(8)
        buffer.putDouble(value.toDouble)
        buffer.rewind
        buffer.getLong
    }

    override def long: Long = value.toLong

    override def double: Double = value

    override def toString = value.toString

    override def isTrue: Boolean = value != 0.0

    override def set(index: Literal, value: Literal): Literal =
        new FloatLiteral(valueType, value.double, null)

    override def equals(other: Any): Boolean = other match {
        case l: Literal => value == l.double
        case _ => false
    }

}

object StringLiteral {

    def apply(t: ValueType, v: String, apb: AutoPipeBlock) =
        new StringLiteral(t, v, apb)

    def apply(v: String, apb: AutoPipeBlock) =
        new StringLiteral(ValueType.string, v, apb)

}

class StringLiteral(
        _t: ValueType,
        val value: String,
        _apb: AutoPipeBlock
    ) extends Literal(_t, _apb) {

    override def toString = "\"" + value + "\""

    override def isTrue: Boolean = value != null

    override def equals(other: Any): Boolean = other match {
        case l: StringLiteral => value.equals(l.value)
        case _ => false
    }

}

object SymbolLiteral {

    def apply(t: ValueType, s: String, apb: AutoPipeBlock) =
        new SymbolLiteral(t, s, apb)

    def apply(s: String, apb: AutoPipeBlock) =
        new SymbolLiteral(ValueType.any, s, apb)

}

class SymbolLiteral(
        _t: ValueType,
        val symbol: String,
        _apb: AutoPipeBlock
    ) extends Literal(_t, _apb) {

    override def toString = symbol

    override def isTrue: Boolean = false

    override def equals(other: Any): Boolean = other match {
        case l: SymbolLiteral => symbol.equals(l.symbol)
        case _ => false
    }

}
