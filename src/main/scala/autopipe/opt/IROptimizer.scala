
package autopipe.opt

import autopipe._

private[autopipe] case class IROptimizer(
      val co: CodeObject,
      val context: IRContext
   ) {

   private val parameters = co.parameters

   def optimize(graph: IRGraph): IRGraph = {
      println("Optimizing " + co.name)
      val initialStateCount = countStates(graph)
      val initialVarCount = co.states.size + co.temps.size

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
      val finalVarCount = co.states.size + co.temps.size
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

