package scalapipe

class StreamList private[scalapipe](
        private[scalapipe] val sp: ScalaPipe,
        private[scalapipe] val kernel: KernelInstance
    ) {

    private[scalapipe] var streams = Seq[Stream]()
    private var measures = Seq[(Symbol, Symbol)]()
    private var edgeParameters = Map[Symbol, Any]()
    private var edges = Seq[Edge]()

    private def addStream(port: PortName): Stream = {
        val stream = sp.createStream(kernel, port)
        measures.foreach { m => stream.addMeasure(m._1, m._2) }
        for ( (k, v) <- edgeParameters) {
            stream.addParameter(k, v)
        }
        edges.foreach { stream.edge = _ }
        streams = streams :+ stream
        kernel.setOutput(port, stream)
        stream
    }

    def apply(): Stream = apply(0)

    def apply(i: Int): Stream = addStream(new IntPortName(i))

    def apply(s: String): Stream = addStream(new StringPortName(s))

    def apply(s: Symbol): Stream = apply(s.name)

    private[scalapipe] def edgeCount = edges.length

    private[scalapipe] def addMeasure(stat: Symbol, metric: Symbol) {
        measures = measures :+ ((stat, metric))
        streams.foreach { _.addMeasure(stat, metric) }
    }

    private[scalapipe] def addParameter(param: Symbol, value: Any) {
        edgeParameters += (param -> value)
        streams.foreach { _.addParameter(param, value) }
    }

    private[scalapipe] def setEdge(e: Edge) {
        edges = edges :+ e
        streams.foreach { _.edge = e }
    }

}
