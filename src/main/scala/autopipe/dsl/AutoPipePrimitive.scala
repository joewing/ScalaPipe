

package autopipe.dsl

import scala.reflect._
import autopipe.ValueType
import java.nio.ByteBuffer

abstract class AutoPipePrimitve[P : Manifest](val valueType: ValueType)
    extends AutoPipeType(valueType.name) {

    type T = P
    val mt = classTag[P]

    private[autopipe] def create(): ValueType = valueType

}

object UNSIGNED8 extends AutoPipePrimitve[Byte](ValueType.unsigned8) {

    private[autopipe] override def read(buffer: ByteBuffer): T = buffer.get()

    private[autopipe] override def write(buffer: ByteBuffer, value: T) {
        buffer.put(value)
    }

}

object SIGNED8 extends AutoPipePrimitve[Byte](ValueType.signed8) {

    private[autopipe] override def read(buffer: ByteBuffer): T = buffer.get()

    private[autopipe] override def write(buffer: ByteBuffer, value: T) {
        buffer.put(value)
    }

}

object UNSIGNED16 extends AutoPipePrimitve[Short](ValueType.unsigned16) {

    private[autopipe] override def read(buffer: ByteBuffer): T =
        buffer.getShort()

    private[autopipe] override def write(buffer: ByteBuffer, value: T) {
        buffer.putShort(value)
    }

}

object SIGNED16 extends AutoPipePrimitve[Short](ValueType.signed16) {

    private[autopipe] override def read(buffer: ByteBuffer): T =
        buffer.getShort()

    private[autopipe] override def write(buffer: ByteBuffer, value: T) {
        buffer.putShort(value)
    }

}

object UNSIGNED32 extends AutoPipePrimitve[Int](ValueType.unsigned32) {

    private[autopipe] override def read(buffer: ByteBuffer): T =
        buffer.getInt()

    private[autopipe] override def write(buffer: ByteBuffer, value: T) {
        buffer.putInt(value)
    }

}

object SIGNED32 extends AutoPipePrimitve[Int](ValueType.signed32) {

    private[autopipe] override def read(buffer: ByteBuffer): T =
        buffer.getInt()

    private[autopipe] override def write(buffer: ByteBuffer, value: T) {
        buffer.putInt(value)
    }

}

object UNSIGNED64 extends AutoPipePrimitve[Long](ValueType.unsigned64) {

    private[autopipe] override def read(buffer: ByteBuffer): T =
        buffer.getLong()

    private[autopipe] override def write(buffer: ByteBuffer, value: T) {
        buffer.putLong(value)
    }

}

object SIGNED64 extends AutoPipePrimitve[Long](ValueType.signed64) {

    private[autopipe] override def read(buffer: ByteBuffer): T =
        buffer.getLong()

    private[autopipe] override def write(buffer: ByteBuffer, value: T) {
        buffer.putLong(value)
    }

}

object FLOAT32 extends AutoPipePrimitve[Float](ValueType.float32) {

    private[autopipe] override def read(buffer: ByteBuffer): T =
        buffer.getFloat()

    private[autopipe] override def write(buffer: ByteBuffer, value: T) {
        buffer.putFloat(value)
    }

}

object FLOAT64 extends AutoPipePrimitve[Double](ValueType.float64) {

    private[autopipe] override def read(buffer: ByteBuffer): T =
        buffer.getDouble()

    private[autopipe] override def write(buffer: ByteBuffer, value: T) {
        buffer.putDouble(value)
    }

}

object FLOAT96 extends AutoPipePrimitve[Double](ValueType.float96) {

    private[autopipe] override def read(buffer: ByteBuffer): T =
        buffer.getDouble()

    private[autopipe] override def write(buffer: ByteBuffer, value: T) {
        buffer.putDouble(value)
    }

}

object BOOL extends AutoPipePrimitve[Boolean](ValueType.bool) {

    private[autopipe] override def read(buffer: ByteBuffer): T =
        buffer.get() != 0

    private[autopipe] override def write(buffer: ByteBuffer, value: T) {
        val temp: Byte = if (value) 0 else 1
        buffer.put(temp)
    }

}

object STRING extends AutoPipePrimitve[String](ValueType.string) {

    private[autopipe] override def read(buffer: ByteBuffer): T = ???

    private[autopipe] override def write(buffer: ByteBuffer, value: T) = ???

}

object ANY_TYPE extends AutoPipePrimitve[Any](ValueType.any) {

    private[autopipe] override def read(buffer: ByteBuffer): T = ???

    private[autopipe] override def write(buffer: ByteBuffer, value: T) = ???

}

object VOID extends AutoPipePrimitve[Unit](ValueType.void) {

    private[autopipe] override def read(buffer: ByteBuffer): T = ???

    private[autopipe] override def write(buffer: ByteBuffer, value: T) = ???

}

