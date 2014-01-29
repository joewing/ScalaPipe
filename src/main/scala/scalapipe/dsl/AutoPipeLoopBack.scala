package scalapipe.dsl

import scalapipe._

object AutoPipeLoopBack {
    def apply(t: AutoPipeType) = new AutoPipeLoopBack(t)
}

class AutoPipeLoopBack(val t: AutoPipeType) {

    private[scalapipe] var stream: Stream = null

    private[scalapipe] val kernel = new Kernel {
        setLoopBack
        val in = input(t)
        val out = output(t)
        out = in
    }

    private[scalapipe] var instance: KernelInstance = null

    def input(s: Stream) {
        instance.setInput(null, s)
    }

    def output()(implicit sp: ScalaPipe): Stream = {
        instance = sp.createInstance(kernel)
        instance.apply()()
    }

}
