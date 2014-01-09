
package autopipe.dsl

import autopipe.{ArrayValueType, ValueType}
import java.nio.ByteBuffer

object AutoPipeArray {
    def apply(itemType: AutoPipeType, length: Int) =
        new AutoPipeArray(itemType, length)
}

class AutoPipeArray(val itemType: AutoPipeType, val length: Int)
    extends AutoPipeType {

    type T = Array[itemType.T]
    val mt = itemType.mt.wrap

    private[autopipe] override def create() = 
        ValueType.create(this, () => new ArrayValueType(this))

    private[autopipe] override def read(buffer: ByteBuffer): T = {
        val result = itemType.mt.newArray(length)
        for (i <- 0 until length) {
            result(i) = itemType.read(buffer)
        }
        result
    }

    private[autopipe] override def write(buffer: ByteBuffer, value: T) {
        for (item <- value) {
            itemType.write(buffer, item)
        }
    }

}

