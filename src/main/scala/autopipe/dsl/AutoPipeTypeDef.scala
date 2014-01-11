
package autopipe.dsl

import autopipe.{ValueType, TypeDefValueType}

object AutoPipeTypeDef {
    def apply(value: String) = new AutoPipeTypeDef(value)
}

class AutoPipeTypeDef(val value: String) extends AutoPipeType {

    private[autopipe] override def create() =
        ValueType.create(this, () => new TypeDefValueType(this))

}

