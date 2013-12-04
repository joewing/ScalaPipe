
package autopipe.dsl

import scala.reflect._
import autopipe.{ValueType, FixedValueType}
import java.nio.ByteBuffer

object AutoPipeFixed {
   def apply(bits: Int, fraction: Int) = new AutoPipeFixed(bits, fraction)
}

class AutoPipeFixed(val bits: Int, val fraction: Int)
   extends AutoPipeType("Q" + bits + "p" + fraction) {

   type T = Long
   val mt = classTag[T]

   private[autopipe] override def create() = {
      ValueType.create(this, () => new FixedValueType(this))
   }

   private[autopipe] override def read(buffer: ByteBuffer): T = ???

   private[autopipe] override def write(buffer: ByteBuffer, value: T) = ???

}

