package autopipe

class StreamList(
        val ap: AutoPipe,
        val kernel: KernelInstance
    ) {

    private[autopipe] var streams = Seq[Stream]()
    private var measures = Seq[(Symbol, Symbol)]()
    private var edges = Seq[Edge]()

    def apply(): Stream = apply(0)

    def apply(i: Int): Stream = {
        val port_name = new IntPortName(i)
        val stream = ap.createStream(kernel, port_name)
        measures.foreach { m => stream.addMeasure(m._1, m._2) }
        edges.foreach { stream.edge = _ }
        streams = streams :+ stream
        kernel.setOutput(port_name, stream)
        stream
    }

    def apply(s: String): Stream = {
        val port_name = new StringPortName(s)
        val stream = ap.createStream(kernel, port_name)
        measures.foreach { m => stream.addMeasure(m._1, m._2) }
        edges.foreach { stream.edge = _ }
        streams = streams :+ stream
        kernel.setOutput(port_name, stream)
        stream
    }

    def apply(s: Symbol): Stream = apply(s.name)

    private[autopipe] def edgeCount = edges.length

    private[autopipe] def addMeasure(stat: Symbol, metric: Symbol) {
        measures = measures :+ ((stat, metric))
        streams.foreach { _.addMeasure(stat, metric) }
    }

    private[autopipe] def setEdge(e: Edge) {
        edges = edges :+ e
        streams.foreach { _.edge = e }
    }

}
