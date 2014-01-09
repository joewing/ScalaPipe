
package autopipe

import autopipe.dsl.AutoPipeBlock

private[autopipe] abstract class EdgeAspect(
        val fromBlock: AutoPipeBlock,
        val toBlock:    AutoPipeBlock)

private[autopipe] class EdgeMapping(
        _fb:              AutoPipeBlock,
        _tb:              AutoPipeBlock,
        val edge:        Edge)
    extends EdgeAspect(_fb, _tb)

private[autopipe] class EdgeMeasurement(
        _fb: AutoPipeBlock,
        _tb: AutoPipeBlock,
        val stat: Symbol,
        val metric: Symbol)
    extends EdgeAspect(_fb, _tb)

