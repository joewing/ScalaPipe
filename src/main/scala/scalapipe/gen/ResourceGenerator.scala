
package scalapipe.gen

import scalapipe._
import java.io.File

private[scalapipe] abstract class ResourceGenerator extends Generator {

    def getRules: String

    def emit(dir: File): Unit

}
