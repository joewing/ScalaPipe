
package autopipe.opt

import autopipe. _

private[opt] object StrengthReduction extends Pass {

    override def toString = "strength reduction"

    def run(context: IRContext, graph: IRGraph): IRGraph = {

        println("\tPerforming strength reduction")

        graph.blocks.foldLeft(graph) { (g, b) =>
            val nodes = b.nodes.collect { case i: IRInstruction => i }
            val reduced = nodes.map { n => (n, reduce(n)) }
            reduced.filter(_._2 != null).foldLeft(g) { (g2, update) =>
                println("\t\tReplacing " + update._1 + " with " + update._2)
                g2.replace(update._1, update._2)
            }
        }

    }

    // Sort arguments so the first argument is the immediate
    // if one of the arguments is an immediate.
    // Note that both arguments shouldn't be immediates due
    // to constant folding.
    private def sortArgs(node: IRInstruction): (BaseSymbol, BaseSymbol) = {
        if (node.srcb.isInstanceOf[ImmediateSymbol]) {
            assert(!node.srca.isInstanceOf[ImmediateSymbol])
            (node.srcb, node.srca)
        } else {
            (node.srca, node.srcb)
        }
    }

    private def reduceIntAdd(node: IRInstruction): IRInstruction = {
        val args = sortArgs(node)
        args._1 match {
            case im: ImmediateSymbol if im.value.long == 0 =>
                new IRInstruction(NodeType.assign, node.dest, args._2)
            case ts: TempSymbol if node.srca == node.srcb =>
                val lit = IntLiteral(ts.valueType, 1, null)
                val sym = new ImmediateSymbol(lit)
                new IRInstruction(NodeType.shl, node.dest, node.srca, sym)
            case ss: StateSymbol if node.srca == node.srcb =>
                val lit = IntLiteral(ss.valueType, 1, null)
                val sym = new ImmediateSymbol(lit)
                new IRInstruction(NodeType.shl, node.dest, node.srca, sym)
            case _ => null
        }
    }

    private def reduceFixedAdd(node: IRInstruction): IRInstruction = {
        val args = sortArgs(node)
        args._1 match {
            case im: ImmediateSymbol if im.value.long == 0 =>
                new IRInstruction(NodeType.assign, node.dest, args._2)
            case ts: TempSymbol if node.srca == node.srcb =>
                val lit = IntLiteral(ts.valueType, 1, null)
                val sym = new ImmediateSymbol(lit)
                new IRInstruction(NodeType.shl, node.dest, node.srca, sym)
            case ss: StateSymbol if node.srca == node.srcb =>
                val lit = IntLiteral(ss.valueType, 1, null)
                val sym = new ImmediateSymbol(lit)
                new IRInstruction(NodeType.shl, node.dest, node.srca, sym)
            case _ => null
        }
    }

    private def reduceFloatAdd(node: IRInstruction): IRInstruction = {
        val args = sortArgs(node)
        args._1 match {
            case im: ImmediateSymbol if im.value.double == 0.0 =>
                new IRInstruction(NodeType.assign, node.dest, args._2)
            case _ => null
        }
    }

    private def reduceAdd(node: IRInstruction): IRInstruction = {
        node.dest.valueType match {
            case it: IntegerValueType => reduceIntAdd(node)
            case ft: FixedValueType => reduceFixedAdd(node)
            case ft: FloatValueType => reduceFloatAdd(node)
            case _ => null
        }
    }

    private def reduceIntSub(node: IRInstruction): IRInstruction = {
        node.srcb match {
            case im: ImmediateSymbol if im.value.long == 0 =>
                new IRInstruction(NodeType.assign, node.dest, node.srca)
            case ts: TempSymbol if node.srca == node.srcb =>
                val lit = IntLiteral(ts.valueType, 0, null)
                val sym = new ImmediateSymbol(lit)
                new IRInstruction(NodeType.assign, node.dest, sym)
            case ss: StateSymbol if node.srca == node.srcb =>
                val lit = IntLiteral(ss.valueType, 0, null)
                val sym = new ImmediateSymbol(lit)
                new IRInstruction(NodeType.assign, node.dest, sym)
            case _ => null
        }
    }

    private def reduceFixedSub(node: IRInstruction): IRInstruction = {
        node.srcb match {
            case im: ImmediateSymbol if im.value.long == 0 =>
                new IRInstruction(NodeType.assign, node.dest, node.srca)
            case ts: TempSymbol if node.srca == node.srcb =>
                val lit = IntLiteral(ts.valueType, 0, null)
                val sym = new ImmediateSymbol(lit)
                new IRInstruction(NodeType.assign, node.dest, sym)
            case ss: StateSymbol if node.srca == node.srcb =>
                val lit = IntLiteral(ss.valueType, 0, null)
                val sym = new ImmediateSymbol(lit)
                new IRInstruction(NodeType.assign, node.dest, sym)
            case _ => null
        }
    }

    private def reduceFloatSub(node: IRInstruction): IRInstruction = {
        node.srcb match {
            case im: ImmediateSymbol if im.value.double == 0 =>
                new IRInstruction(NodeType.assign, node.dest, node.srca)
            case ts: TempSymbol if node.srca == node.srcb =>
                val lit = FloatLiteral(ts.valueType, 0.0, null)
                val sym = new ImmediateSymbol(lit)
                new IRInstruction(NodeType.assign, node.dest, sym)
            case ss: StateSymbol if node.srca == node.srcb =>
                val lit = FloatLiteral(ss.valueType, 0.0, null)
                val sym = new ImmediateSymbol(lit)
                new IRInstruction(NodeType.assign, node.dest, sym)
            case _ => null
        }
    }

    private def reduceSub(node: IRInstruction): IRInstruction = {
        node.dest.valueType match {
            case it: IntegerValueType => reduceIntSub(node)
            case ft: FixedValueType => reduceFixedSub(node)
            case ft: FloatValueType => reduceFloatSub(node)
            case _ => null
        }
    }

    private def reduceIntMul(node: IRInstruction): IRInstruction = {
        val args = sortArgs(node)
        args._1 match {
            case im: ImmediateSymbol if im.value.long == 0 =>
                    new IRInstruction(NodeType.assign, node.dest, args._1)
            case im: ImmediateSymbol if im.value.long == 1 =>
                    new IRInstruction(NodeType.assign, node.dest, args._2)
            case im: ImmediateSymbol if im.value.long == -1 =>
                    new IRInstruction(NodeType.neg, node.dest, args._2)
            case im: ImmediateSymbol =>
                val l = log2(im.value.long)
                if (im.value.long == (1 << l)) {
                    val lit = IntLiteral(im.valueType, l, null)
                    val sym = new ImmediateSymbol(lit)
                    new IRInstruction(NodeType.shl, node.dest, args._2, sym)
                } else {
                    null
                }
            case _ => null
        }
    }

    private def reduceFloatMul(node: IRInstruction): IRInstruction = {
        val args = sortArgs(node)
        args._1 match {
            case im: ImmediateSymbol =>
                if (im.value.double == 0.0) {
                    new IRInstruction(NodeType.assign, node.dest, args._1)
                } else if (im.value.double == 1.0) {
                    new IRInstruction(NodeType.assign, node.dest, args._2)
                } else if (im.value.double == -1.0) {
                    new IRInstruction(NodeType.neg, node.dest, args._2)
                } else {
                    null
                }
            case _ => null
        }
    }

    // Attempt to get a better multiply.
    private def reduceMul(node: IRInstruction): IRInstruction = {
        node.dest.valueType match {
            case it: IntegerValueType => reduceIntMul(node)
            case ft: FixedValueType => null // FIXME
            case ft: FloatValueType => reduceFloatMul(node)
            case _ => null
        }
    }

    private def reduceIntDiv(node: IRInstruction): IRInstruction = {
        node.srcb match {
            case im: ImmediateSymbol =>
                if (im.value.long == 1) {
                    new IRInstruction(NodeType.assign, node.dest, node.srca)
                } else if (im.value.long == -1) {
                    new IRInstruction(NodeType.neg, node.dest, node.srca)
                } else {
                    val l = log2(im.value.long)
                    if (im.value.long == (1 << l)) {
                        val lit = IntLiteral(im.valueType, l, null)
                        val sym = new ImmediateSymbol(lit)
                        new IRInstruction(NodeType.shr, node.dest,
                                          node.srca, sym)
                    } else {
                        null
                    }
                }
            case _ => null
        }
    }

    // Attempt to get a better divide.
    private def reduceDiv(node: IRInstruction): IRInstruction = {
        node.dest.valueType match {
            case it: IntegerValueType => reduceIntDiv(node)
            case ft: FixedValueType => null // FIXME
            case ft: FloatValueType => null // FIXME
            case _ => null
        }
    }

    // Get the improved version of an instruction, if one exists.
    private def reduce(node: IRInstruction): IRInstruction = node.op match {
        case NodeType.add => reduceAdd(node)
        case NodeType.sub => reduceSub(node)
        case NodeType.mul => reduceMul(node)
        case NodeType.div => reduceDiv(node)
        case _ => null
    }

}

