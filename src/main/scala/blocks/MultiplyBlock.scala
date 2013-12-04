
package blocks

import autopipe.dsl._

class MultiplyBlock(t: AutoPipeType) extends AutoPipeBlock {

   val x0 = input(t)
   val x1 = input(t)
   val y0 = output(t)

   y0 = x0 * x1

}

