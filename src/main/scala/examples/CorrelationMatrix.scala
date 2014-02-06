package examples

import scalapipe.kernels.MT19937
import scalapipe.kernels.stdio
import scalapipe.kernels.MT19937State
import scalapipe._
import scalapipe.dsl._



object CorrelationMatrix {


	def main(args: Array[String]) {
		val pi = 3.14159265359
		val n = 5
		val seed = 15
		val size = 3
		val mtlength = 25

		val SmallArray = Vector(FLOAT32, n)
		val BigArray = Vector(FLOAT32, n*n)

		val pow = new Func {
			val x = input(FLOAT32)
			val y = input(SIGNED32)
			val counter = local(SIGNED32, 0)
			returns(FLOAT32)

			val answer = local(FLOAT32, 1)
			if(y > 0){
				while (counter < y) {
					answer = answer * x
					counter += 1
				}
			} else if (y < 0) {
				while (counter > y) {
					answer = answer / x
					counter -= 1
				}
			}
			return answer
		}

		val abs = new Func {
			val x = input(FLOAT32)
			returns(FLOAT32)
			if(x < 0) {
				x = -x
			}
			return x
		}

		val sin = new Func {
			val x = input(FLOAT32)
			returns(FLOAT32)

			val answer = local(FLOAT32, 0)
			val y = local(FLOAT32, 0)
			y = abs(x)
			answer = (16 * y * (pi - y))/(5 * pi * pi - 4 * y * (pi - y))
			if(x < 0) {
				answer *= -1
			}
			return answer
		}

		val cos = new Func {
			val x = input(FLOAT32)
			returns(FLOAT32)

			val z = local(FLOAT32, 0)
			val y = local(FLOAT32, 0) 
			y = x
			val answer = local(FLOAT32, 0)
			y += (pi / 2)
			if(y > pi) {
				y -= 2 * pi
			}
			z = abs(y)
			answer = (16 * z * (pi - z))/(5 * pi * pi - 4 * z * (pi - z))
			if(x < 0) {
				answer *= -1
			}

			return answer
		}


		val CorrelationMatrixGenerator = new Kernel("CorrelationMatrixGenerator") {
			val n = config(SIGNED32, 'n, 5)
			val x0 = input(UNSIGNED32)
			val switcher = local(FLOAT32)
			val state = local(SIGNED32, 0)

			val B = local(BigArray)
			val theta = local(BigArray)
			val BTranspose = local(BigArray)
			val C = local(BigArray)
			val Cholesky = local(BigArray)
			val tempA = local(SmallArray)
			val tempB = local(SmallArray)

			val i = local(SIGNED32, 0)
			val j = local(SIGNED32, 0)
			val k = local(SIGNED32, 0)
			val temp = local(FLOAT32, 1)
			val s = local(FLOAT32, 0)

			switch(state) {

				when(0) {

					if(i < n){
						if(i == 0 && j == 0) {
							stdio.printf("--Theta--\n")
						} else if(j == 0) {
							stdio.printf("\n")
						}
						if(j < n) {
							//stdio.printf("i: %d j: %d\t", i, j)
							if(i > j) {
								switcher = cast(x0 % 6283, FLOAT32) 
								//switcher = switcher % 6283
								switcher = switcher / 1000.0 
								switcher -= 3.14159
								theta(i*n+j) = switcher
							} else {
								theta(i*n+j) = 0
							}
							stdio.printf("%.3f\t", theta(i*n+j))
							j += 1
						} else {
							i += 1
							j = 0
						}
					} else {
						stdio.printf("\n\n")
						state = 1
						i = 0
						j = 0
						k = 0
					}
				}

				when(1) {
					i = 0
					while(i < n) {
						if(i == 0) {
							//stdio.printf("--B--\n")
						} else {
							//stdio.printf("\n")
						}
						j = 0
						while(j < n){
							//stdio.printf("i: %d j: %d\t", i, j)
							if(j == 0){
								//stdio.printf("j is 0\n")
								if(i == 0){
									B(i*n+j) = 1
								} else {
									B(i*n+j) = cos(theta(i*n+j))
								}
							} else if(i == j) {
								temp = 1
								k = 0
								while(k <= j - 1) {
									temp *= sin(theta(i*n+k))
									k += 1
								}
								B(i*n+j) = temp
							} else if(j >= 1 && j <= i - 1) {
								temp = 1
								k = 0
								while(k <= j - 1) {
									temp *= sin(theta(i*n+k))
									k += 1
								}
								B(i*n+j) = cos(theta(i*n+j)) * temp
							} else {
								B(i*n+j) = 0
							}
							//stdio.printf("%.3f\t", B(i*n+j))
							j += 1
						}
						i += 1
					}
					//stdio.printf("\n\n")
					state = 2
					i = 0
					j = 0
					k = 0
					temp = 1
				}

				when(2) {
					i = 0
					while(i < n) {
						if(i == 0){
							//stdio.printf("--Transpose B--\n")
						} else {
							//stdio.printf("\n")
						}
						j = 0
						while(j < n) {
							BTranspose(i*n+j) = B(j*n+i)
							//stdio.printf("%.3f\t", BTranspose(i*n+j))
							j += 1
						}
						i += 1
					}
					//stdio.printf("\n\n")
					state = 3
					i = 0
					j = 0
					k = 0
				}

				when(3) {
					i = 0
					while(i < n) {
						if(i == 0) {
							//stdio.printf("--C--\n")
						} else {
							//stdio.printf("\n")
						}
						j = 0
						while(j < n) {
							k = 0
							while(k < n) {
								tempA(k) = B(i*n+k)
								tempB(k) = BTranspose(k*n+j)
								k += 1
							}
							temp = 0
							k = 0
							while(k < n) {
								temp += tempA(k) * tempB(k)
								k += 1
							}
							C(i*n+j) = temp
							//stdio.printf("%.3f\t", C(i*n+j))
							j += 1
						}
						i += 1
					}
					//stdio.printf("\n\n")
					state = 4
					i = 0
					j = 0
					k = 0
				}

				when(4) {
					i = 0
					while(i < n) {
						j = 0
						while(j < n) {
							Cholesky(i*n+j) = 0
							j += 1
						}
						i += 1
					}
					i = 0
					j = 0
					k = 0
					state = 5
				}

				when(5) {
					i = 0
					while(i < n) {
						j = 0
						while(j < (i+1)) {
							s = 0
							k = 0
							while(k < j) {
								s += Cholesky(i*n+k) * Cholesky(j*n+k)
								k += 1
							}
							if(i == j) {
								Cholesky(i*n+j) = sqrt(C(i*n+i) - s)
							} else {
								Cholesky(i*n+j) = (1.0/Cholesky(j*n+j)*(C(i*n+j) - s))
							}
							j += 1
						}
						i += 1
					}
					state = 6
					i = 0
					j = 0
					k = 0
				}

				when(6) {
					i = 0
					while(i < n) {
						if(i == 0) {
							stdio.printf("--Cholesky Decomposed C--\n")
						} else {
							stdio.printf("\n")
						}
						j = 0
						while(j < n) {
							stdio.printf("%.3f\t", Cholesky(i*n+j))
							j += 1
						}
						i += 1
					}
					stdio.printf("\n\n")
					stdio.exit(0)
				}
			}
		}

		/*val Randeaux = new Kernel("Rand") {
			val y0 = output(UNSIGNED32)
			val n = config(SIGNED32, 'n, 5)
			val temp = local(FLOAT32, 0)
			val iterations = local(UNSIGNED32, 0)
			val i = local(SIGNED32)
			i = n
			while(i > 0) {
				//stdio.printf("iterations: %d\n", iterations)
				iterations += (i - 1)
				i -= 1
			}
			while(iterations > 0) {
				temp = stdio.rand()
				y0 = temp
				iterations -= 1
			}
			stop
		}*/

		val Random = MT19937State
		val MT = MT19937

		object CorrelationMatrix extends Application {

			val random = Random('seed -> seed)
			val twister = MT(random, 'iterations -> mtlength)
			val corr = CorrelationMatrixGenerator(twister, 'n -> n)
		}

		CorrelationMatrix.emit("CorrelationMatrix")

	}
}
