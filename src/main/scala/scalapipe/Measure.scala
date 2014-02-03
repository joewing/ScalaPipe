package scalapipe

private[scalapipe] class Measure(
        val stream: Stream,
        val stat: Symbol,
        val metric: Symbol
    ) {

    private[scalapipe] var sourceOffset = -1
    private[scalapipe] var sourceActivityOffset = -1
    private[scalapipe] var sourceQueueOffset = -1
    private[scalapipe] var sourceInterOffset = -1

    private[scalapipe] var destOffset = -1
    private[scalapipe] var destActivityOffset = -1
    private[scalapipe] var destQueueOffset = -1
    private[scalapipe] var destInterOffset = -1

    private[scalapipe] def setSourceOffsets(offsets: Array[Int]) {
        sourceOffset = offsets(0)
        sourceActivityOffset = offsets(1)
        sourceQueueOffset = offsets(2)
        sourceInterOffset = offsets(3)
    }

    private[scalapipe] def setDestOffsets(offsets: Array[Int]) {
        destOffset = offsets(0)
        destActivityOffset = offsets(1)
        destQueueOffset = offsets(2)
        destInterOffset = offsets(3)
    }

    private[scalapipe] def emit(stream: Stream): String =
        s"measure ${stat.name} ${metric.name} at ${stream.label};\n"

    private[scalapipe] def usePush: Boolean = metric match {
        case 'occupancy     => true
        case 'rate          => true
        case 'backpressure  => true
        case 'latency       => true
        case 'interpush     => true
        case _              => false
    }

    private[scalapipe] def usePop: Boolean = metric match {
        case 'occupancy     => true
        case 'latency       => true
        case 'interpop      => true
        case _              => false
    }

    private[scalapipe] def useFull: Boolean = metric match {
        case 'backpressure  => true
        case _              => false
    }

    private[scalapipe] def useQueueMonitor: Boolean = metric match {
        case 'occupancy     => true
        case _              => false
    }

    private[scalapipe] def useInputActivity: Boolean = metric match {
        case 'rate          => true
        case 'util          => true
        case _              => false
    }

    private[scalapipe] def useOutputActivity: Boolean = metric match {
        case _              => false
    }

    private[scalapipe] def useFullActivity: Boolean = metric match {
        case 'backpressure  => true
        case _              => false
    }

    private[scalapipe] def useInterPush: Boolean = metric match {
        case 'interpush => true
        case _          => false
    }

    private[scalapipe] def useInterPop: Boolean = metric match {
        case 'interpop  => true
        case _          => false
    }

    private[scalapipe] def getTTAStat: String =
        "TTA_STAT_" + stat.name.toUpperCase

    private[scalapipe] def getTTAMetric: String =
        "TTA_MEASURE_" + metric.name.toUpperCase

    private[scalapipe] def getName: String = s"${stat.name} ${metric.name}"

}
