package scalapipe

private[scalapipe] abstract class KernelPort(  
        val name: String,
        val valueType: ValueType
    )

private[scalapipe] class KernelInput(
        _name: String,
        _valueType: ValueType
    ) extends KernelPort(_name, _valueType)

private[scalapipe] class KernelOutput(
        _name: String,
        _valueType: ValueType
    ) extends KernelPort(_name, _valueType)

