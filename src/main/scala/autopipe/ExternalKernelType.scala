package autopipe

import autopipe.dsl.AutoPipeBlock
import java.io.File

private[autopipe] class ExternalKernelType(
        ap: AutoPipe,
        apb: AutoPipeBlock,
        p: Platforms.Value
    ) extends KernelType(ap, apb, p) {

    override def internal = false

    override def emit(dir: File) {
    }

}
