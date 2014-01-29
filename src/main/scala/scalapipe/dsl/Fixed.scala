package scalapipe.dsl

import scalapipe.{ValueType, FixedValueType}

object Fixed {
    def apply(bits: Int, fraction: Int) = new Fixed(bits, fraction)
}

class Fixed(
        val bits: Int, val fraction: Int
    ) extends Type("Q" + bits + "p" + fraction) {

    private[scalapipe] override def create = {
        ValueType.create(this, () => new FixedValueType(this))
    }

}
