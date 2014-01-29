package scalapipe.kernels

import scalapipe.dsl._

class JoinBlock(t: Type, n: Int = 2) extends Kernel {

    val y = output(t)

    for (i <- 0 until n) {
        val x = input(t)
        if (avail(x)) {
            y = x
        }
    }

}
