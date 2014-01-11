 
package autopipe.dsl

import scala.collection.mutable.HashMap

import autopipe.{UnionValueType, ValueType}

object AutoPipeUnion {
    def apply() = new AutoPipeUnion
}

class AutoPipeUnion extends AutoPipeType {

    private[autopipe] val fields = new HashMap[Symbol, AutoPipeType]

    def field(n: Symbol, t: AutoPipeType) = {
        fields += (n -> t)
    }

    private[autopipe] override def create() =
        ValueType.create(this, () => new UnionValueType(this))

}

