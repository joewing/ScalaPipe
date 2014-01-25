package autopipe

import autopipe.dsl.AutoPipeFunction

private[autopipe] class ExternalFunctionType(
        ap: AutoPipe,
        apf: AutoPipeFunction,
        p: Platforms.Value
    ) extends ExternalKernelType(ap, apf, p)
