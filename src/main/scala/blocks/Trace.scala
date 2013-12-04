
package blocks

import autopipe.dsl._

object TraceF64 extends AutoPipeBlock {

   val in = input(FLOAT64)
   val out = output(FLOAT64)
   val temp = local(FLOAT64)
   val msg = config(STRING, 'msg, "TraceF64")

   temp = in
   stdio.printf("""%s: %lf\n""", msg, temp)
   out = temp

}

