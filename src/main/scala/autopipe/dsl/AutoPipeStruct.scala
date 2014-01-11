
package autopipe.dsl

import scala.collection.mutable.HashMap

import autopipe._

object AutoPipeStruct {
    def apply() = new AutoPipeStruct
}

class AutoPipeStruct extends AutoPipeType {

    private[autopipe] val fields = new HashMap[Symbol, AutoPipeType]

    /** Declare a field in this structure.
     * @param n The name of the field.
     * @param t The type of the field.
     */
    def field(n: Symbol, t: AutoPipeType) {
        fields += (n -> t)
    }

    private[autopipe] override def create() = {
        ValueType.create(this, () => new StructValueType(this))
    }

}

