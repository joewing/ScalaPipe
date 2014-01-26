package autopipe.gen

import autopipe._

/** Edge generator for edges mapped between the CPU and FPGA on a
 *  SmartFusion SoC. */
private[autopipe] class SmartFusionEdgeGenerator(val ap: AutoPipe)
    extends EdgeGenerator(Platforms.HDL) {

    override def emitCommon() {
        write("#include <sys/ioctl.h>")
    }

    override def emitGlobals(streams: Traversable[Stream]) {

        val devices = getDevices(streams)

        // Write globals for each device.
        for (d <- devices) {

            val fd = d.label + "_fd"
            write("static int " + fd + " = -1;")

            val senderStreams = getSenderStreams(d, streams)
            val receiverStreams = getReceiverStreams(d, streams)
            for (stream <- senderStreams ++ receiverStreams) {
                write("static APQ *q_" + stream.label + " = NULL;")
            }

        }

        // Write the send/recv functions.
        for (d <- devices) {

            val senderStreams = getSenderStreams(d, streams)
            val receiverStreams = getReceiverStreams(d, streams)

            if (!senderStreams.isEmpty) {
                writeSendFunctions(d, senderStreams)
            }

            if (!receiverStreams.isEmpty) {
                writeRecvFunctions(d, receiverStreams)
            }

        }

    }

    override def emitInit(streams: Traversable[Stream]) {
        for (d <- getDevices(streams)) {
            writeInit(d, getExternalStreams(d, streams))
        }
    }

    override def emitDestroy(streams: Traversable[Stream]) {
        for (d <- getDevices(streams)) {
            writeDestroy(d, getExternalStreams(d, streams))
        }
    }

    private def writeInit(device: Device, streams: Traversable[Stream]) {

        val senderStreams = getSenderStreams(device, streams)
        val receiverStreams = getReceiverStreams(device, streams)

        // Open the device.
        val fd = device.label + "_fd"
        write(fd + " = open(\"/dev/sp\", O_RDWR);")
        write("if(" + fd + " < 0) {")
        enter
        write("perror(\"could not open /dev/sp\");")
        write("exit(-1);")
        leave
        write("}")

        // Create buffers.
        for (stream <- senderStreams ++ receiverStreams) {
            val depth = stream.depth
            val valueType = stream.valueType
            val queueName = "q_" + stream.label
            write(queueName + " = (APQ*)malloc(APQ_GetSize(" + depth +
                    ", sizeof(" + valueType + ")));")
            write("APQ_Initialize(" + queueName + ", " + depth +
                    ", sizeof(" + valueType + "));")
        }

    }

    private def writeDestroy(device: Device, streams: Traversable[Stream]) {

        val fd = device.label + "_fd"
        write("close(" + fd + ");")

        for (s <- streams) {
            val queueName = "q_" + s.label
            write("free(" + queueName + ");")
        }

    }

    private def writeSendFunctions(device: Device,
                                             streams: Traversable[Stream]) {

        val fd = device.label + "_fd"

        // Write the per-stream functions.
        for (stream <- streams) {

            val queueName = "q_" + stream.label
            val valueType = stream.valueType

            // "get_free"
            write("static int " + stream.label + "_get_free()")
            write("{")
            enter
            write("return APQ_GetFree(" + queueName + ");")
            leave
            write("}")

            // "is_empty"
            write("static int " + stream.label + "_is_empty()")
            write("{")
            enter
            write("return APQ_IsEmpty(" + queueName + ");")
            leave
            write("}")

            // "allocate"
            write("static void *" + stream.label + "_allocate(int count)")
            write("{")
            enter
            write("return APQ_StartWrite(" + queueName + ", count);")
            leave
            write("}")

            // "send"
            write("static void " + stream.label + "_send(int count)")
            write("{")
            enter
            write("APQ_FinishWrite(" + queueName + ", count);")
            write("char *data;")
            write("uint32_t c = APQ_StartRead(" + queueName + ", &data);")
            write("const ssize_t sz = (sizeof(" + valueType + ") + 3) & ~3;")
            write("ioctl(" + fd + ", 0, " + stream.index + ");")
            write("for(uint32_t i = 0; i < c * sz; i += sz) {");
            enter
            write("ssize_t offset = 0;")
            write("while(offset < sz) {")
            enter
            write("offset += write(" + fd + ", &data[i + offset], sz - offset);")
            leave
            write("}")
            leave
            write("}")
            write("APQ_FinishRead(" + queueName + ", c);")
            leave
            write("}")

        }

    }

    private def writeRecvFunctions(device: Device,
                                             streams: Traversable[Stream]) {

        val fd = device.label + "_fd"

        // Write the per-stream functions.
        for (stream <- streams) {

            val queueName = "q_" + stream.label
            val valueType = stream.valueType
            val destKernel = stream.destKernel
            val destIndex = stream.destIndex

            reset
            set("fd", fd)
            set("index", stream.index)
            set("label", stream.label)
            set("queueName", queueName)
            set("valueType", valueType)
            set("destLabel", destKernel.label)
            set("destIndex", destIndex)
            set("destName", destKernel.kernelType.name)

            // "process"
            write("static void $label$_process()")
            write("{")
            enter
            write("char *data;")
            write("bool got_read;")
            write("do {")
            enter
            write("got_read = false;")
            write("data = APQ_StartWrite($queueName$, 1);")
            write("if(data != NULL) {")
            enter
            write("ioctl($fd$, 0, $index$);")
            if (valueType.bits <= 32) {
                write("ssize_t offset = 0;")
                write("UNSIGNED32 temp;")
                write("offset = read($fd$, &temp, 4);")
                write("if(offset > 0) {")
                enter
                write("memcpy(data, &temp, sizeof($valueType$));")
                write("got_read = true;")
                write("APQ_FinishWrite($queueName$, 1);")
                leave
                write("}")
            } else {
                write("ssize_t offset = 0;")
                write("const ssize_t sz = (sizeof($valueType$) + 3) & ~3;")
                write("offset = read($fd$, &data[0], sz);")
                write("if(offset > 0) {")
                enter
                write("while(offset < sz) {")
                enter
                write("offset += read($fd$, &data[offset], sz - offset);")
                leave
                write("}")
                write("got_read = true;")
                write("APQ_FinishWrite($queueName$, 1);")
                leave
                write("}")
                leave
                write("}")
            }
            leave
            write("} while(got_read);")
            write

            write("if(!APQ_IsEmpty($queueName$) && " +
                    "$destLabel$.inputs[$destIndex$].data == NULL) {")
            enter
            write("uint32_t c = APQ_StartBlockingRead($queueName$, &data);")
            write("$destLabel$.inputs[$destIndex$].data = ($valueType$*)data;")
            write("$destLabel$.inputs[$destIndex$].count = c;")
            write("$destLabel$.clock.count += 1;")
            write("APC_Start(&$destLabel$.clock);")
            write("ap_$destName$_push(&$destLabel$.priv, $destIndex$, " +
                    "$destLabel$.inputs[$destIndex$].data, c);")
            write("APC_Stop(&$destLabel$.clock);")
            leave
            write("}")
            write

            leave
            write("}")

            // "release"
            write("static void $label$_release(int count)")
            write("{")
            enter
            write("APQ_FinishRead($queueName$, count);")
            leave
            write("}")

        }

    }

}
