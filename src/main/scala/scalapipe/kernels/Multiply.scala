package scalapipe.kernels

import scalapipe.dsl._

class Multiply(t: Type) extends Func {

    val x0 = input(t)
    val x1 = input(t)
    val y0 = output(t)

    y0 = x0 * x1

}
