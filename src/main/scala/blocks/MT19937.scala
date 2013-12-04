
package blocks

import autopipe.dsl._

object MT19937 extends AutoPipeBlock {

   val state = input(UNSIGNED32)
   val out = output(UNSIGNED32)

   val mt = local(new AutoPipeArray(UNSIGNED32, 624))
   val index = local(UNSIGNED32, 0)
   val configured = local(UNSIGNED8, 0)
   val i = local(UNSIGNED32)
   val j = local(UNSIGNED32)
   val y = local(UNSIGNED32)

   if (configured && index < 624) {

      // Extract a tempered number.
      y = mt(index)
      y ^= (y >> 11)
      y ^= ((y << 7) & 0x9d2c5680)
      y ^= ((y << 15) & 0xefc60000)
      y ^= (y >> 18)
      index += 1
      out = y

   } else {

      // Generate the array of untempered numbers.
      if (configured) {
         i = 0
         while (i < 624) {
            j = i + 1
            if (j == 624) {
               j = 0
            }
            y = mt(i) >> 31
            y += mt(j) & 0x7FFFFFFF
            j = i + 397
            if (j > 623) {
               j -= 624
            }
            mt(i) = mt(j) ^ (y >> 1)
            if (y & 1) {
               mt(i) ^= 0x9908b0df
            }
            i += 1
         }
         index = 0
      } else {
         mt(index) = state
         index += 1
         configured = index == 624
      }

   }

}

