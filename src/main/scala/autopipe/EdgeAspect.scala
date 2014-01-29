package autopipe

import autopipe.dsl.Kernel

private[autopipe] abstract class EdgeAspect(
        val fromKernel: Kernel,
        val toKernel:   Kernel
    )

private[autopipe] class EdgeMapping(
        _fk:        Kernel,
        _tk:        Kernel,
        val edge:   Edge
    ) extends EdgeAspect(_fk, _tk)

private[autopipe] class EdgeMeasurement(
        _fk: Kernel,
        _tk: Kernel,
        val stat: Symbol,
        val metric: Symbol
    ) extends EdgeAspect(_fk, _tk)
