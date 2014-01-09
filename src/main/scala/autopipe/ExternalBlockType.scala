
package autopipe

import autopipe.dsl.AutoPipeBlock
import java.io.File

private[autopipe] class ExternalBlockType(ap: AutoPipe,
                                                        apb: AutoPipeBlock,
                                                        p: Platforms.Value)
        extends BlockType(ap, apb, p) {

    override def internal = false

    override def emit(dir: File) {
    }

    override def run(i: BlockInterface) {
    }

}

