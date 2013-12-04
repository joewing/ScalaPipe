
package autopipe.opt

import autopipe._

import scala.collection.immutable.HashSet

object ReachingDefs extends DataFlowProblem {

   type T = (BaseSymbol, Int)

   def forward = true

   def init(co: CodeObject, graph: IRGraph) = HashSet[T]()

   def gen(sb: StateBlock, in: Set[T]): Set[T] =
      HashSet[T](sb.dests.map(d => ((d, sb.label))): _*)

   def kill(sb: StateBlock, in: Set[T]): Set[T] = {
      in.filter { case t@(s, d) =>
         sb.nodes.exists { node =>
            node match {
               case vs: IRVectorStore  => false
               case as: IRArrayStore   => false
               case n: IRNode          => n.dests.contains(s)
               case _                  => false
            }
         }
      }
   }

   def meet(a: Set[T], b: Set[T]) = a.union(b)

}

