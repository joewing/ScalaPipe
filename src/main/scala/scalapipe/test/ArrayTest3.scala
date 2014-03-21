package scalapipe.test

import scalapipe.kernels._
import scalapipe.dsl._

object ArrayTest3 {

    def main(args: Array[String]) {

        val mapping = args.headOption.getOrElse("0").toInt

        val ArrayType = Vector(UNSIGNED32, 100)

        val Gen = new Kernel("Gen") {
            val y0      = output(UNSIGNED32)
            val array   = local(ArrayType)
            val temp    = local(ArrayType)

            for (i <- 0 until 100) {
                array(i) = i
                temp(i) = 0
            }

            temp = array

            for (i <- 0 until 100) {
                y0 = temp(i)
            }

            stop

        }

        val Print = new Kernel("Print") {
            val x0      = input(UNSIGNED32)
            val count   = local(UNSIGNED32, 0)
            val sum     = local(UNSIGNED32)
            sum += x0
            count += 1
            if (count == 100) {
                stdio.printf("OUTPUT %d\n", sum)
                stdio.exit(0)
            }
        }

        val app = new Application {
            Print(Gen())
            mapping match {
                case 0 => ()
                case 1 => map(Gen -> Print, FPGA2CPU())
            }
        }
        app.emit("ArrayTest3")
    }

}
