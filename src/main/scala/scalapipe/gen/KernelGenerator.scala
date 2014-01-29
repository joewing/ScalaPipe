package scalapipe.gen

import java.io.File
import scalapipe.InternalKernelType

private[scalapipe] abstract class KernelGenerator(
        val kt: InternalKernelType
    ) extends Generator {

    def emit(dir: File): Unit

}
