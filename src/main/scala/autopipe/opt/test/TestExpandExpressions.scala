
package autopipe.opt.test

import autopipe._
import autopipe.dsl._
import autopipe.gen._
import autopipe.opt._

object TestExpandExpressions {

   val block = new IRBuilder {

      val in = input(SIGNED32)
      val out = output(SIGNED32)

      val x = state(SIGNED32)
      val y = state(SIGNED32)

      label(0)
      start

      label(1)
      op(NodeType.assign, x, in)

      label(2)
      op(NodeType.assign, y, literal(1))

      label(3)
      op(NodeType.add, x, x, y)

      label(4)
      op(NodeType.add, x, x, x)

      label(5)
      op(NodeType.assign, out, x)

   }

   val expected = new IRBuilder {

      val in = input(SIGNED32)
      val out = output(SIGNED32)

      val x = state(SIGNED32)
      val y = state(SIGNED32)
      val t1 = temp(SIGNED32, 1)
      val t2 = temp(SIGNED32, 2)

      label(0)
      start

      label(1)
      op(NodeType.assign, t1, in)

      label(2)
      op(NodeType.assign, y, literal(1))

      label(3)
      op(NodeType.add, t2, t1, y)

      label(4)
      op(NodeType.add, x, t2, t2)

      label(5)
      op(NodeType.assign, out, x)

   }

   def run: Boolean = {
      val graph = block.graph
      val context = new HDLIRContext(block)
      val optGraph = ExpandExpressions.run(context, graph)
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

