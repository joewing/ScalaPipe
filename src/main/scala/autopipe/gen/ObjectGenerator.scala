
package autopipe.gen

import autopipe._

private[autopipe] abstract class ObjectGenerator(val co: CodeObject)
        extends Generator {

    def emitModule: String

    def emitCall(op: String, args: Seq[BaseSymbol]): String

}

