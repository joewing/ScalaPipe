
package autopipe.opt.test

import autopipe._
import autopipe.dsl._
import autopipe.gen._
import autopipe.opt._

object TestStateCompression {

   val block = new IRBuilder {

      val in = input(SIGNED32)
      val out = output(SIGNED32)

      val x = state(SIGNED32)
      val y = state(SIGNED32)
      val z = state(SIGNED32)

      label(0)
      start

      label(1)
      op(NodeType.assign, x, in)

      label(2)
      op(NodeType.add, y, x, literal(1))

      label(3)
      op(NodeType.add, z, x, literal(1))

      label(4)
      op(NodeType.add, z, z, literal(1))

      label(5)
      op(NodeType.assign, out, z)

      label(6)
      op(NodeType.assign, x, in)

      label(7)
      op(NodeType.assign, y, x)

      label(8)
      op(NodeType.assign, z, y)

      label(9)
      op(NodeType.assign, out, z)

   }

   val expected = new IRBuilder {

      val in = input(SIGNED32)
      val out = output(SIGNED32)

      val x = state(SIGNED32)
      val y = state(SIGNED32)
      val z = state(SIGNED32)

      label(0)
      start

      label(1)
      op(NodeType.assign, x, in)

      label(2)
      op(NodeType.add, z, x, literal(1))
      op(NodeType.add, y, x, literal(1))

      label(4)
      op(NodeType.add, z, z, literal(1))

      label(5)
      op(NodeType.assign, out, z)

      label(6)
      op(NodeType.assign, x, in)

      label(7)
      op(NodeType.assign, y, x)

      label(8)
      op(NodeType.assign, z, y)

      label(9)
      op(NodeType.assign, out, z)

   }

   def run: Boolean = {
      val graph = block.graph
      val context = new HDLIRContext(block)
      val optGraph = StateCompression.run(context, graph)
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

