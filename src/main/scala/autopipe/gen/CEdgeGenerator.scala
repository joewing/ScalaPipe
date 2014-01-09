
package autopipe.gen

import autopipe._

/** Edge generator for edges mapped to CPUs on the same host.
 * Note that the sending and receiving sides will always be in
 * the same process.
 */
private[autopipe] class CEdgeGenerator extends EdgeGenerator(Platforms.C) {

    private def getQueueName(stream: Stream) = "q_" + stream.label

    override def emitGlobals(streams: Traversable[Stream]) {
        for (s <- streams) {
            writeGlobals(s)
        }
    }

    override def emitInit(streams: Traversable[Stream]) {
        for (s <- streams) {
            writeInit(s)
        }
    }

    override def emitDestroy(streams: Traversable[Stream]) {
        for (s <- streams) {
            writeDestroy(s)
        }
    }

    private def writeInit(stream: Stream) {

        val queueName = getQueueName(stream)

        val depth = stream.depth
        val valueType = stream.valueType

        // Initialize the queue.
        write(queueName + " = (APQ*)malloc(APQ_GetSize(" + depth +
                ", sizeof(" + valueType + ")));")
        write("APQ_Initialize(" + queueName + ", " + depth +
                ", sizeof(" + valueType + "));")

    }

    private def writeGlobals(stream: Stream) {

        val queueName = getQueueName(stream)
        val destIndex = stream.destIndex
        val destBlock = stream.destBlock
        val sourceBlock = stream.sourceBlock
        val sourceIndex = stream.sourceIndex
        val destDevice = stream.destBlock.device
        val sourceDevice = stream.sourceBlock.device

        // Define the queue data structure.
        write("static APQ *" + queueName + ";")

        // "process" - Run on the consumer thread (dest).
        write("static void " + sourceBlock.label + "_send_signal(int,int,int);")
        write("static void " + destBlock.label + "_send_signal(int,int,int);")
        write("static bool " + stream.label + "_process()")
        write("{")
        enter
        write("if(" + destBlock.label + ".inputs[" + destIndex +
                "].data == NULL) {")
        enter
        write("char *buf;")
        write("uint32_t c = APQ_StartRead(" + queueName + ", &buf);")
        write("if(c > 0) {")
        enter
        write(destBlock.label + ".inputs[" + destIndex + "].data = (" +
                stream.valueType + "*)buf;")
        write(destBlock.label + ".inputs[" + destIndex + "].count = c;")

        leave
        write("}")
        leave
        write("}")

        write("if(" + destBlock.label + ".inputs[" + destIndex + "].data) {")
        enter
        write(destBlock.label + ".clock.count += 1;")
        write("ap_" + destBlock.blockType.name + "_push(&" +
                destBlock.label + ".priv, " +
                destIndex + ", " +
                destBlock.label + ".inputs[" + destIndex + "].data, " +
                destBlock.label + ".inputs[" + destIndex + "].count);")
        write("return true;")
        leave
        write("} else {")
        enter
        write("return false;")
        leave
        write("}")
        leave
        write("}")

        // "release" - Run on the consumer thread (dest).
        write("static void " + stream.label + "_release(int count)")
        write("{")
        enter
        write("APQ_FinishRead(q_" + stream.label + ", count);")
        leave
        write("}")

        // "get_free" - Run on the producer thread (source).
        write("static int " + stream.label + "_get_free()")
        write("{")
        enter
        write("return APQ_GetFree(" + queueName + ");")
        leave
        write("}")

        // "is_empty" - Run on the consumer thread (dest).
        write("static int " + stream.label + "_is_empty()")
        write("{")
        enter
        write("return APQ_IsEmpty(" + queueName + ");")
        leave
        write("}")

        // "allocate" - Run on the producer thread (source).
        write("static void *" + stream.label + "_allocate(int count)")
        write("{")
        enter
        write("return APQ_StartWrite(" + queueName + ", count);")
        leave
        write("}")

        // "send" - Run on the producer thread (source).
        write("static void " + stream.label + "_send(int count)")
        write("{")
        enter
        write("APQ_FinishWrite(" + queueName + ", count);")
        leave
        write("}")

        // "send_signal" - Run on the producer thread (source).
        write("static void " + stream.label + "_send_signal(UNSIGNED64 s)")
        write("{")
        enter
        leave
        write("}")

    }

    private def writeDestroy(stream: Stream) {
        val queueName = getQueueName(stream)
        write("free(" + queueName + ");")
    }

}

