package scalapipe.gen

import scalapipe._

private[scalapipe] class HDLIRContext(val kt: KernelType) extends IRContext {

    def eliminateVariables = true

    val minRamBits = 1024
    val ramWidth = 32

    private val share: Boolean = kt.parameters.get[Int]('share) > 1

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
        case (ia: IRStore, ib: IRStore) => ia.dest == ib.dest ||
            (!ia.dest.valueType.flat && !ib.dest.valueType.flat)
        case (ia: IRStore, ib: IRLoad)  => ia.dest == ib.src ||
            (!ia.dest.valueType.flat && !ib.src.valueType.flat)
        case (ia: IRLoad,  ib: IRStore) => ia.src  == ib.dest ||
            (!ia.src.valueType.flat && !ib.dest.valueType.flat)
        case (ia: IRLoad,  ib: IRLoad)  => ia.src  == ib.src ||
            (!ia.src.valueType.flat && !ib.src.valueType.flat)
        case _ => false
    }

}
