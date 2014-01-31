package scalapipe.dsl

import scalapipe._

class Struct extends Type {

    private[scalapipe] var fields = Seq[(Symbol, Type)]()

    /** Declare a field in this structure.
     * @param n The name of the field.
     * @param t The type of the field.
     */
    def field(n: Symbol, t: Type) {
        fields = fields :+ (n -> t)
    }

    private[scalapipe] override def create = {
        ValueType.create(this, () => new StructValueType(this))
    }

}
