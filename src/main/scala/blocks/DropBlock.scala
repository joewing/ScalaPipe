
package blocks

import autopipe.dsl._

class DropBlock(t: AutoPipeType) extends AutoPipeBlock {

   val i = input(t)
   val temp = local(t)

   temp = i

}

