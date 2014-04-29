package scalapipe.opt

import scalapipe._

private[opt] object CombineVariables extends Pass {

    override def toString = "combine variables"

    def run(context: IRContext, graph: IRGraph): IRGraph = {

        println("\tCombining variables")

        // Get a set of all variables that are actually used.
        val vars = graph.blocks.flatMap(_.symbols).toSet.filter { s =>
            s.isInstanceOf[StateSymbol] || s.isInstanceOf[TempSymbol]
        }
        var removed = Set[BaseSymbol]()

        // Get live variables for each state.
        var live = LiveVariables.solve(context.kt, graph)

        // Determine if two variables can be combined.
        def canCombine(g: IRGraph, a: BaseSymbol, b: BaseSymbol): Boolean = {

            // We can combine two variables if:
            //     1. Both variables have the same type.
            //     2. Both variables are still in use.
            //     3. Neither variable is used in a continuous assignment.
            //     4. The live ranges for both variables are disjoint.

            if (a == b) {
                return false
            }

            // Check types.
            if (a.valueType != b.valueType || !a.valueType.flat) {
                return false
            }

            // Make sure both variables are still in use.
            if (removed.contains(a) || removed.contains(b)) {
                return false
            }

            // Check if either variable is used in a continuous assignment.
            for (sb <- g.blocks if sb.continuous) {
                if (sb.symbols.contains(a) || sb.symbols.contains(b)) {
                    return false
                }
            }

            // Check if the live ranges are disjoint.
            val overlap = live.values.exists { v =>
                v.contains(a) && v.contains(b)
            }
            if (overlap) {
                return false
            }
            val same = g.blocks.exists { sb =>
                sb.symbols.contains(a) && sb.symbols.contains(b)
            }
            if (same) {
                return false
            }

            return true

        }

        // Replace variable a by b.
        def replace(g: IRGraph, a: BaseSymbol, b: BaseSymbol): IRGraph = {
            removed += a
            val updated = g.blocks.map(sb => sb.replace(a, b))
            updated.foldLeft(g) { (ng, sb) =>
                if (live(sb.label).contains(a)) {
                    live += (sb.label -> ((live(sb.label) - a) + b))
                }
                ng.update(sb)
            }
        }

        vars.foldLeft(graph) { (g1, a) =>
            vars.foldLeft(g1) { (g2, b) =>
                if (canCombine(g2, a, b)) {
                    println("\t\tReplacing " + a + " with " + b)
                    replace(g2, a, b)
                } else {
                    g2
                }
            }
        }

    }

}

