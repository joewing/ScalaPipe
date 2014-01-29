package scalapipe.kernels

import scalapipe.dsl._

class DropBlock(t: Type) extends Kernel {

    val i = input(t)
    val temp = local(t)

    temp = i

}
