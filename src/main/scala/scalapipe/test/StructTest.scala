package scalapipe.test

import scalapipe.kernels._
import scalapipe.dsl._

object StructTest {

    val Struct1 = new Struct {
        val a = SIGNED32    // 0 .. 3
        val b = BOOL        // 4 .. 4
    }

    val Struct2 = new Struct {
        val x = Struct1     // 0 .. 7
        val y = UNSIGNED32  // 8 .. 11
    }

    val Gen = new Func("Gen") {
        val y0      = output(Struct2)
        val count   = local(UNSIGNED32, 0)
        val t       = local(Struct2)

        t.x.a = count + 1
        t.x.b = count & 1
        t.y = count
        y0 = t
        count += 1
    }

    val Print = new Kernel("Print") {
        val x0  = input(Struct2)
        val t   = local(Struct2)

        t = x0
        stdio.printf("OUTPUT %d\n", t.y)
        if (t.x.a <> t.y + 1) {
            stdio.printf("OUTPUT: ERROR a\n")
        }
        if (t.x.b <> (t.y & 1)) {
            stdio.printf("OUTPUT: ERROR b\n")
        }
        if (t.y == 9) {
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
        app.emit("StructTest")
    }

}
