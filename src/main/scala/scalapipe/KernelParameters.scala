package scalapipe

import scalapipe.dsl.Kernel

private[scalapipe] class KernelParameter(val kernel: Kernel,
                                         val param: Symbol,
                                         val value: Any)

private[scalapipe] class KernelParameters(
        val defaults: ApplicationParameters
    ) extends Parameters {

    add(defaults)
    add('affinity, -1)      // CPU affinity (-1 for no affinity).

}
