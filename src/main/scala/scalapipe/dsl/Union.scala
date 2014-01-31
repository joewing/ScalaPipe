package scalapipe.dsl

import scalapipe.{UnionValueType, ValueType}

class Union extends Type {

    private[scalapipe] var fields = Seq[(Symbol, Type)]()

    def field(n: Symbol, t: Type) = {
        fields = fields :+ (n -> t)
    }

    private[scalapipe] override def create =
        ValueType.create(this, () => new UnionValueType(this))

}
