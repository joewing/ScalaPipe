
package autopipe

object TypeConverter {

    def convert(value: Literal, to: ValueType): Literal = {
        value match {
            case i: IntLiteral    => convertInt(i, to)
            case f: FloatLiteral => convertFloat(f, to)
            case _ => Error.raise("invalid conversion", value)
        }
    }

    private def convertInt(value: IntLiteral, to: ValueType): Literal = {
        (value.valueType, to) match {
            case (a: IntegerValueType, b: IntegerValueType) =>
                new IntLiteral(b, value.long)
            case (a: IntegerValueType, b: FloatValueType) =>
                new FloatLiteral(b, value.double)
            case (a: IntegerValueType, b: FixedValueType) =>
                new IntLiteral(b, value.long << b.fraction)
            case (a: FixedValueType, b: IntegerValueType) =>
                new IntLiteral(b, value.long >> a.fraction)
            case (a: FixedValueType, b: FloatValueType) =>
                new FloatLiteral(b, value.double / (1L << a.fraction))
            case (a: FixedValueType, b: FixedValueType) =>
                if (a.fraction > b.fraction) {
                    new IntLiteral(b, value.long >> (a.fraction - b.fraction))
                } else {
                    new IntLiteral(b, value.long << (b.fraction - a.fraction))
                }
            case (a: IntegerValueType, b: PointerValueType) =>
                new IntLiteral(b, value.long)
            case _ =>
                Error.raise("invalid (int) conversion from " + value.valueType +
                                " to " + to, value)
        }
    }

    private def convertFloat(value: FloatLiteral, to: ValueType): Literal = {
        (value.valueType, to) match {
            case (a: FloatValueType, b: FloatValueType) =>
                new FloatLiteral(b, value.double)
            case (a: FloatValueType, b: IntegerValueType) =>
                new IntLiteral(b, value.long)
            case (a: FloatValueType, b: FixedValueType) =>
                new IntLiteral((value.double * (1L << b.fraction)).toLong)
            case _ =>
                Error.raise("invalid (float) conversion from " + value.valueType +
                                " to " + to, value)
        }
    }

}

