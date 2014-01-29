package examples

import scalapipe.kernels.stdio
import scalapipe.kernels.GenState
import scalapipe.kernels.MT19937
import scalapipe.kernels.ZigguratNormalFloat
import scalapipe.kernels.CholeskyDecomposition
import scalapipe.kernels.DuplicateBlock
import scalapipe.kernels.ANY_KERNEL


import scalapipe._
import scalapipe.dsl._

object BLSC {

	def main(args: Array[String]){

		val seed = 16
		val size = 4
		val mtlength = size * size
		val pi = 3.14159265359

		val Random = GenState

		val MT = MT19937

		val Ziggy = ZigguratNormalFloat

		val FloatArray = new AutoPipeArray(FLOAT32, size)

		val Cholesky = CholeskyDecomposition

		val Dup = new DuplicateBlock(FLOAT32)

		val SDup = new DuplicateBlock(UNSIGNED32)

		val SmallArray = new AutoPipeArray(FLOAT32, size)
		val BigArray = new AutoPipeArray(FLOAT32, size*size)

		

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
			ret(answer)
		}

		val abs = new Func {
			val x = input(FLOAT32)
			returns(FLOAT32)
			if(x < 0) {
				x = -x
			}
			ret(x)
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
			ret(answer)
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

			ret(answer)
		}

