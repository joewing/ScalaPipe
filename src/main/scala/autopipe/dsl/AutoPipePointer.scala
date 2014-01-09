
package autopipe.dsl

import scala.reflect._
import autopipe.{PointerValueType, ValueType}
import java.nio.ByteBuffer

object AutoPipePointer {
    def apply(itemType: AutoPipeType) = new AutoPipePointer(itemType)
}

class AutoPipePointer(val itemType: AutoPipeType) extends AutoPipeType {

    type T = Unit
    val mt = classTag[T]

    private[autopipe] override def create() =
        ValueType.create(this, () => new PointerValueType(this))

    private[autopipe] override def read(buffer: ByteBuffer): T = ???

    private[autopipe] override def write(buffer: ByteBuffer, value: T) = ???

}

