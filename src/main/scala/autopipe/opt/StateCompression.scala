
package autopipe.opt

import autopipe._

private[opt] object StateCompression extends Pass {

    override def toString = "state compression"

    def run(context: IRContext, graph: IRGraph): IRGraph = {
        println("\tCompressing state space")
        compress(context, graph)
    }

    private def compress(context: IRContext, graph: IRGraph): IRGraph = {

        // Compute the dominator tree.
        val dom = new Dominators(context.kt, graph)
        val pdom = new PostDominators(context.kt, graph)

        // Get a mapping from node to nodes that are runnable at that state.
        val runnable = RunnableExpressions.solve(context.kt, graph)

        // Get a list of nodes that can be combined into another state.
        val canCombine = graph.blocks.view.flatMap { sb =>
            val other = runnable(sb.label).find { o =>
                val oblock = graph.block(o)
                !sb.continuous && !oblock.continuous &&
                sb != oblock && sb.label != 0 &&
                !context.share(sb, o) &&
                pdom.dominates(oblock, sb) &&
                dom.dominates(sb, oblock)
            }
            other.map(o => (sb, o))
        }

        canCombine.headOption match {
            case Some((sb, other)) => compress(context, graph.move(other, sb))
            case None => graph
        }

    }

}

