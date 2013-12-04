
package autopipe.gen

import autopipe._
import java.io.File

private[autopipe] abstract class ResourceGenerator extends Generator {

   def getRules: String

   def emit(dir: File): Unit

}

