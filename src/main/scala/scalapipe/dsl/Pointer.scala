package scalapipe.dsl

import scalapipe.{PointerValueType, ValueType}

object Pointer {
    def apply(itemType: Type) = new Pointer(itemType)
}

class Pointer(val itemType: Type) extends Type {
    private[scalapipe] override def create =
        ValueType.create(this, () => new PointerValueType(this))
}
