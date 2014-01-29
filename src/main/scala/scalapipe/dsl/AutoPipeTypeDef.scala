
package scalapipe.dsl

import scalapipe.{ValueType, TypeDefValueType}

object AutoPipeTypeDef {
    def apply(value: String) = new AutoPipeTypeDef(value)
}

class AutoPipeTypeDef(val value: String) extends AutoPipeType {

    private[scalapipe] override def create() =
        ValueType.create(this, () => new TypeDefValueType(this))

}

