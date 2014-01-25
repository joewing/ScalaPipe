package autopipe.gen

import java.io.File
import autopipe.InternalKernelType

private[autopipe] abstract class KernelGenerator(
        val kt: InternalKernelType
    ) extends Generator {

    def emit(dir: File): Unit

}
