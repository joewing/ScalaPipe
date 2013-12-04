package examples

import blocks.stdio

import autopipe._
import autopipe.dsl._

object MultiOutput {

		val count = 10

		def main(args: Array[String]) {

			val Random = new AutoPipeBlock("Random") {
				val y0 = output(FLOAT32)
				val iterations = local(UNSIGNED32, count)

				val temp = local(FLOAT32, 0)
				while (iterations > 0) {
					temp = stdio.rand() % 10000
					temp = temp / 100.0
					y0 = temp
					iterations -= 1
				}
				stop
			}

			class MathBlock(t: AutoPipeType) extends AutoPipeBlock("MathBlock") {

				val x0 = input(t)
				val x1 = input(t)
				val y0 = output(t)
				val y1 = output(t)
				val y2 = output(t)
				val y3 = output(t)

				y0 = x0 + x1
				y1 = x0 - x1
				y2 = x0 * x1
				y3 = x0 / x1
			}

			val Math = new MathBlock(FLOAT32)

			val Print = new AutoPipeBlock("Print") {
				val x0 = input(FLOAT32)
				val x1 = input(FLOAT32)
				val x2 = input(FLOAT32)
				val x3 = input(FLOAT32)
				val x4 = input(FLOAT32)
				val x5 = input(FLOAT32)

				stdio.printf("""Number 1:\t%f\nNumber 2:\t%f\nAddition:\t%f\nSubtraction:\t%f\nMultiplication:\t%f\nDivision:\t%f\n""", x0, x1, x2, x3, x4, x5)
				
			}

			object MultiOutput extends AutoPipeApp {

				
				val random0 = Random()
				val random1 = Random()
				val result = Math(random0, random1)
				Print(random0, random1, result)

			}

			MultiOutput.emit("MultiOutput")

		}
}