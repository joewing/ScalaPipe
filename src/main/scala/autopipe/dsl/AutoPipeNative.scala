
package autopipe.dsl

import scala.reflect._
import autopipe._
import java.nio.ByteBuffer

object AutoPipeNative {
    def apply(name: String) = new AutoPipeNative(name)
}

class AutoPipeNative(_name: String) extends AutoPipeType(_name) {

    type T = Unit
    val mt = classTag[T]

    private[autopipe] override def create() = {
        ValueType.create(this, () => new NativeValueType(this))
    }

    private[autopipe] override def read(buffer: ByteBuffer): T = ???

    private[autopipe] override def write(buffer: ByteBuffer, value: T) = ???

}


