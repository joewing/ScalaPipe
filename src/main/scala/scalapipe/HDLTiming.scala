package scalapipe

import scala.collection.immutable.HashMap

private[scalapipe] object HDLTiming {

    // Number of bits that can be multiplied at a time.
    private val multSize = 18

    def computeAST(graph: IRGraph): Map[ASTNode, Int] = {
        val ir = computeIR(graph)
        val groups = ir.filter(_._2 > 0).groupBy { case (k, v) => k.ast }
        groups.mapValues { vs => vs.foldLeft(0) { (a, v) => a + v._2 } }
    }

    def computeIR(graph: IRGraph): Map[StateBlock, Int] = {
        HashMap(graph.blocks.map(b => (b, getStateTime(b))): _*)
    }

    def getStateTime(state: StateBlock): Int = {
        if (state.continuous) {
            0
        } else {
            state.nodes.map(getNodeTime(_)).max
        }
    }

    private def getIntInstrTime(node: IRInstruction): Int = node.op match {
        case NodeType.convert =>
            if (node.srca.valueType.isInstanceOf[FloatValueType]) {
                3
            } else {
                1
            }
        case NodeType.mul   =>
            1 + (node.srca.valueType.bits + multSize - 1) / multSize
        case NodeType.div   => 1 + node.srca.valueType.bits
        case NodeType.mod   => 1 + node.srca.valueType.bits
        case NodeType.sqrt  => 1 + node.srca.valueType.bits
        case _              => 1
    }

    private def getFloatInstrTime(node: IRInstruction): Int = node.op match {
        case NodeType.convert => 3
        case NodeType.add | NodeType.sub => 3
        case NodeType.mul =>
            1 + (node.srca.valueType.bits + multSize - 1) / multSize
        case NodeType.div   => 1 + node.srca.valueType.bits
        case NodeType.mod   => 1 + node.srca.valueType.bits
        case NodeType.sqrt  => 1 + node.srca.valueType.bits
        case _              => 1
    }

    private def getInstrTime(node: IRInstruction): Int = {
        node.dest.valueType match {
            case ft: FloatValueType => getFloatInstrTime(node)
            case _                  => getIntInstrTime(node)
        }
    }

    private def getLoadTime(node: IRLoad): Int = {
        if (node.src.valueType.flat) {
            1
        } else {
            2
        }
    }

    private def getStoreTime(node: IRStore): Int = 1

    private def getNodeTime(node: IRNode): Int = node match {
        case start: IRStart         => 0  // We initialize to 1.
        case noop:  IRNoOp          => 1
        case instr: IRInstruction   => getInstrTime(instr)
        case st:    IRStore         => getStoreTime(st)
        case ld:    IRLoad          => getLoadTime(ld)
        case goto:  IRGoto          => 1
        case stop:  IRStop          => 1
        case ret:   IRReturn        => 1
        case cond:  IRConditional   => 1
        case sw:    IRSwitch        => 2
        case phi:   IRPhi           => 1
        case call:  IRCall          => 1  // TODO
    }

}
