package blocks

import scalapipe._
import scalapipe.dsl._

object CholeskyDecomposition extends Kernel {

	val size = 3
	val x0 = input(FLOAT32)
	val y0 = output(FLOAT32)
	val inputMatrix = local(new AutoPipeArray(FLOAT32, size * size))
	val outputMatrix = local(new AutoPipeArray(FLOAT32, size * size))
	val state = local(SIGNED32, -1)
	val i = local(SIGNED32, 0)
	val j = local(SIGNED32, 0)
	val k = local(SIGNED32, 0)

	switch(state) {

		when(-1) {
			inputMatrix(i) = 0
			outputMatrix(i) = 0
			if(i < size * size - 1) {
				i += 1
			} else {
				i = 0
				state = 1
			}
		}

		when(1) {
			inputMatrix(i) = x0
			if(i < size * size -1) {
				i += 1
			} else {
				i = 0
				state = 2
			}
		}

		when(2) {
			val temp = local(FLOAT32, 0)
			while(i < size) {
				j = 0
				while(j < (i + 1)) {
					k = 0
					temp = 0
					while(k < j) {
						temp += outputMatrix(i * size + k) * outputMatrix(j * size + k)
						k += 1
					}
					if(i == j) {
							outputMatrix(i * size + j) = sqrt(inputMatrix(i * size + i) - temp)
					} else {
							outputMatrix(i * size + j) = 1.0/outputMatrix(j * size + j) * (inputMatrix(i * size + j) - temp)
					}

					j += 1

				}
				i += 1
			}

			i = 0
			j = 0
			k = 0
			state = 3
		}

		when(3) {
			y0 = outputMatrix(i)
			if(i < size * size - 1){
				i += 1
			} else {
				i = 0
				state = -1
			}
		}
	}

}
