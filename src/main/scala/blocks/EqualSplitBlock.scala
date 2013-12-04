
package blocks

import autopipe.dsl._

class EqualSplitBlock(t: AutoPipeType, n: Int = 2) extends AutoPipeBlock {

   val x = input(t)

   for (i <- 0 until n) {
      val y = output(t)
      y = x
   }

}

