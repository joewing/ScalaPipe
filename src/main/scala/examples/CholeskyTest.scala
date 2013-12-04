package examples

import blocks.stdio
import blocks.DuplicateBlock
import blocks.CholeskyDecomposition

import autopipe._
import autopipe.dsl._





object CholeskyTest {

	def main(args: Array[String]) {

		val arraysize = 4

		val InputMatrix = new AutoPipeBlock("InputMatrix") {
			val size = 3
			val matrix = local(new AutoPipeArray(FLOAT32, 9))
			val state = local(SIGNED32, 0)
			val i = local(SIGNED32, 0)

			val y0 = output(FLOAT32)

			switch(state) {
				when(0) {
					if(size == 3) {
						matrix(0) = 1
						matrix(1) = .6
						matrix(2) = .3
						matrix(3) = .6
						matrix(4) = 1
						matrix(5) = .5
						matrix(6) = .3
						matrix(7) = .5
						matrix(8) = 1
					} else if(size == 4) {
						matrix(0) = 1
						matrix(1) = .6
						matrix(2) = .3
						matrix(3) = .6
						matrix(4) = 1
						matrix(5) = .5
						matrix(6) = .3
						matrix(7) = .5
						matrix(8) = 1
					}

				}
				when(1) {
					y0 = matrix(i)
					if(i < size * size - 1) {
						i += 1
					} else {
						stop
					}
				}
			}
			
		}

		val Cholesky = CholeskyDecomposition

		val Print = new AutoPipeBlock("Print") {

			val size = 4
			val i = local(SIGNED32, 0)
			val state = local(SIGNED32, 0)

			val x0 = input(FLOAT32)
			val x1 = input(FLOAT32)
			val temp = local(FLOAT32, 0)

			switch(state) {
				when(0) {
					temp = x0
					if(i == 0) {
						stdio.printf("""--Input Matrix---\n""")
					}
					stdio.printf("""%f\t""", temp)
					if((i+1) % size == 0) {
						stdio.printf("""\n""")
					}
					if(i == (size * size - 1)) {
						i = 0
						state = 1
					} else {
						i += 1
					}
				}

				when(1) {
					temp = x1
					if(i == 0) {
						stdio.printf("""--Cholesky Decomposed Matrix--\n""")
					}
					stdio.printf("""%f\t""", temp)
					if((i+1) % size == 0) {
						stdio.printf("""\n""")
					}
					if(i == (size * size - 1)) {
						stop
					} else {
						i += 1
					}
				}
			}
			
				
		}

		
		val Dup = new DuplicateBlock(FLOAT32)
		

		object CholeskyTest extends AutoPipeApp {

			val iMatrix = Dup(InputMatrix('size -> arraysize))
			//val printiMatrix = Print2(iMatrix)
			val oMatrix = Cholesky(iMatrix(0), 'size -> arraysize)
			Print(iMatrix(1), oMatrix, 'size -> arraysize)

		}

		CholeskyTest.emit("CholeskyTest")


		
	}
}

