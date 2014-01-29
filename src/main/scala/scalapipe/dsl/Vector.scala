package scalapipe.dsl

import scalapipe.{ArrayValueType, ValueType}

object Vector {
    def apply(itemType: AutoPipeType, length: Int) =
        new Vector(itemType, length)
}

class Vector(
        val itemType: AutoPipeType,
        val length: Int
    ) extends AutoPipeType {

    private[scalapipe] override def create() = 
        ValueType.create(this, () => new ArrayValueType(this))

}
