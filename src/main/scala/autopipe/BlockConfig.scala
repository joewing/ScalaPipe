
package autopipe

import autopipe.dsl.AutoPipeType

private[autopipe] class BlockConfig(
      val name: String,
      val t: AutoPipeType,
      val default: Any)

