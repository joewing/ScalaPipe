package scalapipe

import scalapipe.dsl.Type

private[scalapipe] class KernelConfig(
        val name: String,
        val t: Type,
        val default: Any
    ) {

    def valueType = t.create

}
