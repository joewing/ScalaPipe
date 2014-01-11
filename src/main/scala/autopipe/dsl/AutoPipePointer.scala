
package autopipe.dsl

import autopipe.{PointerValueType, ValueType}

object AutoPipePointer {
    def apply(itemType: AutoPipeType) = new AutoPipePointer(itemType)
}

class AutoPipePointer(val itemType: AutoPipeType) extends AutoPipeType {
    private[autopipe] override def create() =
        ValueType.create(this, () => new PointerValueType(this))
}

