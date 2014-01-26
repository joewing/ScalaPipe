package autopipe

import autopipe.dsl.AutoPipeBlock

private[autopipe] abstract class EdgeAspect(
        val fromKernel: AutoPipeBlock,
        val toKernel:   AutoPipeBlock
    )

private[autopipe] class EdgeMapping(
        _fk:        AutoPipeBlock,
        _tk:        AutoPipeBlock,
        val edge:   Edge
    ) extends EdgeAspect(_fk, _tk)

private[autopipe] class EdgeMeasurement(
        _fk: AutoPipeBlock,
        _tk: AutoPipeBlock,
        val stat: Symbol,
        val metric: Symbol
    ) extends EdgeAspect(_fk, _tk)
