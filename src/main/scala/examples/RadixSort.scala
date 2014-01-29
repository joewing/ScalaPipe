package examples

import blocks._
import autopipe._
import autopipe.dsl._

import scala.util.Random

object RadixSort {

    val bitCount = 8
    val itemCount = 16

    val Source = new Kernel {
        val out = output(UNSIGNED32)
        for (i <- 0 until itemCount) {
            out = Random.nextInt(1 << bitCount)
        }
        stop
    }

    val Print = new Kernel {
        val in = input(UNSIGNED32)
        stdio.printf("""%u\n""", in)
    }

    val Sort = new Kernel {

        val in = input(UNSIGNED32)
        val out = output(UNSIGNED32)
        val radix = config(UNSIGNED32, 'radix, 0)
        val buffer = local(new AutoPipeArray(UNSIGNED32, itemCount))
        val temp = local(UNSIGNED32)
        val i = local(UNSIGNED32)
        val j = local(UNSIGNED32)

        i = 0
        while (i < itemCount) {
            temp = in
            if ((temp & radix) == 0) {
                out = temp
            } else {
                buffer(j) = temp
                j += 1
            }
            i += 1
        }
        i = 0
        while (i < j) {
            out = buffer(i)
            i += 1
        }
        stop

    }

    val RadixSort = new AutoPipeApp {
        var src = Source()()
        for (i <- 0 until bitCount) {
            src = Sort('radix -> (1 << i), src)()
        }
        Print(src)
    }

    def main(args: Array[String]) {
        RadixSort.emit("radix")
    }

}
