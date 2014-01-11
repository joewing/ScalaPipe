
package autopipe.dsl

import autopipe.{ArrayValueType, ValueType}

object AutoPipeArray {
    def apply(itemType: AutoPipeType, length: Int) =
        new AutoPipeArray(itemType, length)
}

class AutoPipeArray(val itemType: AutoPipeType, val length: Int)
    extends AutoPipeType {

    private[autopipe] override def create() = 
        ValueType.create(this, () => new ArrayValueType(this))

}

