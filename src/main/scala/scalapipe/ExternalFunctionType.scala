package scalapipe

import scalapipe.dsl.Func

private[scalapipe] class ExternalFunctionType(
        sp: ScalaPipe,
        func: Func,
        p: Platforms.Value
    ) extends ExternalKernelType(sp, func, p)
