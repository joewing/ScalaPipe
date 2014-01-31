package scalapipe.opt

import scalapipe._

private[opt] object ExpandExpressions extends Pass {

    override def toString = "expand expressions"

    def run(context: IRContext, graph: IRGraph): IRGraph = {

        println("\tExpanding expressions")

        // Extract basic blocks.
        val basicBlocks = getStateBlocks(graph)
        val updatedGraph = basicBlocks.foldLeft(graph) { (ng, bb) =>

            // Get a list of all sources used in this basic block that we
            // can rename.
            val srcs = bb.foldLeft(Seq[BaseSymbol]()) { (a, b) =>
                a ++ ng.block(b).srcs.filter {
                    case st: StateSymbol    => st.valueType.flat
                    case _                  => false
                }
            }

            // Rename each symbol.
            srcs.foldLeft(ng) { (g, src) =>
                rename(context, g, src, bb)
            }

        }

        return updatedGraph

    }

    /** Rename a symbol in a basic block. */
    private def rename(context: IRContext,
                       g: IRGraph,
                       sym: BaseSymbol,
                       sb: Seq[Int]): IRGraph = {

        // Find the last index where the source is assigned.
        val uses = sb.filter { b =>
            g.block(b).symbols.contains(sym)
        }.zipWithIndex
        val assignments = uses.filter { case (b, i) =>
            g.block(b).dests.contains(sym)
        }
        if (assignments.size > 1) {
            val index = assignments.last._2

            // Get the assignment to change.
            val toModify = assignments.head
            val b = toModify._1
            val i = toModify._2

            // Create temporary for the destination.
            val temp = context.kt.createTemp(sym.valueType)

            // Update the destination.
            // The updated graph is called updatedDests.
            val toUpdate = g.block(b).nodes.filter(_.dests.contains(sym))
            val updatedDests = toUpdate.foldLeft(g) { (ng, node) =>
                ng.replace(node, node.setDest(temp))
            }

            // Locate future uses and the next write to the variable.
            // Note that there must be a next write since we don't change the
            // last assignment.
            val remaining = uses.filter(_._2 > i)
            val nextWrite = uses.find { case (ob, oi) =>
                oi > i && g.block(ob).dests.contains(sym)
            }
            val nextWriteIndex = nextWrite.get._2

            // Update uses.
            // updatedSrcs will contain the updated graph.
            // nextWriteIndex now holds the index of the next write
            // or -1 if there are no more writes to this variable.
            val updatedSrcs = remaining.foldLeft(updatedDests) { (ng, o) =>
                val ob = o._1  // Block index
                val oi = o._2  // Write index
                if (oi <= nextWriteIndex || nextWriteIndex < 0) {
                    ng.update(ng.block(ob).replaceSources(sym, temp))
                } else {
                    ng
                }
            }

            // If there is another write to this symbol, continue.
            if (nextWriteIndex >= 0) {
                rename(context, updatedSrcs, sym, sb.drop(nextWriteIndex))
            } else {
                updatedSrcs
            }

        } else {
            g
        }

    }

}
