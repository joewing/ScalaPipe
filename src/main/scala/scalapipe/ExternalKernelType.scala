package scalapipe

import scalapipe.dsl.Kernel
import java.io.File

private[scalapipe] class ExternalKernelType(
        sp: ScalaPipe,
        kernel: Kernel,
        p: Platforms.Value
    ) extends KernelType(sp, kernel, p) {

    override def internal = false

    override def pure = true

    override def emit(dir: File) {
    }

}
