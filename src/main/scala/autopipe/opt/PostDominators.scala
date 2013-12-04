
package autopipe.opt

import autopipe._
import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet
import scala.collection.mutable.Stack

private[autopipe] class PostDominators(co: CodeObject, graph: IRGraph)
//      extends DominatorBase(_co, _graph) {
{

   def inputs(a: StateBlock) = a.links

   def outputs(a: StateBlock) = graph.inLinks(a)

   def root = graph.root

   /** Determine if a post-dominates b. */
   // FIXME: This is slow, we do this for now because there is no
   // "end" node.
   def dominates(a: StateBlock, b: StateBlock): Boolean = {

      val work = new HashSet[StateBlock]
      val visited = new HashSet[StateBlock]

      // Start with all links into A.
      work ++= graph.inLinks(a)
      while (!work.isEmpty) {
         val block = work.head
         work.remove(block)
         if (block == a) {
            // Found another path.
            return false
         }
         if (block != b && !visited.contains(block)) {
            visited += block
            work ++= graph.inLinks(block)
         }
      }

      return true

   }

}

