package autopipe.test

import blocks._
import autopipe.dsl._

object ArrayTest2 {

    def main(args: Array[String]) {

        // First argument is the mapping.
        val mapping = if (args.length > 0) args(0).toInt else 0

        // Second argument is the array element type.
        val vtype = if (args.length > 1) args(0).toInt match {
            case 0 => UNSIGNED8
            case 1 => UNSIGNED16
            case 2 => UNSIGNED32
            case 3 => UNSIGNED64
        } else {
            UNSIGNED32
        }

        val ArrayType = new AutoPipeArray(vtype, 1024)

        val Gen = new Func("Gen") {
            val y0      = output(UNSIGNED32)
            val count   = local(UNSIGNED32, 0)
            val array   = local(ArrayType)
            val i       = local(UNSIGNED32)
            val sum     = local(UNSIGNED32)

            if (count == 0) {
                i = 0
                while (i < 1024) {
                    array(i) = i
                    i += 1
                }
            }

            i = 0
            sum = 0
            while (i < 1024) {
                array(i) += 1
                sum += array(i)
                i += 1
            }

            y0 = sum
            count += 1

        }

        val Print = new Kernel("Print") {
            val x0      = input(UNSIGNED32)
            val count   = local(UNSIGNED32, 0)
            stdio.printf("""OUTPUT %d\n""", x0)
            count += 1
            if (count == 10) {
                stdio.exit(0)
            }
        }

        val app = new AutoPipeApp {
            Print(Gen())
            mapping match {
                case 0 => ()
                case 1 => map(Gen -> Print, FPGA2CPU())
            }
        }
        app.emit("ArrayTest2")
    }

}
