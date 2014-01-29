package scalapipe

import scalapipe.dsl.Type

private[scalapipe] abstract class KernelPort(  
        val name: String,
        val t: Type
    ) {

    def valueType = t.create()

}

private[scalapipe] class KernelInput(
        _name: String,
        _t: Type
    ) extends KernelPort(_name, _t)

private[scalapipe] class KernelOutput(
        _name: String,
        _t: Type
    ) extends KernelPort(_name, _t)

