
package autopipe.opt

import scala.collection.mutable.HashSet
import autopipe._

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
    protected def getStateBlocks(graph: IRGraph, block: Int): List[Int] = {
        getStateBlocks(graph).filter(sb => sb.contains(block)).flatten
    }

    protected def getStateBlocks(graph: IRGraph): List[List[Int]] = {

        val visited = new HashSet[Int]

        def expand(block: Int, current: List[Int],
                      all: List[List[Int]]): List[List[Int]] = {

            if (!visited.contains(block)) {
                visited += block
                if (graph.inLinks(block).size > 1) {
                    // Start of a new basic block.
                    val nb = List(block)
                    val all2 = all :+ current
                    graph.links(block).size match {
                        case 0 =>        // Stop statement
                            all2 :+ nb
                        case 1 =>        // Normal block
                            expand(graph.links(block).head.label, nb, all2)
                        case _ =>        // Conditional
                            graph.links(block).foldLeft(all2 :+ nb) { (a, l) =>
                                expand(l.label, Nil, a)
                            }
                    }
                } else {
                    // Append to the current basic block.
                    val current2 = current :+ block
                    graph.links(block).size match {
                        case 0 =>
                            // Stop statement.
                            all :+ current2
                        case 1 =>
                            // Continue this basic block.
                            expand(graph.links(block).head.label, current2, all)
                        case _ =>
                            // End of the current basic block.
                            graph.links(block).foldLeft(all :+ current2) { (a, l) =>
                                expand(l.label, Nil, a)
                            }
                    }
                }
            } else {
                all :+ current
            }

        }

        expand(graph.root.label, Nil, Nil)

    }

}

