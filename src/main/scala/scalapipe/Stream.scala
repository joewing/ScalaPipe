package scalapipe

import scalapipe.dsl.Kernel

class Stream(
        private[scalapipe] val sp: ScalaPipe,
        private[scalapipe] val sourceKernel: KernelInstance,
        private[scalapipe] val sourcePort: PortName
    ) {

    private[scalapipe] val index = LabelMaker.getEdgeIndex
    private[scalapipe] val label = s"edge$index"
    private[scalapipe] var destKernel: KernelInstance = null
    private[scalapipe] var destPort: PortName = null
    private[scalapipe] var measures = Set[Measure]()
    private[scalapipe] var edge: Edge = null

    /** Take this stream and apply it to the input of a split kernel. */
    def iteratedMap(iterations: Int, splitter: Kernel): Seq[Stream] = {

        def helper(iter: Int, inputs: Seq[Stream]): Seq[Stream] = {
            if (iter > 0) {
                val outputs = inputs.flatMap(a => {
                    val kernel = sp.createInstance(splitter)((null, a))
                    val outputCount = sp.getOutputCount(splitter.name)
                    Seq.range(0, outputCount).map(kernel(_))
                })
                helper(iter - 1, outputs)
            } else {
                inputs
            }
        }

        helper(iterations, Seq(this))

    }

    private[scalapipe] def setEdge(e: Edge) {
        edge = e
    }

    private[scalapipe] def valueType = sourceKernel.outputType(sourcePort)

    private[scalapipe] def checkType {
        val st = sourceKernel.outputType(sourcePort)
        val dt = destKernel.inputType(destPort)
        if (st != dt) {
            Error.raise(s"stream type mismatch: $st vs $dt")
        }
    }

    private[scalapipe] def addMeasure(stat: Symbol, metric: Symbol) {
        measures += new Measure(this, stat, metric)
    }

    private[scalapipe] def setDest(k: KernelInstance, p: PortName) {
        destKernel = k
        destPort = p
    }

    private[scalapipe] def destIndex = destKernel.inputIndex(destPort)

    private[scalapipe] def sourceIndex = sourceKernel.outputIndex(sourcePort)

    private[scalapipe] def usePush = measures.exists(_.usePush)

    private[scalapipe] def usePop = measures.exists(_.usePop)

    private[scalapipe] def useFull = measures.exists(_.useFull)

    private[scalapipe] def useQueueMonitor =
        measures.exists(_.useQueueMonitor)

    private[scalapipe] def useOutputActivity =
        measures.exists(_.useOutputActivity)

    private[scalapipe] def useInputActivity =
        measures.exists(_.useInputActivity)

    private[scalapipe] def useFullActivity =
        measures.exists(_.useFullActivity)

    private[scalapipe] def useInterPush = measures.exists(_.useInterPush)

    private[scalapipe] def useInterPop = measures.exists(_.useInterPop)

}
