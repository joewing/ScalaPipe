package autopipe.opt

import autopipe._

import scala.collection.mutable.HashSet

private[opt] object RemoveVariables extends Pass {

    override def toString = "remove variables"

    def run(context: IRContext, graph: IRGraph): IRGraph = {

        if (context.eliminateVariables) {

            // Get the variables that are used.
            val states = new HashSet[StateSymbol]
            val temps  = new HashSet[TempSymbol]
            graph.blocks.foreach { b =>
                states ++= b.symbols.collect { case s: StateSymbol => s }
                temps  ++= b.symbols.collect { case t: TempSymbol  => t }
            }

            // Build up the list to remove.
            val removedStates = context.kt.states.filter { s =>
                !states.contains(s)
            }
            val removedTemps = context.kt.temps.filter { t =>
                !temps.contains(t)
            }

            // Remove extras.
            context.kt.states --= removedStates
            context.kt.temps --= removedTemps

        }

        return graph

    }

}
