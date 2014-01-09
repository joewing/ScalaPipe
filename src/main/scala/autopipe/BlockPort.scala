
package autopipe

import autopipe.dsl.AutoPipeType

private[autopipe] abstract class BlockPort(  
        val name: String,
        val t: AutoPipeType)

private[autopipe] class BlockInput(
        _name: String,
        _t: AutoPipeType)
    extends BlockPort(_name, _t)

private[autopipe] class BlockOutput(
        _name: String,
        _t: AutoPipeType)
    extends BlockPort(_name, _t)

