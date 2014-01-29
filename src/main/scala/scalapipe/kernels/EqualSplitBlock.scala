package scalapipe.kernels

import scalapipe.dsl._

class EqualSplitBlock(t: Type, n: Int = 2) extends Kernel {

    val x = input(t)

    for (i <- 0 until n) {
        val y = output(t)
        y = x
    }

}
