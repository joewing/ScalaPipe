package scalapipe.dsl

import scalapipe.{ArrayValueType, ValueType}

object Vector {
    def apply(itemType: Type, length: Int) =
        new Vector(itemType, length)
}

class Vector(val itemType: Type, val length: Int) extends Type {

    private[scalapipe] override def create = 
        ValueType.create(this, () => new ArrayValueType(this))

}
