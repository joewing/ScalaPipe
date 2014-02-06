package scalapipe.kernels

import scalapipe.dsl._

class Join(t: Type, n: Int = 2) extends Kernel {

    val y = output(t)

    for (i <- Range(0, n)) {
        val x = input(t)
        if (avail(x)) {
            y = x
        }
    }

}
