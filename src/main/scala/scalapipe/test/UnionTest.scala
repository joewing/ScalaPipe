package scalapipe.test

import scalapipe.kernels._
import scalapipe.dsl._

object UnionTest {

    val Union1 = new Union {
        val a = SIGNED32
        val b = BOOL
    }

    val Union2 = new Union {
        val x = Union1
        val y = SIGNED32
    }

    val Gen = new Kernel {
        val y0 = output(Union2)
        val t = local(Union2)
        t.y = 5
        y0 = t
        stop
    }

    val Print = new Kernel {
        val x0 = input(Union2)
        val t = local(Union2)

        t = x0
        stdio.printf("OUTPUT %d %d\n", t.x.a, sizeof(t))
        stdio.exit(0)
    }

    def main(args: Array[String]) {
        val mapping = if (args.length > 0) args(0).toInt else 0
        val app = new Application {
            Print(Gen())
            mapping match {
                case 0 => ()
                case 1 => map(Gen -> Print, FPGA2CPU())
            }
        }
        app.emit("UnionTest")
    }

}
