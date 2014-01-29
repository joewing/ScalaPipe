package scalapipe

import scalapipe.dsl.Kernel

private[scalapipe] abstract class EdgeAspect(
        val fromKernel: Kernel,
        val toKernel:   Kernel
    )

private[scalapipe] class EdgeMapping(
        _fk:        Kernel,
        _tk:        Kernel,
        val edge:   Edge
    ) extends EdgeAspect(_fk, _tk)

private[scalapipe] class EdgeMeasurement(
        _fk: Kernel,
        _tk: Kernel,
        val stat: Symbol,
        val metric: Symbol
    ) extends EdgeAspect(_fk, _tk)
