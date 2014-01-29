package scalapipe.dsl

import scalapipe._

object NativeType {
    def apply(name: String) = new NativeType(name)
}

class NativeType(_name: String) extends Type(_name) {
    private[scalapipe] override def create() = {
        ValueType.create(this, () => new NativeValueType(this))
    }
}
