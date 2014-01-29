package autopipe

import scala.collection.mutable.HashSet
import autopipe.dsl.Kernel

class Stream(
        ap: AutoPipe,
        private[autopipe] val sourceKernel: KernelInstance,
        private[autopipe] val sourcePort: PortName
    ) {

    private[autopipe] val index = LabelMaker.getEdgeIndex
    private[autopipe] val label = "edge" + index
    private[autopipe] var destKernel: KernelInstance = null
    private[autopipe] var destPort: PortName = null
    private[autopipe] val measures = new HashSet[Measure]
    private[autopipe] var edge: Edge = null
    private[autopipe] var depth = ap.parameters.get[Int]('queueDepth)

    /** Take this stream and apply it to the input of a split kernel. */
    def iteratedMap(iterations: Int, splitter: Kernel): Seq[Stream] = {

        def helper(iter: Int, inputs: Seq[Stream]): Seq[Stream] = {
            if (iter > 0) {
                val outputs = inputs.flatMap(a => {
                    val kernel = ap.createInstance(splitter)((null, a))
                    val outputCount = ap.getOutputCount(splitter.name)
                    Seq.range(0, outputCount).map(kernel(_))
                })
                helper(iter - 1, outputs)
            } else {
                inputs
            }
        }

        helper(iterations, Seq(this))

    }

    private[autopipe] def setEdge(e: Edge) {
        edge = e
        if (edge.queueSize > 0) {
            depth = edge.queueSize
        }
    }

    private[autopipe] def getDepthBits: Int = {
        val log2 = math.log(depth) / math.log(2.0)
        math.ceil(log2).toInt
    }

    private[autopipe] def valueType = sourceKernel.outputType(sourcePort)

    private[autopipe] def checkType {
        val st = sourceKernel.outputType(sourcePort)
        val dt = destKernel.inputType(destPort)
        if (st != dt) {
            Error.raise("stream type mismatch: " + st + " vs " + dt)
        }
    }

    private[autopipe] def addMeasure(stat: Symbol, metric: Symbol) {
        measures += new Measure(this, stat, metric)
    }

    private[autopipe] def setDest(k: KernelInstance, p: PortName) {
        destKernel = k
        destPort = p
    }

    private[autopipe] def destIndex = destKernel.inputIndex(destPort)

    private[autopipe] def sourceIndex = sourceKernel.outputIndex(sourcePort)

    private[autopipe] def usePush = measures.exists(_.usePush)

    private[autopipe] def usePop = measures.exists(_.usePop)

    private[autopipe] def useFull = measures.exists(_.useFull)

    private[autopipe] def useQueueMonitor =
        measures.exists(_.useQueueMonitor)

    private[autopipe] def useOutputActivity =
        measures.exists(_.useOutputActivity)

    private[autopipe] def useInputActivity =
        measures.exists(_.useInputActivity)

    private[autopipe] def useFullActivity =
        measures.exists(_.useFullActivity)

    private[autopipe] def useInterPush = measures.exists(_.useInterPush)

    private[autopipe] def useInterPop = measures.exists(_.useInterPop)

}

