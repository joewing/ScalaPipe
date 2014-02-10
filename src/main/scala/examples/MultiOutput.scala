package examples

import scalapipe.kernels.{stdio, Duplicate}
import scalapipe.dsl._

object MultiOutput {

		val count = 10

		def main(args: Array[String]) {

			val Random = new Kernel("Random") {
				val y0 = output(FLOAT32)
				val iterations = local(UNSIGNED32, count)
				while (iterations > 0) {
					y0 = (stdio.rand() % 10000) + 1
					iterations -= 1
				}
				stop
			}

			class MathBlock(t: Type) extends Kernel("MathBlock") {
				val x0 = input(t)
				val x1 = input(t)
				val y0 = output(t)
				val y1 = output(t)
				val y2 = output(t)
				val y3 = output(t)
                val a = local(t)
                val b = local(t)
                a = x0
                b = x1
				y0 = a + b
				y1 = a - b
				y2 = a * b
				y3 = a / b
			}

			val Math = new MathBlock(FLOAT32)

			val Print = new Kernel("Print") {
				val x0 = input(FLOAT32)
				val x1 = input(FLOAT32)
				val x2 = input(FLOAT32)
				val x3 = input(FLOAT32)
				val x4 = input(FLOAT32)
				val x5 = input(FLOAT32)
                stdio.printf("Input: %g, %g\n", x0, x1)
                stdio.printf("\tAddition: %g\n", x2)
                stdio.printf("\tSubtraction: %g\n", x3)
                stdio.printf("\tMultiplication: %g\n", x4)
                stdio.printf("\tDivision: %g\n", x5)
			}

            val DupF32 = new Duplicate(FLOAT32)

			val MultiOutput = new Application {
				val random0 = DupF32(Random())
				val random1 = DupF32(Random())
				val result = Math(random0(0), random1(0))
				Print(random0(1), random1(1), result(0), result(1),
                      result(2), result(3))

			}
			MultiOutput.emit("MultiOutput")
		}
}
