package scalapipe.kernels

import scalapipe.dsl._

class SquareBlock(t: AutoPipeType) extends Kernel {

    val x0 = input(t)
    val y0 = output(t)
    val temp = local(t)

    temp = x0
    y0 = temp * temp

}
