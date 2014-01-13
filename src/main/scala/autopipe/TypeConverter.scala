
package autopipe

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
        val apb = value.apb
        (value.valueType, to) match {
            case (a: IntegerValueType, b: IntegerValueType) =>
                IntLiteral(b, value.long, apb)
            case (a: IntegerValueType, b: FloatValueType) =>
                FloatLiteral(b, value.double, apb)
            case (a: IntegerValueType, b: FixedValueType) =>
                IntLiteral(b, value.long << b.fraction, apb)
            case (a: FixedValueType, b: IntegerValueType) =>
                IntLiteral(b, value.long >> a.fraction, apb)
            case (a: FixedValueType, b: FloatValueType) =>
                FloatLiteral(b, value.double / (1L << a.fraction), apb)
            case (a: FixedValueType, b: FixedValueType) =>
                if (a.fraction > b.fraction) {
                    IntLiteral(b, value.long >> (a.fraction - b.fraction), apb)
                } else {
                    IntLiteral(b, value.long << (b.fraction - a.fraction), apb)
                }
            case (a: IntegerValueType, b: PointerValueType) =>
                IntLiteral(b, value.long, apb)
            case _ =>
                Error.raise("invalid (int) conversion from " +
                            value.valueType + " to " + to, value)
                value
        }
    }

    private def convertFloat(value: FloatLiteral,
                             to: ValueType): Literal = {
        val apb = value.apb
        (value.valueType, to) match {
            case (a: FloatValueType, b: FloatValueType) =>
                FloatLiteral(b, value.double, apb)
            case (a: FloatValueType, b: IntegerValueType) =>
                IntLiteral(b, value.long, apb)
            case (a: FloatValueType, b: FixedValueType) =>
                IntLiteral((value.double * (1L << b.fraction)).toLong, apb)
            case _ =>
                Error.raise("invalid (float) conversion from " +
                            value.valueType + " to " + to, value)
                value
        }
    }

}

