package autopipe.opt

import autopipe._

private[autopipe] case class IROptimizer(
        val kt: KernelType,
        val context: IRContext
    ) {

    private val parameters = kt.parameters

    def optimize(graph: IRGraph): IRGraph = {
        println("Optimizing " + kt.name)
        val initialStateCount = countStates(graph)
        val initialVarCount = kt.states.size + kt.temps.size

        val passes = Array[Pass](
            ExpandExpressions,
            CSE,
            DSE,
            DCE,
            StrengthReduction,
            CopyPropagation,
            StateCompression,
            ContinuousAssignment,
            CombineVariables,
            CSE,
            RemoveVariables,
            ReassignStates
        )
        val newGraph = passes.foldLeft(graph) { (g, pass) =>
            pass.run(context, g)
        }

        val finalStateCount = countStates(newGraph)
        val finalVarCount = kt.states.size + kt.temps.size
        println("\tStates:    " + initialStateCount + " -> " + finalStateCount)
        println("\tVariables: " + initialVarCount + " -> " + finalVarCount)
        println("\tScore:     " + computeScore(newGraph))
        println("\tClocks:    " + computeClocks(newGraph))
        return newGraph
    }

    private def computeClocks(graph: IRGraph): Int = {
        graph.blocks.foldLeft(0) { (a, b) =>
            a + HDLTiming.getStateTime(b)
        }
    }

    private def computeScore(graph: IRGraph): Float = {
        val stateCount = graph.blocks.filter(b => !b.continuous).size
        val opCount = graph.nodes.size
        opCount.toFloat / stateCount.toFloat
    }

    private def countStates(graph: IRGraph): Int = {
        graph.blocks.count(b => b.label > 0 && !b.continuous)
    }

}
