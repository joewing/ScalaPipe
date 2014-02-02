package scalapipe.dsl

import scalapipe.{UnionValueType, ValueType}

class Union extends Type {
    private[scalapipe] override def create = {
        ValueType.create(this, () => new UnionValueType(this))
    }
}
