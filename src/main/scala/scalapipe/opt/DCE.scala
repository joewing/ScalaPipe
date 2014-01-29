package scalapipe.opt

import scalapipe._

private[opt] object DCE extends Pass {

    override def toString = "DCE"

    def run(context: IRContext, graph: IRGraph): IRGraph = {

        println("\tEliminating dead code")

        // Determine what code is reachable.
        val reachable = getConnectedBlocks(graph)

        // Determine which blocks to remove.
        val toRemove = graph.blocks.map(_.label).filter(!reachable.contains(_))

        // Remove the blocks.
        toRemove.foldLeft(graph) { (g, sb) =>
            println("\t\tRemoving " + sb);
            g.remove(g.block(sb))
        }

    }

    /** Get connected blocks. */
    private def getConnectedBlocks(graph: IRGraph): Set[Int] = {

        def helper(block: Int, visited: Set[Int]): Set[Int] = {
            graph.links(block).map(_.label).foldLeft(visited) { (a, b) =>
                if (a.contains(b)) {
                    a
                } else {
                    helper(b, a + b)
                }
            }
        }

        helper(graph.root.label, Set(graph.root.label))

    }

}
