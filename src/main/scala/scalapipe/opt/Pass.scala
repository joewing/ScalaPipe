package scalapipe.opt

import scalapipe._

private[opt] abstract class Pass {

    def run(context: IRContext, graph: IRGraph): IRGraph

    // Compute the log, base two, of a number.
    protected def log2(i: Long, a: Int = 0): Int = {
        if (a < 64 && i > (1 << a)) {
            log2(i, a + 1)
        } else {
            a
        }
    }

    /** Get all blocks contained in a basic block with the specified block. */
    protected def getStateBlocks(graph: IRGraph, block: Int): Seq[Int] = {
        getStateBlocks(graph).filter(sb => sb.contains(block)).flatten
    }

    /** Get a Seq of Seqs of states in the same basic block. */
    protected def getStateBlocks(graph: IRGraph): Seq[Seq[Int]] = {

        var visited = Set[Int]()

        def expand(block: Int,
                   current: Seq[Int],
                   all: Seq[Seq[Int]]): Seq[Seq[Int]] = {

            if (visited.contains(block)) {
                return all :+ current
            }

            visited += block
            if (graph.inLinks(block).size > 1) {

                // Start of a new basic block.
                val nb = Seq(block)
                val all2 = all :+ current
                val result = graph.links(block).size match {
                    case 0 =>        // Stop statement
                        all2 :+ nb
                    case 1 =>        // Normal block
                        expand(graph.links(block).head.label, nb, all2)
                    case _ =>        // Conditional
                        val start = all2 :+ nb
                        val blocks = graph.links(block).map(_.label)
                        blocks.foldLeft(start) { (a, l) =>
                            expand(l, Seq(), a)
                        }
                }
                return result

            } else {

                // Append to the current basic block.
                val current2 = current :+ block
                val result = graph.links(block).size match {
                    case 0 =>   // Stop statement.
                        all :+ current2
                    case 1 =>   // Continue this basic block.
                        expand(graph.links(block).head.label, current2, all)
                    case _ =>   // End of the current basic block.
                        val start = all :+ current2
                        val blocks = graph.links(block).map(_.label)
                        blocks.foldLeft(start) { (a, l) =>
                            expand(l, Seq(), a)
                        }
                }
                return result

            }
        }

        expand(graph.root.label, Seq(), Seq())
    }

}
