
package autopipe.opt.test

import autopipe._
import autopipe.dsl._
import autopipe.gen._
import autopipe.opt._

object TestReassignStates {

   val block = new IRBuilder {

      val in = input(SIGNED32)
      val out = output(SIGNED32)

      val x = state(SIGNED32)

      label(0)
      start(5)

      label(5)
      op(NodeType.assign, x, in)

      label(8)
      op(NodeType.add, x, x, literal(1))

      label(9)
      op(NodeType.assign, out, x)

   }

   val expected = new IRBuilder {

      val in = input(SIGNED32)
      val out = output(SIGNED32)

      val x = state(SIGNED32)

      label(0)
      start

      label(1)
      op(NodeType.assign, x, in)

      label(2)
      op(NodeType.add, x, x, literal(1))

      label(3)
      op(NodeType.assign, out, x)

   }

   def run: Boolean = {
      val graph = block.graph
      val context = new HDLIRContext(block)
      val optGraph = ReassignStates.run(context, graph)
      val expectedGraph = expected.graph
      if (!expectedGraph.equivalent(optGraph)) {
         println("INPUT:\n" + graph)
         println("GOT:\n" + optGraph)
         println("EXPECTED:\n" + expectedGraph)
         return false
      } else {
         return true
      }
   }

}