		val RandomCleaner = new Kernel("RandomCleaner") {
			val x0 = input(UNSIGNED32)
			val y0 = output(FLOAT32)
			val temp = local(FLOAT32)
			val lowerrange = config(FLOAT32, 'lowerrange, 0)
			val upperrange = config(FLOAT32, 'upperrange, 1)
			val resolution = config(FLOAT32, 'resolution, 100)
			val n = config(SIGNED32, 'n, 1)
			val i = local(SIGNED32, 0)
			val state = local(SIGNED32, 0)
			//val temporary = local(SIGNED32, n)
			//n = temporary
			switch(state) {
				when(0) {
					i = 0
					while(i < n){
						temp = cast(x0 % cast(resolution*(upperrange - lowerrange), UNSIGNED32), FLOAT32)
						temp = temp / resolution
						temp += lowerrange
						y0 = temp
						i += 1
					}
				}

			}
			
			//stop
		}



		

		val CorrelationMatrixGenerator = new Kernel("CorrelationMatrixGenerator") {
			/*The correlation matrix generator makes use of the method for creating
			a randomly correlated matrix descriped in the following:
			Numpacharoen K, Atsawarungruangkit A (2012) Generating Correlation Matrices Based on the Boundaries of Their Coefficients. PLoS ONE 7(11): e48902. doi:10.1371/journal.pone.0048902

			The url to the paper is: http://www.plosone.org/article/info:doi/10.1371/journal.pone.0048902

			The general idea of the correlation matrix generator is to create matrices
			of user defined sizes (n). Once the size is defined, a matrix, theta, is
			populated with numbers in the range [-pi, pi] in the lower trangle. Everything
			else (including the diagonal) is set to 0. From there, a matrix, B, is populated
			with numbers in the range [-1, 1] where each cell in the lower triangle (and
			diagonal) is assigned a number based on some combination sine and cosine of the 
			values in theta. B is then transposed and B is multiplied by BT to give the full
			correlation matrix C. C is then Cholesky decomposed.

			Example:
			
			(n = 5)

			--Theta--
			0.000	0.000	0.000	0.000	0.000	
			1.533	0.000	0.000	0.000	0.000	
			4.455	4.847	0.000	0.000	0.000	
			3.548	5.704	6.041	0.000	0.000	
			0.897	6.242	1.779	4.628	0.000	

			--B--
			1.000	0.000	0.000	0.000	0.000	
			0.038	0.999	0.000	0.000	0.000	
			-0.254	-0.130	0.958	0.000	0.000	
			-0.918	-0.331	0.210	-0.052	0.000	
			0.624	0.781	0.007	0.003	0.031	

			--Transpose B--
			1.000	0.038	-0.254	-0.918	0.624	
			0.000	0.999	-0.130	-0.331	0.781	
			0.000	0.000	0.958	0.210	0.007	
			0.000	0.000	0.000	-0.052	0.003	
			0.000	0.000	0.000	0.000	0.031	

			--C--
			1.000	0.038	-0.254	-0.918	0.624	
			0.038	1.000	-0.140	-0.366	0.804	
			-0.254	-0.140	1.000	0.478	-0.254	
			-0.918	-0.366	0.478	1.000	-0.830	
			0.624	0.804	-0.254	-0.830	1.000	

			--Cholseky Decomposed C--
			1.000	0.000	0.000	0.000	0.000	
			0.038	0.999	0.000	0.000	0.000	
			-0.254	-0.130	0.958	0.000	0.000	
			-0.918	-0.331	0.210	0.052	0.000	
			0.624	0.781	0.007	-0.003	0.031	

			*/
			val n = config(SIGNED32, 'n, 5)
			val x0 = input(UNSIGNED32)
			val y0 = output(FLOAT32)
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
					//State 0 creates theta
					if(i < n){
						if(i == 0 && j == 0) {
							//stdio.printf("""--Theta--\n""")
						} else if(j == 0) {
							//stdio.printf("""\n""")
						}
						if(j < n) {
							//stdio.printf("""i: %d j: %d\t""", i, j)
							if(i > j) {
								switcher = cast(x0 % 6283, FLOAT32) 
								//switcher = switcher % 6283
								switcher = switcher / 1000.0 
								switcher -= 3.14159
								theta(i*n+j) = switcher
							} else {
								theta(i*n+j) = 0
							}
							//stdio.printf("""%.3f\t""", theta(i*n+j))
							j += 1
						} else {
							i += 1
							j = 0
						}
					} else {
						//stdio.printf("""\n\n""")
						state = 1
						i = 0
						j = 0
						k = 0
					}
				}

				when(1) {
					//State 1 creates B
					i = 0
					while(i < n) {
						if(i == 0) {
							//stdio.printf("""--B--\n""")
						} else {
							//stdio.printf("""\n""")
						}
						j = 0
						while(j < n){
							//stdio.printf("""i: %d j: %d\t""", i, j)
							if(j == 0){
								//stdio.printf("""j is 0\n""")
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
							//stdio.printf("""%.3f\t""", B(i*n+j))
							j += 1
						}
						i += 1
					}
					//stdio.printf("""\n\n""")
					state = 2
					i = 0
					j = 0
					k = 0
					temp = 1
				}

				when(2) {
					//State 2 transposes B
					i = 0
					while(i < n) {
						if(i == 0){
							//stdio.printf("""--Transpose B--\n""")
						} else {
							//stdio.printf("""\n""")
						}
						j = 0
						while(j < n) {
							BTranspose(i*n+j) = B(j*n+i)
							//stdio.printf("""%.3f\t""", BTranspose(i*n+j))
							j += 1
						}
						i += 1
					}
					//stdio.printf("""\n\n""")
					state = 3
					i = 0
					j = 0
					k = 0
				}

				when(3) {
					//State 3 creates C
					i = 0
					while(i < n) {
						if(i == 0) {
							//stdio.printf("""--C--\n""")
						} else {
							//stdio.printf("""\n""")
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
							//stdio.printf("""%.3f\t""", C(i*n+j))
							j += 1
						}
						i += 1
					}
					//stdio.printf("""\n\n""")
					state = 4
					i = 0
					j = 0
					k = 0
				}

				when(4) {
					//State 4 sets the Cholesky matrix to 0 at all places
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
					//State 5 Cholesky decomposes C
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
					//State 6 sends out the Cholesky decomposed C
					i = 0
					while(i < n) {
						if(i == 0) {
							//stdio.printf("""--Cholesky Decomposed C--\n""")
						} else {
							//stdio.printf("""\n""")
						}
						j = 0
						while(j < n) {
							//stdio.printf("""%.3f\t""", Cholesky(i*n+j))
							y0 = Cholesky(i*n+j)
							j += 1
						}
						i += 1
					}
					stop
					//stdio.printf("""\n\n""")
					//stdio.exit(0)
				}
			}
		}

		val Transform = new Kernel("transform") {
			val n = config(SIGNED32, 'n, 5)
			val x0 = input(FLOAT32)
			val x1 = input(FLOAT32)
			val y0 = output(FLOAT32)
			val corrMatrix = local(BigArray)
			val corrMatrixV = local(SmallArray)
			val vector = local(SmallArray)
			val corrVector = local(SmallArray)
			val state = local(SIGNED32, -1)
			val i = local(SIGNED32, 0)
			val j = local(SIGNED32, 0)
			val k = local(SIGNED32, 0)
			val temp = local(FLOAT32, 0)

			switch(state) {

				when(-1) {
					//State -1 takes in the lower right triangle matrix
					//(i.e. the Cholesky decomposed C)
					if(j == 0) {
						if(i == 0) {
							//stdio.printf("""--Cholesky--""")
						}
						//stdio.printf("""\n""")
					}
					if(i < n) {
						//stdio.printf("""i: %d j: %d\n""", i, j)
						if(j < n) {
							//stdio.printf("""j: %d i: %d\n""", j, i)
							corrMatrix(i*n+j) = x0
							//stdio.printf("""%.3f\t""", corrMatrix(i*n+j))
							j += 1
						} else {
							j = 0
							i += 1
						}						
					} else {
						state = 0
						i = 0
						j = 0
						//stdio.printf("""\n\n""")
					}
					
				}

				when(0) {
					if(i == 0) {
						//stdio.printf("""--Vector--\n""")
					}
					if(i < n) {
						vector(i) = x1
						//stdio.printf("""%.3f\t""", vector(i))
						i += 1
					} else {
						i = 0
						state = 1
						//stdio.printf("""\n\n""")
					}
					
				}

				when(1) {
					i = 0
					j = 0
					k = 0
					//stdio.printf("""--Correlated Vector--\n""")
					while(i < n) {
						k = 0
						while(k < n) {
							corrMatrixV(k) = corrMatrix(i*n+k)
							k += 1
						}
						temp = 0
						k = 0
						while(k < n) {
							temp += corrMatrixV(k) * vector(k)
							k += 1
						}
						corrVector(i) = temp
						if(corrVector(i) < 0) {
							corrVector(i) *= -1
						}
						//stdio.printf("""%.3f\t""", corrVector(i))
						i += 1
					}
					i = 0
					k = 0
					state = 2
					//stdio.printf("""\n\n""")
					//stdio.exit(0)
				}

				when(2) {
					i = 0
					while(i < n) {
						y0 = corrVector(i)
						i += 1
					}
					state = 0
					i = 0
					//stop
				}
			}
		}

		val BlackScholes = new Kernel("BlackScholes") {
			val n = config(SIGNED32, 'n, 5)
			val x0 = input(FLOAT32)
			val x1 = input(FLOAT32)
			val x2 = input(FLOAT32)
			val x3 = input(FLOAT32)
			val x4 = input(FLOAT32)
			val stocks = local(SmallArray) 	//x0
			val spot = local(SmallArray) 	//x1
			val vol = local(SmallArray)		//x2
			val r = local(FLOAT32, .1)		//x3
			val t = local(FLOAT32, .5)		//x4
			val price = local(SmallArray)
			val state = local(SIGNED32, 0)
			val i = local(UNSIGNED32, 0)
			val temp = local(FLOAT32, 0)
			val counter = local(UNSIGNED32, 0)
			val runcount = local(UNSIGNED32, 10)
			val dummy = local(FLOAT32, 0)

			switch(state) {

				when(0) {
					
					if(i < n) {
						stocks(i) = x0
						spot(i) = x1
						vol(i) = x2
						if(i == 0) {
							//stdio.printf("""--Stocks--\n""")
						}
						//stdio.printf("""%.3f\t""", stocks(i))
						i += 1
					} else {
						state = 1
						//stdio.printf("""\n\n""")
						//stdio.exit(0)
					}
				}

				when(1) {
					r = x3
					t = x4
					state = 2
				}

				when(2) {
					i = 0
					stdio.printf("""--Stocks--\n""")
					while(i < n) {
						stdio.printf("""%.3f\t""", stocks(i))
						i += 1
					}
					stdio.printf("""\n""")
					i = 0
					stdio.printf("""\n--Spot Prices--\n""")
					while(i < n) {
						stdio.printf("""%.3f\t""", spot(i))
						i += 1
					}
					stdio.printf("""\n""")
					i = 0
					stdio.printf("""\n--Volatilities--\n""")
					while(i < n) {
						stdio.printf("""%.3f\t""", vol(i))
						i += 1
					}
					stdio.printf("""\n""")
					stdio.printf("""\n--Interest Rate--\n%f\n\n""", r)
					stdio.printf("""--Time--\n%.3f\n\n""", t)
					state = 3
					//stdio.exit(0)
				}

				when(3) {
					//stdio.printf("""State 3\n""")
					i = 0
					temp = 0
					stdio.printf("""--Prices--\n""")
					while(i < n) {
						temp = spot(i) * exp((r - vol(i) * vol(i) / 2) * t + vol(i) * sqrt(t) * stocks(i)) - spot(i)
						price(i) = temp
						stdio.printf("""price(%d): %f\n""", i, price(i))
						i += 1
						
					}
					counter += 1
					stdio.printf("""\nCounter:\t%d\n""", counter)
					i = 0
					state = 4
					//stdio.exit(0)
				}

				when(4) {
					//stdio.printf("""State 4\n""")
					runcount -= 1
					stdio.printf("""runcount = %d\n""", runcount)
					if(runcount == 0) {
						stdio.exit(0)
					}
					if(i < n) {
						if(avail(x0) && avail(x1) && avail(x2) && avail(x3) && avail(x4)) {
							stocks(i) = x0
							dummy = x1
							dummy = x2
							dummy = x3
							dummy = x4
							i += 1
							runcount = 100
						}	
					} else {
						state = 3
					}
				}
			}
		}
			
			
		

		object BLSC extends Application {

			param('fpga, "Simulation")

			val random = Random('seed -> seed)
			val twister = SDup(MT(random, 'iterations -> mtlength))
			val ziggy = ZigguratNormalFloat(twister(0))
			val matrix = CorrelationMatrixGenerator(twister(1), 'n -> size)
			val trans = Transform(matrix, ziggy, 'n -> size)
			val spotR = Random('seed -> seed)
			val spotT = MT(spotR, 'iterations -> mtlength)
			val spotC = RandomCleaner(spotT, 'n -> size, 'upperrange -> 100) 
			val volR = Random('seed -> seed)
			val volT = MT(volR, 'iterations -> mtlength)
			val volC = RandomCleaner(volT, 'n -> size, 'upperrange -> .5, 'resolution -> 1000)
			val rR = Random('seed -> seed)
			val rT = MT(rR, 'iterations -> mtlength)
			val rC = RandomCleaner(rT, 'n -> 1, 'upperrange -> .1, 'lowerrange -> .001, 'resolution -> 10000)
			val tR = Random('seed -> seed)
			val tT = MT(tR, 'iterations -> mtlength)
			val tC = RandomCleaner(tT, 'n -> 1, 'upperrange -> 2, 'lowerrange -> .01, 'resolution -> 10000)
			val black = BlackScholes(trans, spotC, volC, rC, tC, 'n -> size)

			map(ANY_KERNEL -> Transform, CPU2FPGA())
			map(Transform -> ANY_KERNEL, FPGA2CPU())
			//map(ANY_KERNEL -> CorrelationMatrixGenerator, CPU2FPGA())
			//map(CorrelationMatrixGenerator -> Transform, FPGA2FPGA())

		}

		BLSC.emit("BLSC")
	}
}
