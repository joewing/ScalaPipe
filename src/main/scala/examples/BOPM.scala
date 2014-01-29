package examples
import scalapipe.kernels.stdio

import scalapipe._
import scalapipe.dsl._

object BOPM {

	def main(args: Array[String]) {
		val count = 1000
		val n = 100

		val Stock = new Kernel("Stock") {

			val y0 = output(FLOAT32)
			val iterations = local(UNSIGNED32, count)

			val temp = local(FLOAT32, 0)
			while (iterations > 0) {
			temp = (stdio.rand() % 20000)
			temp = temp / 100
			y0 = temp
			iterations -= 1
			}
			stop
		}

		val Strike = new Kernel("Strike") {

			val y0 = output(FLOAT32)
			val iterations = local(UNSIGNED32, count)

			val temp = local(FLOAT32, 0)
			while (iterations > 0){
				temp = (stdio.rand() % 20000)
				temp = temp / 100
				y0 = temp
				iterations -= 1
			}
			stop
			
		}

		val Interest = new Kernel("Interest") {

			val y0 = output(FLOAT32)
			val iterations = local(UNSIGNED32, count)

			val temp = local(FLOAT32, 0)
			while (iterations > 0){
				temp = (stdio.rand() % 2499)
				temp += 1
				temp = temp / 10000
				y0 = temp
			}
			stop
		}

		val Dividend = new Kernel("Dividend") {

			val y0 = output(FLOAT32)
			val iterations = local(UNSIGNED32, count)

			val temp = local(FLOAT32, 0)

			while (iterations > 0){
				temp = (stdio.rand() % 1000) / 10000
				y0 = temp
			}
			stop
		}

		val Volatility = new Kernel("Volatility") {
			val y0 = output(FLOAT32)
			val iterations = local(UNSIGNED32, count)

			val temp = local(FLOAT32, 0)
			while (iterations > 0){
				temp = (stdio.rand() % 4999)
				temp += 1
				temp = temp / 10000
				y0 = temp
			}
			stop
			
		}

		val Time = new Kernel {

			val y0 = output(FLOAT32)
			val iterations = local(UNSIGNED32, count)

			val temp = local(FLOAT32, 0)
			while (iterations > 0){
				temp = stdio.rand() % 49999
				temp += 1
				temp = temp / 100000
				y0 = temp
			}
			stop
		}

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

		val Print =  new Kernel("Print") {
			val keeperofcount = local(UNSIGNED32, 0)
			val x0 = input(FLOAT32)

			stdio.printf("""%f\n""", x0)
			keeperofcount += 1
			if(keeperofcount == count) {
				stdio.printf("""Bye!\n""")
				stdio.exit(0)
			}
		}

		val PriceFinder = new Kernel("PriceFinder") {
			val x0 = input(FLOAT32) //s
			val x1 = input(FLOAT32) //k
			val x2 = input(FLOAT32) //r
			val x3 = input(FLOAT32) //q
			val x4 = input(FLOAT32) //sigma
			val x5 = input(FLOAT32) //t
			
			val y0 = output(FLOAT32) //price

			val exercise = local(FLOAT32, 0)
			val deltaT = local(FLOAT32, 0)
			val stock = local(FLOAT32, 0)
			val strike = local(FLOAT32, 0)
			val interest = local(FLOAT32, 0)
			val dividend = local(FLOAT32, 0)
			val sigma = local(FLOAT32, 0)
			val time = local(FLOAT32, 0)
			
			val up = local(FLOAT32, 0)


			val p0 = local(FLOAT32, 0)
			val p1 = local(FLOAT32, 0)
			val state = local(UNSIGNED8, 0)



			val i = local(UNSIGNED32, 0)
			val prices = local(new AutoPipeArray(FLOAT32, n))
			val j = local(SIGNED32, n - 1)

			if (state == 0 && avail(x0) && avail(x1) && avail(x2) && avail(x3) && avail(x4) && avail(x5)) {
				stock = x0
				strike = stock
				interest = x2
				dividend = x3
				sigma = x4
				time = x5
				deltaT = time / n
				up = exp(sigma * sqrt(deltaT))
				p0 = (up * exp(-interest * deltaT) - exp(-dividend * deltaT)) * up / (pow(up, 2) -1)
				p1 = exp(-interest * deltaT) - p0
				//stdio.printf("""Stock:\t%f\nStrike:\t%f\nInterest:\t%f\nDividend:\t%f\nSigma:\t%f\nTime:\t%f\n""", stock, strike, interest, dividend, sigma, time)
				state = 1
			} else if (state == 1) {
				

				while (i <= n) {
					//stdio.printf("""Stock:\t%f\nStrike:\t%f\nInterest:\t%f\nDividend:\t%f\nSigma:\t%f\nTime:\t%f\n""", stock, strike, interest, dividend, sigma, time)
					prices(i) = strike - stock * pow(up, 2*i - n)
					if(prices(i) < 0) {
						prices(i) = 0
					}
					i += 1
				}
				
					state = 2
					i = 0
					j = n - 1
				

			} else if (state == 2) {
				j = n - 1
				
				while (j >= 0) {
					i = 0
					while (i <= j) {
						//stdio.printf("""Stock:\t%f\nStrike:\t%f\nInterest:\t%f\nDividend:\t%f\nSigma:\t%f\nTime:\t%f\n""", stock, strike, interest, dividend, sigma, time)
						prices(i) = p0 * prices(i) + p1 * prices(i+1)
						exercise = strike - stock * pow(up, 2*i-j)
						if(prices(i) < exercise) {
							prices(i) = exercise
						}
						i += 1
					} 
					
					j -= 1
						
					
				}
				
					//stdio.printf("""made it\n""")
					state = 0
					j = n -1
					i = 0
					y0 = prices(0)
					
				
			}
		}

		object BOPM extends AutoPipeApp {
			param('queueDepth, count)
			val s = Stock()
			val k = Strike()
			val r = Interest()
			val q = Dividend()
			val sigma = Volatility()
			val t = Time()
			val price = PriceFinder(s,k,r,q,sigma,t)
			Print(price)

		}

		BOPM.emit("BOPM")
	}
}
