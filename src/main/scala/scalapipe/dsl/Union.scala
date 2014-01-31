package scalapipe.dsl

import scalapipe.{UnionValueType, ValueType, SymbolValidator}

class Union extends Type {

    private[scalapipe] var fields = Seq[(Symbol, Type)]()

    def field(n: Symbol, t: Type) {
        SymbolValidator.validate(n.name, this)
        fields = fields :+ (n -> t)
    }

    def field(n: String, t: Type) {
        field(Symbol(n), t)
    }

    private[scalapipe] override def create =
        ValueType.create(this, () => new UnionValueType(this))

}
