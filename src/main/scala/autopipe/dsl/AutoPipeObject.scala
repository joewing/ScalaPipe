
package autopipe.dsl

import autopipe._
import autopipe.gen.ObjectGenerator

abstract class AutoPipeObject(val name: String) {

    def this() = this(LabelMaker.getBlockLabel)

    private[autopipe] def generator(co: CodeObject): ObjectGenerator

}

