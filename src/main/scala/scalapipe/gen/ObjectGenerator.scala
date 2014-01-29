package scalapipe.gen

import scalapipe._

private[scalapipe] abstract class ObjectGenerator(
        val kt: KernelType
    ) extends Generator {

    def emitModule: String

    def emitCall(op: String, args: Seq[BaseSymbol]): String

}
