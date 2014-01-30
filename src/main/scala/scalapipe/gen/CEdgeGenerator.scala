package scalapipe.gen

import scalapipe._

/** Edge generator for edges mapped to CPUs on the same host.
 * Note that the sending and receiving sides will always be in
 * the same process (but likely in different threads).
 */
private[scalapipe] class CEdgeGenerator
    extends EdgeGenerator(Platforms.C) with CGenerator {

    private def queueName(stream: Stream) = s"q_${stream.label}"

    override def emitGlobals(streams: Traversable[Stream]) {
        streams.foreach { s => writeGlobals(s) }
    }

    override def emitInit(streams: Traversable[Stream]) {
        streams.foreach { s => writeInit(s) }
    }

    override def emitDestroy(streams: Traversable[Stream]) {
        streams.foreach { s => writeDestroy(s) }
    }

    private def writeInit(stream: Stream) {

        val qname = queueName(stream)
        val depth = stream.depth
        val vtype = stream.valueType

        // Initialize the queue.
        write(s"$qname = (APQ*)malloc(APQ_GetSize($depth, sizeof($vtype)));")
        write(s"APQ_Initialize($qname, $depth, sizeof($vtype));")

    }

    private def writeGlobals(stream: Stream) {

        val qname = queueName(stream)
        val label = stream.label
        val destIndex = stream.destIndex
        val destKernel = stream.destKernel
        val destLabel = destKernel.label
        val destName = destKernel.name
        val sourceIndex = stream.sourceIndex
        val sourceKernel = stream.sourceKernel
        val sourceLabel = sourceKernel.label
        val sourceName = sourceKernel.name
        val destDevice = stream.destKernel.device
        val sourceDevice = stream.sourceKernel.device
        val vtype = stream.valueType

        // Define the queue data structure.
        write(s"static APQ *$qname;")

        // "process" - Run on the consumer thread (dest).
        write(s"static bool ${label}_process()")
        enter
        writeIf(s"$destLabel.inputs[$destIndex].data == NULL")
        write(s"char *buf;")
        write(s"uint32_t c = APQ_StartRead($qname, &buf);")
        writeIf(s"c > 0")
        write(s"$destLabel.inputs[$destIndex].data = ($vtype*)buf;")
        write(s"$destLabel.inputs[$destIndex].count = c;")
        writeEnd
        writeEnd

        writeIf(s"$destLabel.inputs[$destIndex].data")
        write(s"$destLabel.clock.count += 1;")
        write(s"ap_${destName}_push(&$destLabel.priv, $destIndex, " +
              s"$destLabel.inputs[$destIndex].data, " +
              s"$destLabel.inputs[$destIndex].count);")
        writeReturn("true")
        writeElse
        writeReturn("false")
        writeEnd
        leave

        // "release" - Run on the consumer thread (dest).
        write(s"static void ${label}_release(int count)")
        enter
        write(s"APQ_FinishRead($qname, count);")
        leave

        // "get_free" - Run on the producer thread (source).
        write(s"static int ${label}_get_free()")
        enter
        writeReturn(s"APQ_GetFree($qname)")
        leave

        // "is_empty" - Run on the consumer thread (dest).
        write(s"static int ${label}_is_empty()")
        enter
        writeReturn(s"APQ_IsEmpty($qname)")
        leave

        // "allocate" - Run on the producer thread (source).
        write(s"static void *${label}_allocate(int count)")
        enter
        writeReturn(s"APQ_StartWrite($qname, count)")
        leave

        // "send" - Run on the producer thread (source).
        write(s"static void ${label}_send(int count)")
        enter
        write(s"APQ_FinishWrite($qname, count);")
        leave

    }

    private def writeDestroy(stream: Stream) {
        val qname = queueName(stream)
        write(s"free($qname);")
    }

}
