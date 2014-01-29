
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
            state.nodes.foldLeft(0) { (a, n) =>
                math.max(a, getNodeTime(n))
            }
        }
    }

    private def getInstructionTime(node: IRInstruction): Int = node.op match {
        case NodeType.add | NodeType.sub    =>
            if (node.dest.valueType.isInstanceOf[FloatValueType]) {
                3
            } else {
                1
            }
        case NodeType.mul     =>
            if (node.dest.valueType.isInstanceOf[FloatValueType] &&
                !node.srca.isInstanceOf[ImmediateSymbol]) {
                2 + (node.srca.valueType.bits + multSize - 1) / multSize
            } else {
                1
            }
        case NodeType.div     => node.srca.valueType.bits
        case NodeType.mod     => node.srca.valueType.bits
        case NodeType.sqrt    => node.srca.valueType.bits
        case _                    => 1
    }

    private def getNodeTime(node: IRNode): Int = node match {
        case start: IRStart         => 0  // We initialize to 1.
        case noop:  IRNoOp          => 1
        case instr: IRInstruction   => getInstructionTime(instr)
        case st:    IRStore         => 1    // TODO
        case ld:    IRLoad          => 2    // TODO
        case goto:  IRGoto          => 1
        case stop:  IRStop          => 1
        case ret:   IRReturn        => 1
        case cond:  IRConditional   => 1
        case sw:    IRSwitch        => 2
        case phi:   IRPhi           => 1
        case call:  IRCall          => 0  // TODO
    }

}
