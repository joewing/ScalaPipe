
package autopipe.gen

import autopipe._

private[autopipe] class HDLIRContext(val co: CodeObject) extends IRContext {

    def eliminateVariables = true

    private val share: Boolean = co.parameters.get[Int]('share) > 1

    def share(a: IRNode, b: IRNode): Boolean = (a, b) match {
        case (ia: IRInstruction, ib: IRInstruction) =>
            (ia.dest.valueType, ib.dest.valueType) match {
                case (fa: FloatValueType, fb: FloatValueType) if share =>
                    ia.op == ib.op
                case (ta: IntegerValueType, tb: IntegerValueType) if share =>
                    if (ia.op == ib.op) {
                        ia.op match {
                            case NodeType.mul =>
                                !ia.srca.isInstanceOf[ImmediateSymbol] &&
                                !ib.srca.isInstanceOf[ImmediateSymbol]
                            case NodeType.div => true
                            case NodeType.sqrt => true
                            case _ => false
                        }
                    } else {
                        false
                    }
                case (ta: FixedValueType, tb: FixedValueType) if share =>
                    if (ia.op == ib.op) {
                        ia.op match {
                            case NodeType.mul =>
                                !ia.srca.isInstanceOf[ImmediateSymbol] &&
                                !ib.srca.isInstanceOf[ImmediateSymbol]
                            case NodeType.div => true
                            case NodeType.sqrt => true
                            case _ => false
                        }
                    } else {
                        false
                    }
                case _ => false
            }
        case (ia: IRArrayStore, ib: IRArrayStore) => ia.dest == ib.dest
        case (ia: IRArrayStore, ib: IRArrayLoad)  => ia.dest == ib.src
        case (ia: IRArrayLoad,  ib: IRArrayStore) => ia.src  == ib.dest
        case (ia: IRArrayLoad,  ib: IRArrayLoad)  => ia.src  == ib.src
        case _ => false
    }

}

