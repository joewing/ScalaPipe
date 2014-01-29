package blocks

import scalapipe.dsl._

class DuplicateBlock(t: AutoPipeType, n: Int = 2) extends Kernel {

    val x0 = input(t)
    val temp = local(t)

    temp = x0
    for (i <- 0 until n) {
        val o = output(t)
        o = temp
    }

}
