package scalapipe.kernels

import scalapipe.dsl._

class Average(t: Type) extends Func {

    val x0 = input(t)
    val x1 = input(t)

    return (x0 + x1) / 2

}
