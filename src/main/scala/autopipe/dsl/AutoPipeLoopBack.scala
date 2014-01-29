package autopipe.dsl

import autopipe._

object AutoPipeLoopBack {
    def apply(t: AutoPipeType) = new AutoPipeLoopBack(t)
}

class AutoPipeLoopBack(val t: AutoPipeType) {

    private[autopipe] var stream: Stream = null

    private[autopipe] val kernel = new Kernel {
        setLoopBack
        val in = input(t)
        val out = output(t)
        out = in
    }

    private[autopipe] var instance: KernelInstance = null

    def input(s: Stream) {
        instance.setInput(null, s)
    }

    def output()(implicit ap: AutoPipe): Stream = {
        instance = ap.createInstance(kernel)
        instance.apply()()
    }

}
