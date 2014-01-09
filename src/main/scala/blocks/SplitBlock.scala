
package blocks

import autopipe.dsl._

class SplitBlock(t: AutoPipeType, n: Int = 2) extends AutoPipeBlock {

    val x = input(t)

    for (i <- 0 until n) {
        val y = output(t)
        if (avail(y)) {
            y = x
        }
    }

}

