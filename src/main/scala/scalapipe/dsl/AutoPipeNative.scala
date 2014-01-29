
package scalapipe.dsl

import scalapipe._

object AutoPipeNative {
    def apply(name: String) = new AutoPipeNative(name)
}

class AutoPipeNative(_name: String) extends AutoPipeType(_name) {
    private[scalapipe] override def create() = {
        ValueType.create(this, () => new NativeValueType(this))
    }
}


