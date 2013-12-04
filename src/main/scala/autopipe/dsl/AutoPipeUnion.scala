 
package autopipe.dsl

import scala.collection.mutable.HashMap

import scala.reflect._
import autopipe.{UnionValueType, ValueType}
import java.nio.ByteBuffer

object AutoPipeUnion {
   def apply() = new AutoPipeUnion
}

class AutoPipeUnion extends AutoPipeType {

   type T = Unit
   val mt = classTag[T]

   private[autopipe] val fields = new HashMap[Symbol, AutoPipeType]

   def field(n: Symbol, t: AutoPipeType) = {
      fields += (n -> t)
   }

   private[autopipe] override def create() =
      ValueType.create(this, () => new UnionValueType(this))

   private[autopipe] override def read(buffer: ByteBuffer): T = ???

   private[autopipe] override def write(buffer: ByteBuffer, value: T) = ???

}

