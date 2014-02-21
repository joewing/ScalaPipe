package scalapipe.test

import scalapipe.kernels._
import scalapipe.dsl._

object ArrayTest {

    val SmallArray = Vector(UNSIGNED8, 8)

    val LargeArray = Vector(SmallArray, 1024)

    val Gen = new Func("Gen") {
        val y0      = output(SmallArray)
        val count   = local(UNSIGNED32, 0)
        val big     = local(LargeArray)

        if (count == 0) {
            for (i <- 0 until 1024) {
                for (k <- 0 until 8) {
                    big(i)(k) = i * 8 + k
                }
            }
        }

        y0 = big(count)
        count += 1

    }

    val Print = new Kernel("Print") {
        val x0      = input(SmallArray)
        val temp    = local(Vector(UNSIGNED8, 8))
        val count   = local(UNSIGNED32, 0)

        temp = x0
        stdio.printf("OUTPUT %d: ", count)
        for (i <- 0 until 8) {
            stdio.printf("%d ", temp(i))
        }
        stdio.printf("\n")
        count += 1
        if (count == 10) {
            stdio.exit(0)
        }

    }

    def main(args: Array[String]) {
        val mapping = args.headOption.getOrElse("0").toInt
        val app = new Application {
            Print(Gen())
            mapping match {
                case 0 => ()
                case 1 => map(Gen -> Print, FPGA2CPU())
            }
        }
        app.emit("ArrayTest")
    }

}
