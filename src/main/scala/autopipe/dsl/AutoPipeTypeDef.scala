
package autopipe.dsl

import scala.reflect._
import autopipe.{ValueType, TypeDefValueType}
import java.nio.ByteBuffer

object AutoPipeTypeDef {
    def apply(value: String) = new AutoPipeTypeDef(value)
}

class AutoPipeTypeDef(val value: String) extends AutoPipeType {

    type T = Unit
    val mt = classTag[T]

    private[autopipe] override def create() =
        ValueType.create(this, () => new TypeDefValueType(this))

    private[autopipe] override def read(buffer: ByteBuffer): T = ???

    private[autopipe] override def write(buffer: ByteBuffer, value: T) = ???

}

