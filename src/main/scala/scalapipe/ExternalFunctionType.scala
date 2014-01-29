package scalapipe

import scalapipe.dsl.Func

private[scalapipe] class ExternalFunctionType(
        ap: AutoPipe,
        func: Func,
        p: Platforms.Value
    ) extends ExternalKernelType(ap, func, p)
