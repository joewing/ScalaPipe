
package blocks

import autopipe.dsl._

class PrioritySplitBlock(t: AutoPipeType) extends AutoPipeBlock {

   val x0 = input(t)
   val y0 = output(t)
   val y1 = output(t)

   if (avail(y0)) {
      y0 = x0
   } else if (avail(y1)) {
      y1 = x0
   }

}

