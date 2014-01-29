package scalapipe

object TypeConverter {

    def convert(value: Literal, to: ValueType): Literal = {
        value match {
            case i: IntLiteral      => convertInt(i, to)
            case f: FloatLiteral    => convertFloat(f, to)
            case _ =>
                Error.raise("invalid conversion", value)
                value
        }
    }

    private def convertInt(value: IntLiteral,
                           to: ValueType): Literal = {
        val kernel = value.kernel
        (value.valueType, to) match {
            case (a: IntegerValueType, b: IntegerValueType) =>
                IntLiteral(b, value.long, kernel)
            case (a: IntegerValueType, b: FloatValueType) =>
                FloatLiteral(b, value.double, kernel)
            case (a: IntegerValueType, b: FixedValueType) =>
                IntLiteral(b, value.long << b.fraction, kernel)
            case (a: FixedValueType, b: IntegerValueType) =>
                IntLiteral(b, value.long >> a.fraction, kernel)
            case (a: FixedValueType, b: FloatValueType) =>
                FloatLiteral(b, value.double / (1L << a.fraction), kernel)
            case (a: FixedValueType, b: FixedValueType) =>
                val result = if (a.fraction > b.fraction) {
                    value.long >> (a.fraction - b.fraction)
                } else {
                    value.long << (b.fraction - a.fraction)
                }
                IntLiteral(b, result, kernel)
            case (a: IntegerValueType, b: PointerValueType) =>
                IntLiteral(b, value.long, kernel)
            case _ =>
                Error.raise("invalid (int) conversion from " +
                            value.valueType + " to " + to, value)
                value
        }
    }

    private def convertFloat(value: FloatLiteral,
                             to: ValueType): Literal = {
        val kernel = value.kernel
        (value.valueType, to) match {
            case (a: FloatValueType, b: FloatValueType) =>
                FloatLiteral(b, value.double, kernel)
            case (a: FloatValueType, b: IntegerValueType) =>
                IntLiteral(b, value.long, kernel)
            case (a: FloatValueType, b: FixedValueType) =>
                IntLiteral((value.double * (1L << b.fraction)).toLong, kernel)
            case _ =>
                Error.raise("invalid (float) conversion from " +
                            value.valueType + " to " + to, value)
                value
        }
    }

}
