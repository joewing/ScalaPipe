
package examples

import blocks._
import autopipe._
import autopipe.dsl._

object Temp {

    def main(args: Array[String]) {

        val vtype = UNSIGNED32

        val values = (0 to 1).map(i => i + i)
        val mapping = new ROM(vtype, values)

        val Gen = new AutoPipeBlock {
            val out = output(vtype)
            out = 5
            stop
        }

        val Update = new AutoPipeBlock {
            val n         = input(vtype)
            val result  = output(vtype)

            val i = local(vtype)
            val last = local(vtype)
            val current = local(vtype)
            val temp = local(vtype)

            i = n
            last = 1
            current = 0
            while (i > 0) {
                temp = current
                current += last
                last = temp
                i -= 1
            }
            result = current

        }

        val Print = new AutoPipeBlock {

            val in = input(vtype)

            stdio.printf("""%lg\n""", cast(in, FLOAT64))

        }

        val Temp = new AutoPipeApp {

            val source = Gen()
            val result = Update(source)
            Print(result)

            map(ANY_BLOCK -> Print, FPGA2CPU())

        }
        Temp.emit("temp")

    }

}

