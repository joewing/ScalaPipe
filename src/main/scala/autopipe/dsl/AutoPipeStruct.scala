
package autopipe.dsl

import scala.reflect._
import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet

import autopipe._
import java.nio.ByteBuffer

object AutoPipeStruct {
    def apply() = new AutoPipeStruct
}

class AutoPipeStruct extends AutoPipeType {

    type T = Unit
    val mt = classTag[T]

    private[autopipe] val fields = new HashMap[Symbol, AutoPipeType]

    /** Declare a field in this structure.
     * @param n The name of the field.
     * @param t The type of the field.
     */
    def field(n: Symbol, t: AutoPipeType) {
        fields += (n -> t)
    }

    private[autopipe] override def create() = {
        ValueType.create(this, () => new StructValueType(this))
    }

    private[autopipe] override def read(buffer: ByteBuffer): T = ???

    private[autopipe] override def write(buffer: ByteBuffer, value: T) = ???

}

