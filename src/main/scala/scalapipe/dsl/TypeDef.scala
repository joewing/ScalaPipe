package scalapipe.dsl

import scalapipe.{ValueType, TypeDefValueType}

object TypeDef {
    def apply(value: String) = new TypeDef(value)
}

class TypeDef(val value: String) extends Type {

    private[scalapipe] override def create =
        ValueType.create(this, () => new TypeDefValueType(this))

}
