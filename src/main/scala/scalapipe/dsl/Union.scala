package scalapipe.dsl

import scalapipe.{UnionValueType, ValueType}

class Union extends Type {

    private[scalapipe] var fields = Map[Symbol, Type]()

    def field(n: Symbol, t: Type) = {
        fields += (n -> t)
    }

    private[scalapipe] override def create =
        ValueType.create(this, () => new UnionValueType(this))

}
