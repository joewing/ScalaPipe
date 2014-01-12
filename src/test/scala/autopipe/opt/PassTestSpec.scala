package autopipe.opt

import autopipe._
import autopipe.gen._

abstract class PassTestSpec extends UnitSpec {

    def checkPass(input: IRBuilder,
                  expected: IRBuilder,
                  transform: Pass) {

        val graph = input.graph
        val context = new HDLIRContext(input)
        val transformedGraph = transform.run(context, graph)
        val expectedGraph = expected.graph

        if (!expectedGraph.equivalent(transformedGraph)) {
            val tstr = transformedGraph.toString
            val estr = expectedGraph.toString
            val msg = "got:\n" + tstr + "\nexpected:\n" + estr
            fail(msg)
        }

    }

}
