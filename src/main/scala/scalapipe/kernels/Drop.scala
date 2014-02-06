package scalapipe.kernels

import scalapipe.dsl._

class Drop(t: Type) extends Kernel {

    val i = input(t)
    val temp = local(t)

    temp = i

}
