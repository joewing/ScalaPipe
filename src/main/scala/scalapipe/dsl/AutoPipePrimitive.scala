

package scalapipe.dsl

import scalapipe.ValueType

abstract class AutoPipePrimitve(val valueType: ValueType)
    extends AutoPipeType(valueType.name) {

    private[scalapipe] def create(): ValueType = valueType

}

object UNSIGNED8 extends AutoPipePrimitve(ValueType.unsigned8)

object SIGNED8 extends AutoPipePrimitve(ValueType.signed8)

object UNSIGNED16 extends AutoPipePrimitve(ValueType.unsigned16)

object SIGNED16 extends AutoPipePrimitve(ValueType.signed16)

object UNSIGNED32 extends AutoPipePrimitve(ValueType.unsigned32)

object SIGNED32 extends AutoPipePrimitve(ValueType.signed32)

object UNSIGNED64 extends AutoPipePrimitve(ValueType.unsigned64)

object SIGNED64 extends AutoPipePrimitve(ValueType.signed64)

object FLOAT32 extends AutoPipePrimitve(ValueType.float32)

object FLOAT64 extends AutoPipePrimitve(ValueType.float64)

object FLOAT96 extends AutoPipePrimitve(ValueType.float96)

object BOOL extends AutoPipePrimitve(ValueType.bool)

object STRING extends AutoPipePrimitve(ValueType.string)

object ANY_TYPE extends AutoPipePrimitve(ValueType.any)

object VOID extends AutoPipePrimitve(ValueType.void)

