
package autopipe

import autopipe.dsl.AutoPipeBlock

object Literal {

    def get(v: Any, apb: AutoPipeBlock = null): Literal = v match {
        case b: Boolean    => new IntLiteral(ValueType.bool, if (b) 1 else 0)
        case i: Int         => new IntLiteral(ValueType.signed32, i)
        case l: Long        => new IntLiteral(ValueType.signed64, l)
        case f: Float      => new FloatLiteral(ValueType.float32, f)
        case d: Double     => new FloatLiteral(ValueType.float64, d)
        case s: Symbol     => new SymbolLiteral(ValueType.any, s.name)
        case s: String     => new StringLiteral(s)
        case null            => null
        case _                => Error.raise("invalid literal: " + v, apb)
    }

}

abstract class Literal(_t: ValueType, _apb: AutoPipeBlock)
    extends ASTNode(NodeType.literal, _apb) {

    valueType = _t

    private[autopipe] override def children = List()

    private[autopipe] override def isPure = true

    private[autopipe] override def run(i: BlockInterface): Literal = this

    def long: Long = 0

    def double: Double = 0.0

    def isTrue: Boolean = false

    def eval(op: NodeType.Value, rhs: Literal): Literal = null

    def apply(index: Literal): Literal = this

    def set(index: Literal, value: Literal): Literal = null

    override def equals(other: Any): Boolean = false

    override def hashCode(): Int = long.toInt

}

class IntLiteral(_t: ValueType, val value: Long,
                      _apb: AutoPipeBlock) extends Literal(_t, _apb) {

    def this(t: ValueType, v: Long) = this(t, v, null)

    def this(v: Boolean, apb: AutoPipeBlock) =
        this(ValueType.bool, if (v) 1 else 0, apb)

    def this(v: Boolean) = this(v, null)

    def this(v: Byte, apb: AutoPipeBlock) =
        this(ValueType.signed8, v.toLong, apb)

    def this(v: Byte) = this(v, null)

    def this(v: Char, apb: AutoPipeBlock) =
        this(ValueType.signed16, v.toLong, apb)

    def this(v: Char) = this(v, null)

    def this(v: Short, apb: AutoPipeBlock) =
        this(ValueType.signed16, v.toLong, apb)

    def this(v: Short) = this(v, null)

    def this(v: Int, apb: AutoPipeBlock) =
        this(ValueType.signed32, v.toLong, apb)

    def this(v: Int) = this(v, null)

    def this(v: Long, apb: AutoPipeBlock) =
        this(ValueType.signed64, v, apb)

    def this(v: Long) = this(v, null)

    override def long: Long = value

    override def double: Double = value.toDouble

    override def toString = value.toString

    override def isTrue: Boolean = value != 0

    override def eval(op: NodeType.Value, rhs: Literal): Literal = op match {
        case NodeType.neg     => new IntLiteral(valueType, -value)
        case NodeType.not     => new IntLiteral(value == 0)
        case NodeType.compl  => new IntLiteral(valueType, ~value)
        case NodeType.land    => new IntLiteral(value != 0 && rhs.long != 0)
        case NodeType.lor     => new IntLiteral(value != 0 || rhs.long != 0)
        case NodeType.and     => new IntLiteral(valueType, value & rhs.long)
        case NodeType.or      => new IntLiteral(valueType, value | rhs.long)
        case NodeType.xor     => new IntLiteral(valueType, value ^ rhs.long)
        case NodeType.shr     => new IntLiteral(valueType, value >> rhs.long)
        case NodeType.shl     => new IntLiteral(valueType, value << rhs.long)
        case NodeType.add     => new IntLiteral(valueType, value + rhs.long)
        case NodeType.sub     => new IntLiteral(valueType, value - rhs.long)
        case NodeType.mul     => new IntLiteral(valueType, value * rhs.long)
        case NodeType.div     => new IntLiteral(valueType, value / rhs.long)
        case NodeType.mod     => new IntLiteral(valueType, value % rhs.long)
        case NodeType.eq      => new IntLiteral(value == rhs.long)
        case NodeType.ne      => new IntLiteral(value != rhs.long)
        case NodeType.gt      => new IntLiteral(value > rhs.long)
        case NodeType.lt      => new IntLiteral(value < rhs.long)
        case NodeType.ge      => new IntLiteral(value >= rhs.long)
        case NodeType.le      => new IntLiteral(value <= rhs.long)
        case _                    => Error.raise("invalid op: " + op)
    }

    override def set(index: Literal, value: Literal): Literal =
        new IntLiteral(valueType, value.long, null)

    override def equals(other: Any): Boolean = other match {
        case l: Literal => value == l.long
        case _ => false
    }

}

class FloatLiteral(_t: ValueType, val value: Double,
                         _apb: AutoPipeBlock) extends Literal(_t, _apb) {

    def this(t: ValueType, v: Double) = this(t, v, null)

    def this(v: Float, apb: AutoPipeBlock) =
        this(ValueType.float32, v.toDouble, apb)

    def this(v: Float) = this(v, null)

    def this(v: Double, apb: AutoPipeBlock) = this(ValueType.float64, v, apb)

    def this(v: Double) = this(v, null)

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

    override def eval(op: NodeType.Value, rhs: Literal): Literal = {
        Error.raise("eval not implemented on FloatLiteral")
    }

    override def set(index: Literal, value: Literal): Literal =
        new FloatLiteral(valueType, value.double, null)

    override def equals(other: Any): Boolean = other match {
        case l: Literal => value == l.double
        case _ => false
    }

}

class StringLiteral(_t: ValueType, val value: String,
                          _apb: AutoPipeBlock) extends Literal(_t, _apb) {

    def this(v: String, apb: AutoPipeBlock) =
        this(ValueType.string, v, apb)

    def this(v: String) = this(v, null)

    override def toString = "\"" + value + "\""

    override def isTrue: Boolean = value != null

    override def eval(op: NodeType.Value, rhs: Literal): Literal = {
        Error.raise("eval not implemented on StringLiteral")
    }

    override def equals(other: Any): Boolean = other match {
        case l: StringLiteral => value.equals(l.value)
        case _ => false
    }

}

class SymbolLiteral(_t: ValueType, val symbol: String,
                          _apb: AutoPipeBlock)
    extends Literal(_t, _apb) {

    def this(t: ValueType, s: String) = this(t, s, null)

    def this(s: String) = this(ValueType.any, s)

    override def toString = symbol

    override def isTrue: Boolean = false

    override def eval(op: NodeType.Value, rhs: Literal): Literal = {
        Error.raise("eval not implemented on SymbolLiteral")
    }

    override def equals(other: Any): Boolean = other match {
        case l: SymbolLiteral => symbol.equals(l.symbol)
        case _ => false
    }

}

