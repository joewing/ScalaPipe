package scalapipe.dsl

import scalapipe.{StructValueType, ValueType}

class Struct extends Type {
    private[scalapipe] override def create = {
        ValueType.create(this, () => new StructValueType(this))
    }
}
