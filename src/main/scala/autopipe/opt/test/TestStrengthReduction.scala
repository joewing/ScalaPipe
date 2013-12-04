
package autopipe.opt.test

import autopipe._
import autopipe.dsl._
import autopipe.gen._
import autopipe.opt._

object TestStrengthReduction {

   val block = new IRBuilder {

      val in = input(SIGNED32)
      val out = output(SIGNED32)

      val x = state(SIGNED32)

      label(0)
      start

      label(1)
      op(NodeType.add, x, x, x)

      label(2)
      op(NodeType.sub, x, x, x)

      label(3)
      op(NodeType.mul, x, x, literal(1))

   }

   val expected = new IRBuilder {

      val in = input(SIGNED32)
      val out = output(SIGNED32)

      val x = state(SIGNED32)

      label(0)
      start

      label(1)
      op(NodeType.shl, x, x, literal(1))

      label(2)
      op(NodeType.assign, x, literal(0))

      label(3)
      op(NodeType.assign, x, x)

   }

   def run: Boolean = {
      val graph = block.graph
      val context = new HDLIRContext(block)
      val optGraph = StrengthReduction.run(context, graph)
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

