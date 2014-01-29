package scalapipe

import scalapipe.dsl.AutoPipeType

private[scalapipe] class KernelConfig(
        val name: String,
        val t: AutoPipeType,
        val default: Any
    ) {

    def valueType = t.create

}
