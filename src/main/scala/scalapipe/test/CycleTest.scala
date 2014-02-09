package scalapipe.test

import scalapipe.kernels._
import scalapipe.dsl._

object CycleTest {

    val Start = new Kernel {
        val y0 = output(SIGNED32)
        y0 = 0
        stop
    }

    val Looper = new Kernel {
        val x0 = input(SIGNED32)
        val x1 = input(SIGNED32)
        val y0 = output(SIGNED32)
        val y1 = output(SIGNED32)
        val t = local(SIGNED32, 0)

        if (t == 0) {
            t = x0 + 1
        } else {
            t = x1 + 1
        }
        y0 = t
        y1 = t

    }

    val Print = new Kernel {
        val x0 = input(SIGNED32)
        val t = local(SIGNED32)
        t = x0
        stdio.printf("OUTPUT %d\n", t)
        if (t == 9) {
            stdio.exit(0)
        }
    }

    val CycleU32 = Cycle

    def main(args: Array[String]) {
        val mapping = args.headOption.getOrElse("0").toInt
        val app = new Application {
            val cycle = Cycle(SIGNED32)
            val result = Looper(Start(), cycle)
            Print(result(0))
            cycle(result(1))
            mapping match {
                case 0 => ()
                case 1 => map(ANY_KERNEL -> Print, FPGA2CPU())
            }
        }
        app.emit("CycleTest")
    }

}
