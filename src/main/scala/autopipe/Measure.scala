
package autopipe

import scala.collection.mutable.HashMap

private[autopipe] class Measure(val stream: Stream,
                                          val stat: Symbol,
                                          val metric: Symbol) {

    private[autopipe] var sourceOffset = -1
    private[autopipe] var sourceActivityOffset = -1
    private[autopipe] var sourceQueueOffset = -1
    private[autopipe] var sourceInterOffset = -1

    private[autopipe] var destOffset = -1
    private[autopipe] var destActivityOffset = -1
    private[autopipe] var destQueueOffset = -1
    private[autopipe] var destInterOffset = -1

    private[autopipe] def setSourceOffsets(offsets: Array[Int]) {
        sourceOffset = offsets(0)
        sourceActivityOffset = offsets(1)
        sourceQueueOffset = offsets(2)
        sourceInterOffset = offsets(3)
    }

    private[autopipe] def setDestOffsets(offsets: Array[Int]) {
        destOffset = offsets(0)
        destActivityOffset = offsets(1)
        destQueueOffset = offsets(2)
        destInterOffset = offsets(3)
    }

    private[autopipe] def emit(stream: Stream): String =
        "measure " + stat.name + " " + metric.name + " at " + stream.label + ";\n"

    private[autopipe] def usePush: Boolean = metric match {
        case 'occupancy        => true
        case 'rate              => true
        case 'backpressure    => true
        case 'latency          => true
        case 'interpush        => true
        case _                    => false
    }

    private[autopipe] def usePop: Boolean = metric match {
        case 'occupancy        => true
        case 'latency          => true
        case 'interpop         => true
        case _                    => false
    }

    private[autopipe] def useFull: Boolean = metric match {
        case 'backpressure    => true
        case _                    => false
    }

    private[autopipe] def useQueueMonitor: Boolean = metric match {
        case 'occupancy        => true
        case _                    => false
    }

    private[autopipe] def useInputActivity: Boolean = metric match {
        case 'rate              => true
        case 'util              => true
        case _                    => false
    }

    private[autopipe] def useOutputActivity: Boolean = metric match {
        case _                    => false
    }

    private[autopipe] def useFullActivity: Boolean = metric match {
        case 'backpressure    => true
        case _                    => false
    }

    private[autopipe] def useInterPush: Boolean = metric match {
        case 'interpush        => true
        case _                    => false
    }

    private[autopipe] def useInterPop: Boolean = metric match {
        case 'interpop         => true
        case _                    => false
    }

    private[autopipe] def getTTAStat: String =
        "XTTA_STAT_" + stat.name.toUpperCase

    private[autopipe] def getTTAMetric: String =
        "XTTA_MEASURE_" + metric.name.toUpperCase

    private[autopipe] def getName: String = stat.name + " " + metric.name

}

