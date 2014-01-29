package scalapipe.test

import scalapipe.kernels._
import scalapipe.dsl._

object ArrayTest {

    val SmallArray = new AutoPipeArray(UNSIGNED8, 8)

    val LargeArray = new AutoPipeArray(SmallArray, 1024)

    val Gen = new Func("Gen") {
        val y0      = output(SmallArray)
        val count   = local(UNSIGNED32, 0)
        val big     = local(LargeArray)
        val i       = local(UNSIGNED32)

        if (count == 0) {
            i = 0
            while (i < 1024) {
                for (k <- 0 until 8) {
                    big(i)(k) = i * 8 + k
                }
                i += 1
            }
        }

        y0 = big(count)
        count += 1

    }

    val Print = new Kernel("Print") {
        val x0      = input(SmallArray)
        val temp    = local(SmallArray)
        val i       = local(UNSIGNED32)
        val count   = local(UNSIGNED32, 0)

        temp = x0
        i = 0
        stdio.printf("""OUTPUT %d: """, count)
        while (i < 8) {
            stdio.printf("""%d """, temp(i))
            i += 1
        }
        stdio.printf("""\n""")
        count += 1
        if (count == 10) {
            stdio.exit(0)
        }

    }

    def main(args: Array[String]) {
        val mapping = if (args.length > 0) args(0).toInt else 0
        val app = new AutoPipeApp {
            Print(Gen())
            mapping match {
                case 0 => ()
                case 1 => map(Gen -> Print, FPGA2CPU())
            }
        }
        app.emit("ArrayTest")
    }

}
