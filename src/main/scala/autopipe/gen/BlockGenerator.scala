
package autopipe.gen

import java.io.File
import autopipe._

private[autopipe] abstract class BlockGenerator(val bt: InternalBlockType)
   extends Generator {

   def emit(dir: File): Unit

}

