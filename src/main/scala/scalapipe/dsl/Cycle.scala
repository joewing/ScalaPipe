package scalapipe.dsl

import scalapipe.{ScalaPipe, Stream, Error}

object Cycle {
    def apply() = new Cycle
}

class Cycle {

    private var stream: Stream = null

    private[dsl] def output(sp: ScalaPipe): Stream = {
        if (stream != null) {
            Error.raise("cycle connected multiple times")
        }
        stream = new Stream(sp, null, null)
        stream
    }

    def apply(s: Stream) {
        if (stream == null) {
            Error.raise("unconnected cycle")
        } else {
            stream.destKernel.replaceInput(stream, s)
            s.setDest(stream.destKernel, stream.destPort)
            stream = null
        }
    }

}
