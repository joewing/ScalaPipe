
package scalapipe.dsl

import scalapipe.{PointerValueType, ValueType}

object AutoPipePointer {
    def apply(itemType: AutoPipeType) = new AutoPipePointer(itemType)
}

class AutoPipePointer(val itemType: AutoPipeType) extends AutoPipeType {
    private[scalapipe] override def create() =
        ValueType.create(this, () => new PointerValueType(this))
}

