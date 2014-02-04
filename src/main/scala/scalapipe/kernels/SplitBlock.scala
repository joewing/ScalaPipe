package scalapipe.kernels

import scalapipe.dsl._

class SplitBlock(t: Type, n: Int = 2) extends Kernel {

    val x = input(t)

    for (i <- Range(0, n)) {
        val y = output(t)
        if (avail(y)) {
            y = x
        }
    }

}
