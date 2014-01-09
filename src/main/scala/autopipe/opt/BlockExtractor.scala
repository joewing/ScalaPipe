
package autopipe.opt

import autopipe._

private[opt] object BlockExtractor {

/*
    private val minStates = 2
    private val maxBits    = 128

    def extract(co: CodeObject, graph: IRGraph): List[List[StateBlock]] = {

        val live = LiveVariables.solve(co, graph)
        val lst = extractAll(graph, graph.blocks.toList).filter { l =>
            val liveIn    = used(l).intersect(live(l.head).toList)
            val liveOut  = modified(l).intersect(live(l.last).toList)
            val liveBits = bits(liveIn) + bits(liveOut)
            l.size >= minStates && liveBits <= maxBits && !hasPort(l)
        }

        for (l <- lst) {
            val liveIn  = used(l).intersect(live(l.head).toList)
            val liveOut = modified(l).intersect(live(l.last).toList)
            println(l.head.label + " -> " + l.size + " | " +
                      liveIn.size + "(" + bits(liveIn) + "), " +
                      liveOut.size + "(" + bits(liveOut) + ")")
        }

        lst

    }

    private def bits(symbols: List[BaseSymbol]): Int = {
        symbols.foldLeft(0) { (a, s) => a + s.valueType.bits }
    }

    private def used(blocks: List[StateBlock]): List[BaseSymbol] = {
        blocks.flatMap(b => b.srcs)
    }

    private def modified(blocks: List[StateBlock]): List[BaseSymbol] = {
        blocks.flatMap(b => b.dests)
    }

    private def hasPort(blocks: List[StateBlock]): Boolean = {
        blocks.exists(b => b.symbols.exists(_.isInstanceOf[PortSymbol]))
    }

    private def extractAll(graph: IRGraph,
                                  blocks: List[StateBlock]): List[List[StateBlock]] = {
        if (blocks.isEmpty) {
            return Nil
        } else {
            return extractFrom(graph, List(blocks.head)) ++
                     extractAll(graph, blocks.tail)
        }
    }

    private def extractFrom(graph: IRGraph,
                                    block: List[StateBlock]): List[List[StateBlock]] = {
        val links = graph.links(block.last)
        if (links.size > 1 || graph.inLinks(links.head).size > 1) {
            return List(block)
        } else {
            return block :: extractFrom(graph, block :+ links.head)
        }
    }
*/

}

