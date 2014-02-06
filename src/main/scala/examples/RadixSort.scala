package examples

import scalapipe.kernels._
import scalapipe.dsl._

object RadixSort extends App {

    val bitCount = 8
    val itemCount = 16

    val Source = new Kernel {
        val out = output(UNSIGNED32)
        for (i <- 0 until itemCount) {
            out = stdio.rand() % (1 << bitCount)
        }
        stop
    }

    val Print = new Kernel {
        val in = input(UNSIGNED32)
        stdio.printf("%u\n", in)
    }

    val Sort = new Kernel {

        val in = input(UNSIGNED32)
        val out = output(UNSIGNED32)
        val radix = config(UNSIGNED32, 'radix, 0)
        val buffer = local(new Vector(UNSIGNED32, itemCount))
        val temp = local(UNSIGNED32)
        val j = local(UNSIGNED32)

        for (i <- 0 until itemCount) {
            temp = in
            if ((temp & radix) == 0) {
                out = temp
            } else {
                buffer(j) = temp
                j += 1
            }
        }
        for (i <- 0 until j) {
            out = buffer(i)
        }
        stop

    }

    val RadixSort = new Application {
        var src = Source()()
        for (i <- 0 until bitCount) {
            src = Sort('radix -> (1 << i), src)()
        }
        Print(src)
    }
    RadixSort.emit("radix")

}
