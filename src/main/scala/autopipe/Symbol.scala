
package autopipe

import scala.math.Ordered

private[autopipe] object TempSymbol {

    var id = 1

    def next: Int = {
        val result = id
        id += 1
        return result
    }

}

private[autopipe] abstract class BaseSymbol(
        val name: String,
        val valueType: ValueType)
    extends Ordered[BaseSymbol] {

    var isRegister = true

    def compare(that: BaseSymbol) = this.name.compare(that.name)

    override def equals(arg: Any): Boolean = arg.toString == toString

    override def hashCode(): Int = toString.hashCode

}

private[autopipe] abstract class PortSymbol(
        _name: String,
        _valueType: ValueType,
        val id: Int)
    extends BaseSymbol(_name, _valueType) {

    val index = LabelMaker.getPortIndex

}

private[autopipe] class InputSymbol(
        _name: String,
        _valueType: ValueType,
        _id: Int)
    extends PortSymbol(_name, _valueType, _id) {

    override def toString = "input" + _id

}

private[autopipe] class OutputSymbol(
        _name: String,
        _valueType: ValueType,
        _id: Int)
    extends PortSymbol(_name, _valueType, _id) {

    override def toString = "output" + id

}

private[autopipe] class TempSymbol(
        _valueType: ValueType,
        val id: Int = TempSymbol.next
    ) extends BaseSymbol("temp" + id, _valueType) {

    override def toString = "temp" + id

}

private[autopipe] class StateSymbol(
        _name: String,
        _valueType: ValueType,
        val value: Literal
    ) extends BaseSymbol(_name, _valueType) {

    var isLocal = false

    override def toString = name

}

private[autopipe] class ConfigSymbol(
        _name: String,
        _valueType: ValueType,
        val value: Literal
    ) extends BaseSymbol(_name, _valueType) {

    override def toString = name

}

private[autopipe] class ImmediateSymbol(
        val value: Literal
    ) extends BaseSymbol("", value.valueType) {

    override def toString = value.toString

    override def equals(other: Any): Boolean = other match {
        case im: ImmediateSymbol => value.equals(im.value)
        case _ => false
    }

}

