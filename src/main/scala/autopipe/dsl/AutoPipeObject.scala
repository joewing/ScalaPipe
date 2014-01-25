
package autopipe.dsl

import autopipe._
import autopipe.gen.ObjectGenerator

abstract class AutoPipeObject(val name: String) {

    def this() = this(LabelMaker.getKernelLabel)

    private[autopipe] def generator(kt: KernelType): ObjectGenerator

}

