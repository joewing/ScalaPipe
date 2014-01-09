
package autopipe.dsl

import autopipe._

object AutoPipeLoopBack {
    def apply(t: AutoPipeType) = new AutoPipeLoopBack(t)
}

class AutoPipeLoopBack(val t: AutoPipeType) {

    private[autopipe] var stream: Stream = null

    private[autopipe] val apBlock = new AutoPipeBlock {
        setLoopBack
        val in = input(t)
        val out = output(t)
        out = in
    }

    private[autopipe] var block: Block = null

    def input(s: Stream) {
        block.setInput(null, s)
    }

    def output()(implicit ap: AutoPipe): Stream = {
        block = ap.createBlock(apBlock)
        block.apply()()
    }

}

