package autopipe.gen

import autopipe._

private[autopipe] abstract class ObjectGenerator(
        val kt: KernelType
    ) extends Generator {

    def emitModule: String

    def emitCall(op: String, args: Seq[BaseSymbol]): String

}
