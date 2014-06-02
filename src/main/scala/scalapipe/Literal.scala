package scalapipe

import scalapipe.dsl.{Config, Kernel}

object Literal {

    def get(v: Any, kernel: Kernel = null): Literal = v match {
        case b: Boolean => IntLiteral(ValueType.bool, if (b) 1 else 0, kernel)
        case i: Int     => IntLiteral(ValueType.signed32, i, kernel)
        case l: Long    => IntLiteral(ValueType.signed64, l, kernel)
        case f: Float   => FloatLiteral(ValueType.float32, f, kernel)
        case d: Double  => FloatLiteral(ValueType.float64, d, kernel)
        case s: Symbol  => SymbolLiteral(ValueType.any, s.name, kernel)
        case s: String  => StringLiteral(s, kernel)
        case c: Config  => ConfigLiteral(c)
        case null       => null
        case _          =>
            Error.raise("invalid literal: " + v, kernel)
            IntLiteral(ValueType.signed32, 0, kernel)
    }

}

abstract class Literal(
        _t: ValueType,
        _kernel: Kernel
    ) extends ASTNode(NodeType.literal, _kernel) {

    valueType = _t

    private[scalapipe] override def children = List()

    private[scalapipe] override def pure = true

    def long: Long = 0

    def double: Double = 0.0

    def apply(index: Literal): Literal = this

    def set(index: Literal, value: Literal): Literal = null

    override def equals(other: Any): Boolean = false

    override def hashCode(): Int = long.toInt

}

object IntLiteral {

    def apply(t: ValueType, v: Long, kernel: Kernel) =
        new IntLiteral(t, v, kernel)

    def apply(v: Boolean, kernel: Kernel) =
        new IntLiteral(ValueType.bool, if (v) 1 else 0, kernel)

    def apply(v: Byte, kernel: Kernel) =
        new IntLiteral(ValueType.signed8, v.toLong, kernel)

    def apply(v: Char, kernel: Kernel) =
        new IntLiteral(ValueType.signed16, v.toLong, kernel)

    def apply(v: Short, kernel: Kernel) =
        new IntLiteral(ValueType.signed16, v.toLong, kernel)

    def apply(v: Int, kernel: Kernel) =
        new IntLiteral(ValueType.signed32, v.toLong, kernel)

    def apply(v: Long, kernel: Kernel) =
        new IntLiteral(ValueType.signed64, v, kernel)

}

class IntLiteral(
        _t: ValueType,
        val value: Long,
        _kernel: Kernel
    ) extends Literal(_t, _kernel) {

    override def long: Long = value

    override def double: Double = value.toDouble

    override def toString = valueType.bits match {
        case 8  => (value & 0xFFL).toString
        case 16 => (value & 0xFFFFL).toString
        case 32 => (value & 0xFFFFFFFFL).toString
        case _  => value.toString + "L"
    }

    override def set(index: Literal, value: Literal): Literal =
        IntLiteral(valueType, value.long, kernel)

    override def equals(other: Any): Boolean = other match {
        case l: Literal => value == l.long
        case _ => false
    }

}

object FloatLiteral {

    def apply(t: ValueType, v: Double, kernel: Kernel) =
        new FloatLiteral(t, v, kernel)

    def apply(v: Float, kernel: Kernel) =
        new FloatLiteral(ValueType.float32, v.toDouble, kernel)

    def apply(v: Double, kernel: Kernel) =
        new FloatLiteral(ValueType.float64, v.toDouble, kernel)

}

class FloatLiteral(
        _t: ValueType,
        val value: Double,
        _kernel: Kernel
    ) extends Literal(_t, _kernel) {

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

    override def set(index: Literal, value: Literal): Literal =
        new FloatLiteral(valueType, value.double, null)

    override def equals(other: Any): Boolean = other match {
        case l: Literal => value == l.double
        case _ => false
    }

}

object StringLiteral {

    def apply(t: ValueType, v: String, kernel: Kernel) =
        new StringLiteral(t, v, kernel)

    def apply(v: String, kernel: Kernel) =
        new StringLiteral(ValueType.string, v, kernel)

    private val octal = "01234567"

    def sanitize(str: String): String = {
        str.flatMap { ch =>
            if (ch == '\\') {
                "\\\\"
            } else if (ch < 0x20 || ch > 0x7E || ch == '\"') {
                val first = octal((ch >> 6) & 7)
                val second = octal((ch >> 3) & 7)
                val third = octal(ch & 7)
                "\\" + first + second + third
            } else {
                ch.toString
            }
        }
    }

}

class StringLiteral(
        _t: ValueType,
        val value: String,
        _kernel: Kernel
    ) extends Literal(_t, _kernel) {

    override def toString = "\"" + StringLiteral.sanitize(value) + "\""

    override def equals(other: Any): Boolean = other match {
        case l: StringLiteral => value.equals(l.value)
        case _ => false
    }

}

object SymbolLiteral {

    def apply(t: ValueType, s: String, kernel: Kernel) =
        new SymbolLiteral(t, s, kernel)

    def apply(s: String, kernel: Kernel) =
        new SymbolLiteral(ValueType.any, s, kernel)

}

class SymbolLiteral(
        _t: ValueType,
        val symbol: String,
        _kernel: Kernel
    ) extends Literal(_t, _kernel) {

    SymbolValidator.validate(symbol, this)

    override def toString = symbol

    override def equals(other: Any): Boolean = other match {
        case l: SymbolLiteral => symbol.equals(l.symbol)
        case _ => false
    }

}

object ConfigLiteral {

    def apply(c: Config) = new ConfigLiteral(c)

}

class ConfigLiteral(
        val config: Config
    ) extends Literal(ValueType.any, null) {

    val name = config.name.name

    val default = config.default

    override def toString = name

    override def equals(other: Any): Boolean = other match {
        case l: ConfigLiteral => l.config == config
        case _ => false
    }

}
