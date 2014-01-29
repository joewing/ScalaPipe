package blocks

import autopipe.dsl._

class EqualJoinBlock(t: AutoPipeType, n: Int = 2) extends Kernel {

    val y = output(t)

    for (i <- 0 until n) {
        val x = input(t)
        y = x
    }

}
