package scalapipe.kernels

import scalapipe.dsl._

class DuplicateBlock(t: Type, n: Int = 2) extends Kernel {

    val x0 = input(t)
    val temp = local(t)

    temp = x0
    for (i <- Range(0, n)) {
        val o = output(t)
        o = temp
    }

}
