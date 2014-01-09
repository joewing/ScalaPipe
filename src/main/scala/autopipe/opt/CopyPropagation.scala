
package autopipe.opt

import autopipe._

private[opt] object CopyPropagation extends Pass {

    override def toString = "copy propagation"

    def run(context: IRContext, graph: IRGraph): IRGraph = {
        println("\tPropagating copies")
        eliminate(context, propagate(context, graph))
    }

    private def eliminate(context: IRContext, graph: IRGraph): IRGraph = {

        val selfAssigns = graph.nodes.filter { n =>
            n match {
                case in: IRInstruction if in.op == NodeType.assign =>
                    in.dest == in.srca
                case _ => false
            }
        }

        selfAssigns.foldLeft(graph) { (ng, n) =>
            println("\t\tRemoving self-assignment: " + n)
            ng.remove(n)
        }

    }

    private def propagate(context: IRContext, graph: IRGraph): IRGraph = {

        // Get reaching definitions.
        // Note that reaching contains a mapping from block to a set of
        // definition pairs (symbol, block).
        val reaching = ReachingDefs.solve(context.co, graph)

        // Compute live ranges.
        // Mapping from block to a set of symbols.
        val live = LiveVariables.solve(context.co, graph)

        // Get the set of definitions reaching this block that we use.
        def getDefs(node: IRNode): Set[(BaseSymbol, Int)] = {
            val label = graph.block(node).label
            reaching(label).filter { case (s, r) =>
                node.srcs.contains(s)
            }
        }

        // Get a list of blocks that use a definition.
        def getUses(sym: BaseSymbol, sb: Int): Seq[StateBlock] = {
            graph.blocks.filter { b =>
                b.srcs.contains(sym) &&
                reaching(b.label).exists { case (s, r) =>
                    r == sb && s == sym
                }
            }
        }

        // Find an assignment to be eliminated.
        val assignments = graph.nodes.collect {
            case in: IRInstruction if in.op == NodeType.assign => in
        }
        val updates = assignments.filter { n =>
            val defs = getDefs(n)
            val nblock = graph.block(n)
            !n.symbols.exists(s => s.isInstanceOf[PortSymbol]) &&
            defs.size == 1 &&
            getUses(defs.head._1, defs.head._2).size == 1 &&
            !live(defs.head._2).contains(n.dest)
        }

        updates.headOption match {
            case Some(n) =>
                val d = getDefs(n).head
                assert(d._1 == n.srca)
                val b = graph.block(d._2)
                val toReplace = b.nodes.filter(_.dests.contains(n.srcs.head))
                val updatedGraph = toReplace.foldLeft(graph) { (ng, dn) =>
                    ng.replace(dn, dn.setDest(n.dest))
                }
                propagate(context, updatedGraph.remove(n))
            case None => graph
        }

    }

}

