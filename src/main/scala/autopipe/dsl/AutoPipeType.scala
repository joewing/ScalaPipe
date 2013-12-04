
package autopipe.dsl

import scala.reflect._
import autopipe.{LabelMaker, ValueType}
import java.nio.ByteBuffer

abstract class AutoPipeType(val name: String) {

   type T
   val mt: ClassTag[T]

   def this() = this(LabelMaker.getTypeLabel)

   override def toString = name

   private[autopipe] def create(): ValueType

   private[autopipe] def read(buffer: ByteBuffer): T

   private[autopipe] def write(buffer: ByteBuffer, value: T): Unit

}

