package blocks

import autopipe.dsl._

class JoinBlock(t: AutoPipeType, n: Int = 2) extends Kernel {

    val y = output(t)

    for (i <- 0 until n) {
        val x = input(t)
        if (avail(x)) {
            y = x
        }
    }

}
