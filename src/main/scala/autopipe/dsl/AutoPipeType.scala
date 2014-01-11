
package autopipe.dsl

import autopipe.{LabelMaker, ValueType}

abstract class AutoPipeType(val name: String) {

    def this() = this(LabelMaker.getTypeLabel)

    override def toString = name

    private[autopipe] def create(): ValueType

}

