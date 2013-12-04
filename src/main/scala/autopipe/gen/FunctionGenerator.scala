
package autopipe.gen

import autopipe._
import java.io.File

private[autopipe] abstract class FunctionGenerator(val ft: InternalFunctionType)
      extends Generator {

   def emit(dir: File): Unit

}

