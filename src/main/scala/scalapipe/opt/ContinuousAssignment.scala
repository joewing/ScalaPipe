package scalapipe.opt

import scalapipe._

private[opt] object ContinuousAssignment extends Pass {

    override def toString = "continuous assignment"

    def run(context: IRContext, graph: IRGraph): IRGraph = {

        println("\tInserting continuous assignments")

        // Max cost to allow.
        val maxCost = 10

        // Compute reaching definitions.
        val defs = ReachingDefs.solve(context.kt, graph)

        // Function to compute the max path cost.
        def computeCost(g: IRGraph): Int = {
            g.blocks.view.map(_.label).foldLeft(0) { (a, b) =>
                math.max(a, getPathCost(g, b, defs))
            }
        }

        // Make all blocks that can be made continuous continuous unless
        // the path cost becomes too high.
        val newGraph = graph.blocks.map(_.label).foldLeft(graph) { (g, label) =>
            val sb = g.block(label)
            val otherWrites = g.blocks.exists { o =>
                o != sb && !o.dests.intersect(sb.dests).isEmpty
            }
            if (!otherWrites && continuous(g, sb)) {
                val cg = g.update(sb.copy(continuous = true))
                if (computeCost(cg) > maxCost) g else cg
            } else {
                g
            }
        }

        val cost = computeCost(newGraph)
        println("\t\tMax path cost: " + cost)

        return newGraph

    }

    /** Count the ones in a literal. */
    private def countOnes(node: BaseSymbol): Int = node match {
        case im: ImmediateSymbol =>
            def count(v: Long, a: Int): Int = {
                if (v == 0) {
                    a
                } else {
                    count(v << 1, a + (if (v < 0) 1 else 0))
                }
            }
            count(im.value.long, 0)
        case _ => 0
    }

    /** Determine the path cost of an operator. */
    private def getCost(node: IRNode): Int = node match {
        case phi: IRPhi             => 1
        case in: IRInstruction      => in.op match {
            case NodeType.assign    => 0
            case NodeType.convert   => 3
            case NodeType.neg       => 2
            case NodeType.not       => 1
            case NodeType.compl     => 1
            case NodeType.land      => 2
            case NodeType.lor       => 2
            case NodeType.and       => 1
            case NodeType.or        => 1
            case NodeType.xor       => 1
            case NodeType.shr       => 1
            case NodeType.shl       => 1
            case NodeType.add       => 2
            case NodeType.sub       => 2
            case NodeType.eq        => 2
            case NodeType.ne        => 2
            case NodeType.gt        => 2
            case NodeType.lt        => 2
            case NodeType.ge        => 2
            case NodeType.le        => 2
            case NodeType.abs       => 2
            case NodeType.mul       => math.max(5, log2(countOnes(in.srca)))
            case NodeType.div       => 6
            case NodeType.mod       => 6
            case NodeType.sqrt      => 6
            case _                  => 0
        }
        case _ => 0
    }

    type REACHING = Map[Int, Set[(BaseSymbol, Int)]]

    private def getPathCost(graph: IRGraph, label: Int, defs: REACHING): Int = {

        def getPathCost(l: Int, sym: BaseSymbol): Int = {
            if (l == label) {
                1000000
            } else {
                val nodes = graph.block(l).nodes.filter(_.dests.contains(sym))
                val srcs = nodes.flatMap(_.srcs)
                val srcDefs = defs(l).filter { case (s, b) =>
                    srcs.contains(s) && graph.block(b).continuous
                }
                val srcCost = srcDefs.foldLeft(0) { (a, sb) =>
                    math.max(a, getPathCost(sb._2, sb._1))
                }
                val localCost = nodes.foldLeft(0) { (a, n) =>
                    math.max(a, getCost(n))
                }
                srcCost + localCost
            }
        }

        graph.block(label).nodes.foldLeft(0) { (a, node) =>
            val srcDefs = defs(label).filter { case (s, b) =>
                node.srcs.contains(s) && graph.block(b).continuous
            }
            val srcCost = srcDefs.foldLeft(0) { (a, sb) =>
                math.max(a, getPathCost(sb._2, sb._1))
            }
            val cost = srcCost + getCost(node)
            math.max(a, cost)
        }

    }

    private def usesPort(node: IRNode): Boolean =
        node.symbols.exists(s => s.isInstanceOf[PortSymbol])

    private def writesPort(node: IRNode): Boolean =
        node.dests.exists(s => s.isInstanceOf[PortSymbol])

    private def continuousFloat(in: IRInstruction) = in.op match {
        case NodeType.avail     => !writesPort(in)
        case NodeType.convert   => false
        case NodeType.add       => false
        case NodeType.sub       => false
        case NodeType.mul       => false
        case NodeType.div       => false
        case NodeType.sqrt      => false
        case _                  => !usesPort(in)
    }

    private def continuousInt(in: IRInstruction) = in.op match {
        case NodeType.avail     => !writesPort(in)
        case NodeType.convert   =>
            in.srca.valueType.isInstanceOf[IntegerValueType]
        case NodeType.mul       => false
        case NodeType.div       => false
        case NodeType.mod       => false
        case NodeType.sqrt      => false
        case _                  => !usesPort(in)
    }

    /** Determine if an IRNode can be implemented as an assign statement. */
    private def continuous(node: IRNode): Boolean = node match {
        case in: IRInstruction =>
            in.dest.valueType match {
                case ft: FloatValueType => continuousFloat(in)
                case _                  => continuousInt(in)
            }
        case ld: IRLoad     => ld.src.valueType.flat
        case st: IRStore    => st.dest.valueType.flat
        case gt: IRGoto     => true
        case _              => false
    }

    /** Determine if a block can be implemented as an assign statement. */
    private def continuous(g: IRGraph, sb: StateBlock): Boolean = {
        sb.nodes.forall(n => continuous(n)) && sb.label != 0 &&
        !g.links(sb).exists(l => l.nodes.exists(n => n.isInstanceOf[IRPhi]))
    }

}
