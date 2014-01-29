 
package scalapipe.dsl

import scala.collection.mutable.HashMap

import scalapipe.{UnionValueType, ValueType}

object AutoPipeUnion {
    def apply() = new AutoPipeUnion
}

class AutoPipeUnion extends AutoPipeType {

    private[scalapipe] val fields = new HashMap[Symbol, AutoPipeType]

    def field(n: Symbol, t: AutoPipeType) = {
        fields += (n -> t)
    }

    private[scalapipe] override def create() =
        ValueType.create(this, () => new UnionValueType(this))

}

