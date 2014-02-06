package scalapipe.kernels

import scalapipe.dsl._

class EqualSplit(t: Type, n: Int = 2) extends Kernel {

    val x = input(t)

    for (i <- Range(0, n)) {
        val y = output(t)
        y = x
    }

}
