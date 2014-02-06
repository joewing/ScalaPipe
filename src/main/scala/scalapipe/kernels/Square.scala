package scalapipe.kernels

import scalapipe.dsl._

class Square(t: Type) extends Func {

    val x0 = input(t)
    val temp = local(t)

    temp = x0
    return temp * temp

}
