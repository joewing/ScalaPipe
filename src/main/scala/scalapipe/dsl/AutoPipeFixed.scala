
package scalapipe.dsl

import scalapipe.{ValueType, FixedValueType}

object AutoPipeFixed {
    def apply(bits: Int, fraction: Int) = new AutoPipeFixed(bits, fraction)
}

class AutoPipeFixed(val bits: Int, val fraction: Int)
    extends AutoPipeType("Q" + bits + "p" + fraction) {

    private[scalapipe] override def create() = {
        ValueType.create(this, () => new FixedValueType(this))
    }

}

