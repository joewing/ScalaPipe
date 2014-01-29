package autopipe

import autopipe.dsl.Func

private[autopipe] class ExternalFunctionType(
        ap: AutoPipe,
        func: Func,
        p: Platforms.Value
    ) extends ExternalKernelType(ap, func, p)
