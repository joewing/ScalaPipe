package autopipe

import autopipe.dsl.AutoPipeType

private[autopipe] abstract class KernelPort(  
        val name: String,
        val t: AutoPipeType
    ) {

    def valueType = t.create()

}

private[autopipe] class KernelInput(
        _name: String,
        _t: AutoPipeType
    ) extends KernelPort(_name, _t)

private[autopipe] class KernelOutput(
        _name: String,
        _t: AutoPipeType
    ) extends KernelPort(_name, _t)

