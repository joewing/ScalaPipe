package blocks

import scalapipe.dsl._

class AverageBlock(t: AutoPipeType) extends Kernel {

    val x0 = input(t)
    val x1 = input(t)
    val y0 = output(t)

    y0 = (x0 + x1) / 2

}
