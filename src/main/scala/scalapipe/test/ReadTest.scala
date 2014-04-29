package scalapipe.test

import scalapipe.kernels._
import scalapipe.dsl._

object ReadTest {

    val Gen = new Kernel("Gen") {
        val y0 = output(UNSIGNED32)
        val count = local(UNSIGNED32, 0)
        if (count < 10) {
            y0 = count
            count += 1
        } else {
            stop
        }
    }

    val Update = new Func("Update") {
        val x0 = input(UNSIGNED32)
        return (x0 << 16) | x0
    }

    val Print = new Kernel("Print") {
        val x0 = input(UNSIGNED32)
        stdio.printf("OUTPUT %08x\n", x0)
    }

    def main(args: Array[String]) {
        val mapping = args.headOption.getOrElse("0").toInt
        val app = new Application {
            Print(Update(Gen()))
            mapping match {
                case 0 => ()
                case 1 => map(Update -> Print, FPGA2CPU())
            }
        }
        app.emit("ReadTest")
    }

}
