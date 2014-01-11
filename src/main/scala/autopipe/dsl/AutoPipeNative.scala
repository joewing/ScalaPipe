
package autopipe.dsl

import autopipe._

object AutoPipeNative {
    def apply(name: String) = new AutoPipeNative(name)
}

class AutoPipeNative(_name: String) extends AutoPipeType(_name) {
    private[autopipe] override def create() = {
        ValueType.create(this, () => new NativeValueType(this))
    }
}


