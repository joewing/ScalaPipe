package scalapipe.gen

import scalapipe._

private[scalapipe] class SaturnEdgeGenerator(
        val sp: ScalaPipe
    ) extends EdgeGenerator(Platforms.HDL) {

    override def emitCommon() {
    }

    override def emitGlobals(streams: Traversable[Stream]) {
    }

    override def emitInit(streams: Traversable[Stream]) {
    }

    override def emitDestroy(streams: Traversable[Stream]) {
    }

}
