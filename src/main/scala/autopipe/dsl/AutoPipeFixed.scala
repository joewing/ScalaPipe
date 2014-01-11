
package autopipe.dsl

import autopipe.{ValueType, FixedValueType}

object AutoPipeFixed {
    def apply(bits: Int, fraction: Int) = new AutoPipeFixed(bits, fraction)
}

class AutoPipeFixed(val bits: Int, val fraction: Int)
    extends AutoPipeType("Q" + bits + "p" + fraction) {

    private[autopipe] override def create() = {
        ValueType.create(this, () => new FixedValueType(this))
    }

}

