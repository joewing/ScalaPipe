package scalapipe

import scalapipe.dsl.AutoPipeType

private[scalapipe] abstract class KernelPort(  
        val name: String,
        val t: AutoPipeType
    ) {

    def valueType = t.create()

}

private[scalapipe] class KernelInput(
        _name: String,
        _t: AutoPipeType
    ) extends KernelPort(_name, _t)

private[scalapipe] class KernelOutput(
        _name: String,
        _t: AutoPipeType
    ) extends KernelPort(_name, _t)

