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
        val depth = stream.parameters.get[Int]('queueDepth)
        val vtype = stream.valueType

        // Initialize the queue.
        write(s"$qname = (SPQ*)malloc(spq_get_size($depth, sizeof($vtype)));")
        write(s"spq_init($qname, $depth, sizeof($vtype));")

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
        write(s"static SPQ *$qname;")

        // "get_free"
        write(s"static int ${label}_get_free()")
        enter
        writeReturn(s"spq_get_free($qname);")
        leave

        // "allocate"
        write(s"static void *${label}_allocate()")
        enter
        writeReturn(s"spq_start_write($qname, 1);")
        leave

        // "send"
        write(s"static void ${label}_send()")
        enter
        write(s"spq_finish_write($qname, 1);")
        leave

        // "get_available"
        write(s"static int ${label}_get_available()")
        enter
        writeReturn(s"spq_get_used($qname);")
        leave

        // "read_value"
        write(s"static void *${label}_read_value()")
        enter
        write(s"char *buffer = NULL;")
        writeIf(s"spq_start_read($qname, &buffer) > 0")
        writeReturn(s"buffer")
        writeElse
        writeReturn(s"NULL")
        writeEnd
        leave

        // "release"
        write(s"static void ${label}_release()")
        enter
        write(s"spq_finish_read($qname, 1);")
        leave

    }

    private def writeDestroy(stream: Stream) {
        val qname = queueName(stream)
        write(s"free($qname);")
    }

}
