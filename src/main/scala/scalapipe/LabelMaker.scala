
package scalapipe

private[scalapipe] class LabelMaker(val prefix: String = "") {

    private var count = 0
    
    def nextIndex(): Int = {
        val result = count
        count += 1
        count
    }

    def next(): String = prefix + nextIndex()

}

private[scalapipe] object LabelMaker {

    private val kernelCounter = new LabelMaker("kernel")
    private val instanceCounter = new LabelMaker
    private val edgeCounter = new LabelMaker
    private val typeCounter = new LabelMaker("type")
    private val deviceCounter = new LabelMaker("device")
    private val functionCounter = new LabelMaker("func")
    private val portCounter = new LabelMaker

    def getKernelLabel() = kernelCounter.next()

    def getInstanceIndex() = instanceCounter.nextIndex()

    def getEdgeIndex() = edgeCounter.nextIndex()

    def getTypeLabel() = typeCounter.next()

    def getDeviceLabel() = deviceCounter.next()

    def getFunctionLabel() = functionCounter.next()

    def getPortIndex() = portCounter.nextIndex()

}

