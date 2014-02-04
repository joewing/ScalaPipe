package scalapipe

private[scalapipe] abstract class KernelSymbol(
        val name: String,
        val valueType: ValueType)

private[scalapipe] abstract class KernelPort(
        _name: String,
        _valueType: ValueType
    ) extends KernelSymbol(_name, _valueType)

private[scalapipe] class KernelInput(
        _name: String,
        _valueType: ValueType
    ) extends KernelPort(_name, _valueType)

private[scalapipe] class KernelOutput(
        _name: String,
        _valueType: ValueType
    ) extends KernelPort(_name, _valueType)

private[scalapipe] class KernelLocal(
        _name: String,
        _valueType: ValueType,
        val init: Any = null
    ) extends KernelSymbol(_name, _valueType)

private[scalapipe] class KernelConfig(
        _name: String,
        _valueType: ValueType,
        val default: Any
    ) extends KernelSymbol(_name, _valueType)
