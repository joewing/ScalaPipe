package scalapipe.dsl

import scalapipe._

object Cycle {
    def apply(t: Type) = new Cycle(t)
}

class Cycle(val t: Type) {

    private var stream: Stream = null

    private val kernel = new Kernel {
        val in = input(t)
        val out = output(t)
        out = in
    }

    private var instance: KernelInstance = null

    private[dsl] def output(sp: ScalaPipe): Stream = {
        instance = sp.createInstance(kernel)
        instance.apply()()
    }

    def apply(s: Stream) {
        if (instance == null) {
            Error.raise("unconnected cycle")
        } else {
            instance.setInput(null, s)
        }
    }

}
