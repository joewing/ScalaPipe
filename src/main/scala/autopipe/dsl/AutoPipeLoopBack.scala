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

    private[autopipe] var kernel: Kernel = null

    def input(s: Stream) {
        kernel.setInput(null, s)
    }

    def output()(implicit ap: AutoPipe): Stream = {
        kernel = ap.createKernel(apBlock)
        kernel.apply()()
    }

}
