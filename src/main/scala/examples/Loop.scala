
package examples

import autopipe._
import autopipe.dsl._
import blocks._

object Loop {

    val Source = new AutoPipeBlock {
        val out = output(UNSIGNED32)
        out = 1
        stop
    }

    val Print = new AutoPipeBlock {

        val in0 = input(UNSIGNED32)
        val in1 = input(UNSIGNED32)
        val out = output(UNSIGNED32)
        val t = local(UNSIGNED32, 0)

        if (t == 0) {
            t = in0
        } else {
            t = in1
        }
        stdio.printf("""%d\n""", t)
        if (t < 10) {
            out = t + 1
        } else {
            stop
        }

    }

    val Loop = new AutoPipeLoopBack(UNSIGNED32)

    val App = new AutoPipeApp {

        val src0 = Source()
        val src1 = Loop.output()
        val result = Print(src0, src1)
        Loop.input(result)

    }

    def main(args: Array[String]) {
        App.emit("loop")
    }

}

