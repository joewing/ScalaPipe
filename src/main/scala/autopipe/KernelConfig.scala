package autopipe

import autopipe.dsl.AutoPipeType

private[autopipe] class KernelConfig(
        val name: String,
        val t: AutoPipeType,
        val default: Any
    ) {

    def valueType = t.create

}
