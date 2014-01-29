package scalapipe.opt

import scalapipe._

private[opt] object RemoveVariables extends Pass {

    override def toString = "remove variables"

    def run(context: IRContext, graph: IRGraph): IRGraph = {

        if (context.eliminateVariables) {

            // Get the variables that are used.
            val states = graph.blocks.flatMap { b =>
                b.symbols.collect { case s: StateSymbol => s }
            }
            val temps = graph.blocks.flatMap { b =>
                b.symbols.collect { case t: TempSymbol => t }
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
