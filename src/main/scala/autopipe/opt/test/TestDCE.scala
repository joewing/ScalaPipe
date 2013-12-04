
package autopipe.opt.test

import autopipe._
import autopipe.dsl._
import autopipe.gen._
import autopipe.opt._

object TestDCE {

   val block = new IRBuilder {

      val in = input(SIGNED32)
      val out = output(SIGNED32)

      val x = state(SIGNED32)
      val t = state(BOOL)

      label(0)
      start

      label(1)
      goto(3)

      label(2)
      op(NodeType.assign, x, in)

      label(3)
      op(NodeType.add, x, x, literal(1))

      label(4)
      op(NodeType.assign, out, x)

      label(5)
      op(NodeType.sub, x, x, literal(1))

      label(6)
      op(NodeType.le, t, x, literal(0))

      label(7)
      cond(t, 9, 4)

      label(8)
      op(NodeType.assign, x, literal(0))

      label(9)
      stop

   }

   val expected = new IRBuilder {

      val in = input(SIGNED32)
      val out = output(SIGNED32)

      val x = state(SIGNED32)
      val t = state(BOOL)

      label(0)
      start

      label(1)
      goto(3)

      label(3)
      op(NodeType.add, x, x, literal(1))

      label(4)
      op(NodeType.assign, out, x)

      label(5)
      op(NodeType.sub, x, x, literal(1))

      label(6)
      op(NodeType.le, t, x, literal(0))

      label(7)
      cond(t, 9, 4)

      label(9)
      stop

   }

   def run: Boolean = {
      val graph = block.graph
      val context = new HDLIRContext(block)
      val optGraph = DCE.run(context, graph)
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

