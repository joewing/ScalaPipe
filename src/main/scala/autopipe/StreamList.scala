
package autopipe

import scala.collection.mutable.ListBuffer

class StreamList(ap: AutoPipe, _block: Block) {

    private[autopipe] val block = _block
    private[autopipe] val streams = new ListBuffer[Stream]
    private val measures = new ListBuffer[(Symbol, Symbol)]
    private val edges = new ListBuffer[Edge]

    def apply(): Stream = apply(0)

    def apply(i: Int): Stream = {
        val port_name = new IntPortName(i)
        val stream = new Stream(ap, block, port_name)
        measures.foreach { m => stream.addMeasure(m._1, m._2) }
        edges.foreach { stream.edge = _ }
        streams += stream
        block.setOutput(port_name, stream)
        stream
    }

    def apply(s: String): Stream = {
        val port_name = new StringPortName(s)
        val stream = new Stream(ap, block, port_name)
        measures.foreach { m => stream.addMeasure(m._1, m._2) }
        edges.foreach { stream.edge = _ }
        streams += stream
        block.setOutput(port_name, stream)
        stream
    }

    def apply(s: Symbol): Stream = apply(s.name)

    private[autopipe] def addMeasure(stat: Symbol, metric: Symbol) {
        measures += ((stat, metric))
        streams.foreach { _.addMeasure(stat, metric) }
    }

    private[autopipe] def setEdge(e: Edge) {
        edges += e
        streams.foreach { _.edge = e }
    }

}

