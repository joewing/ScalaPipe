package scalapipe.dsl

import scalapipe.ValueType

abstract class PrimitiveType(
        private[scalapipe] val valueType: ValueType
    ) extends Type(valueType.name) {

    private[scalapipe] def create: ValueType = valueType

}

object UNSIGNED8 extends PrimitiveType(ValueType.unsigned8)

object SIGNED8 extends PrimitiveType(ValueType.signed8)

object UNSIGNED16 extends PrimitiveType(ValueType.unsigned16)

object SIGNED16 extends PrimitiveType(ValueType.signed16)

object UNSIGNED32 extends PrimitiveType(ValueType.unsigned32)

object SIGNED32 extends PrimitiveType(ValueType.signed32)

object UNSIGNED64 extends PrimitiveType(ValueType.unsigned64)

object SIGNED64 extends PrimitiveType(ValueType.signed64)

object FLOAT32 extends PrimitiveType(ValueType.float32)

object FLOAT64 extends PrimitiveType(ValueType.float64)

object FLOAT96 extends PrimitiveType(ValueType.float96)

object BOOL extends PrimitiveType(ValueType.bool)

object STRING extends PrimitiveType(ValueType.string)

object ANY_TYPE extends PrimitiveType(ValueType.any)

object VOID extends PrimitiveType(ValueType.void)
