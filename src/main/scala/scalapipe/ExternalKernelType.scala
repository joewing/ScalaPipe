package scalapipe

import scalapipe.dsl.Kernel
import java.io.File

private[scalapipe] class ExternalKernelType(
        ap: AutoPipe,
        kernel: Kernel,
        p: Platforms.Value
    ) extends KernelType(ap, kernel, p) {

    override def internal = false

    override def emit(dir: File) {
    }

}
