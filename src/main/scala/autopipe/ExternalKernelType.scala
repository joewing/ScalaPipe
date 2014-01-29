package autopipe

import autopipe.dsl.Kernel
import java.io.File

private[autopipe] class ExternalKernelType(
        ap: AutoPipe,
        kernel: Kernel,
        p: Platforms.Value
    ) extends KernelType(ap, kernel, p) {

    override def internal = false

    override def emit(dir: File) {
    }

}
