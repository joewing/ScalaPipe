
package autopipe.opt

import autopipe._

import scala.collection.mutable.HashSet

private[opt] object DSE extends Pass {

   override def toString = "DSE"

   def run(context: IRContext, graph: IRGraph): IRGraph = {

      println("\tEliminating dead stores")

      dse(context, graph)

   }

   private def dse(context: IRContext, graph: IRGraph): IRGraph = {

      // Compute a set of definitions that are used.
      // Note that reaching contains a mapping from block to defining
      // (symbol, block).
      val reaching = ReachingDefs.solve(context.co, graph)
      val defs = new HashSet[IRNode]
      graph.blocks.foreach { sb =>
         reaching(sb.label).foreach { case (s, d) =>

            // Skip if sb doesn't use this symbol.
            if (sb.srcs.contains(s)) {

               // Add the nodes defining this symbol to our set.
               defs ++= graph.block(d).nodes.filter { n =>
                  n.dests.contains(s)
               }

            }
         }
      }

      // Get a list of nodes we can eliminate.
      val toRemove = graph.nodes.flatMap {
         case i: IRInstruction if !defs.contains(i) =>
            if (i.symbols.forall { s => !s.isInstanceOf[PortSymbol] }) {
               Some(i)
            } else {
               None
            }
         case _ => None
      }

      // Remove the nodes.
      if (!toRemove.isEmpty) {
         val newGraph = toRemove.foldLeft(graph) { (g, n) =>
            println("\t\tRemoving " + n)
            g.remove(n)
         }
         dse(context, newGraph)
      } else {
         graph
      }

   }
   

}

