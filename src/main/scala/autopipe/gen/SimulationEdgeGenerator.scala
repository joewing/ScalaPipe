
package autopipe.gen

import autopipe._

/** Edge generator for edges mapped between the CPU and FPGA simulation. */
private[autopipe] class SimulationEdgeGenerator(val ap: AutoPipe)
    extends EdgeGenerator(Platforms.HDL) {

    override def emitCommon() {
        write("#include <sys/types.h>")
        write("#include <sys/stat.h>")
        write("#include <sys/wait.h>")
        write("#include <fcntl.h>")
        write("#include <unistd.h>")
        write("#include <signal.h>")
    }

    override def emitGlobals(streams: Traversable[Stream]) {

        write("static void sim_write(int fd, const char *data, " +
                "size_t size, uint32_t count)")
        write("{")
        enter
        write("const size_t total = size * count + 4;")
        write("char *buffer = (char*)alloca(total);")
        write("*(uint32_t*)&buffer[0] = count;")
        write("if(count) {")
        enter
        write("memcpy(&buffer[4], data, size * count);")
        leave
        write("}")
        write("size_t offset = 0;")
        write("while(offset < total) {")
        enter
        write("offset += write(fd, &buffer[offset], total - offset);")
        leave
        write("}")
        leave
        write("}")

        val devices = getDevices(streams)
        for (d <- devices) {

            // Write the send functions.
            val senderStreams = getSenderStreams(d, streams)
            for (s <- senderStreams) {
                writeSendFunctions(d, s)
            }

            // Write the receive functions.
            val receiverStreams = getReceiverStreams(d, streams)
            for (s <- receiverStreams) {
                writeReceiveFunctions(d, s, senderStreams)
            }

        }

        write("static pid_t sim_pid = 0;")
        write("static void stopSimulation()")
        write("{")
        enter
        write("kill(sim_pid, SIGINT);")
        write("waitpid(sim_pid, NULL, 0);")
        for (s <- streams) {
            reset
            set("label", s.label);
            write("close(stream$label$);")
            write("unlink(\"stream$label$\");")
        }
        leave
        write("}")

    }

    override def emitInit(streams: Traversable[Stream]) {

        // Create and open the FIFOs.
        for (s <- streams) {
            reset
            set("label", s.label)
            set("depth", s.depth)
            set("type", s.valueType)
            set("queue", "q_" + s.label)

            // Create the FIFO.
            write("unlink(\"stream$label$\");")
            write("if(mkfifo(\"stream$label$\", 0600) < 0) {")
            enter
            write("perror(\"could not create FIFO\\n\");")
            write("exit(-1);");
            leave
            write("}")

            // Open the FIFO.
            if (s.sourceKernel.device.platform == platform) {
                // Edge from the device.
                write("stream$label$ = open(\"stream$label$\", " +
                      "O_RDONLY | O_NONBLOCK | O_SYNC);")
            } else {
                // Edge to the device.
                write("stream$label$ = open(\"stream$label$\", " +
                      "O_RDWR | O_SYNC);")
            }
            write("if(stream$label$ < 0) {")
            enter
            write("perror(\"could not open FIFO\");")
            write("exit(-1);")
            leave
            write("}")

            // Create the buffer.
            write("$queue$ = (APQ*)malloc(APQ_GetSize($depth$, " +
                  "sizeof($type$)));")
            write("APQ_Initialize($queue$, $depth$, sizeof($type$));")

        }

        // Start the simulation.
        write("sim_pid = fork();")
        write("if(sim_pid == 0) {")
        enter
        write("int rc = execlp(\"vvp\", \"vvp\", \"hdl\", NULL);")
        write("if(rc < 0) {")
        enter
        write("perror(\"could not run hdl\");")
        write("exit(-1);");
        leave
        write("}")
        leave
        write("} else {")
        enter
        write("atexit(stopSimulation);")
        leave
        write("}")

    }

    override def emitDestroy(streams: Traversable[Stream]) {

    }

    private def writeSendFunctions(device: Device, stream: Stream) {

        reset
        set("label", stream.label)
        set("queue", "q_" + stream.label)
        set("type", stream.valueType)
        set("fd", "stream" + stream.label)

        val itemCount = stream.valueType match {
            case avt: ArrayValueType    => avt.length
            case _                            => 1
        }

        val itemSize = stream.valueType match {
            case avt: ArrayValueType    => avt.itemType.bits / 8
            case _                            => stream.valueType.bits / 8
        }

        // Globals.
        write("static int stream$label$ = -1;")
        write("static APQ *$queue$ = NULL;")

        // "get_free"
        write("static int $label$_get_free()")
        write("{")
        enter
        write("return APQ_GetFree($queue$);")
        leave
        write("}")

        // "is_empty"
        write("static int $label$_is_empty()")
        write("{")
        enter
        write("return APQ_IsEmpty($queue$);")
        leave
        write("}")

        // "allocate"
        write("static pthread_mutex_t $label$_mutex = PTHREAD_MUTEX_INITIALIZER;")
        write("static void *$label$_allocate(int count)")
        write("{")
        enter
        write("pthread_mutex_lock(&$label$_mutex);")
        write("void *ptr =  APQ_StartWrite($queue$, count);")
        write("if(XUNLIKELY(!ptr)) {")
        enter
        write("pthread_mutex_unlock(&$label$_mutex);")
        leave
        write("}")
        write("return ptr;")
        leave
        write("}")

        // "send"
        write("static void $label$_send(int count)")
        write("{")
        enter
        write("APQ_FinishWrite($queue$, count);")
        write("char *data;")
        write("const uint32_t c = APQ_StartRead($queue$, &data);")
        write("sim_write($fd$, data, sizeof($type$), c);")
        write("APQ_FinishRead($queue$, c);")
        write("pthread_mutex_unlock(&$label$_mutex);")
        leave
        write("}")

    }

    private def writeReceiveFunctions(device: Device,
                                      stream: Stream,
                                      senders: Traversable[Stream]) {

        reset
        set("label", stream.label)
        set("queue", "q_" + stream.label)
        set("fd", "stream" + stream.label)
        set("type", stream.valueType)
        set("destLabel", stream.destKernel.label)
        set("destIndex", stream.destIndex)
        set("destName", stream.destKernel.kernelType.name)

        // Globals.
        write("static int stream$label$ = -1;")
        write("static APQ *$queue$ = NULL;")

        // "process"
        write("static bool $label$_process()")
        write("{")
        enter
        write("char *data;")
        write("bool got_read;")
        write("bool updated = false;")
        write("do {")
        enter
        write("got_read = false;")
        write("data = APQ_StartWrite($queue$, 1);")
        write("if(data != NULL) {")
        enter
        write("ssize_t offset = 0;")
        write("do {")
        enter
        write("ssize_t rc = read($fd$, &data[offset], " +
              "sizeof($type$) - offset);")
        write("if(rc > 0) {")
        enter
        write("got_read = true;")
        write("updated = true;")
        write("offset += rc;")
        write("if(offset == sizeof($type$)) {")
        enter
        write("APQ_FinishWrite($queue$, 1);")
        write("break;")
        leave
        write("}")  // offset == sizeof($type$)
        leave
        write("}")  // rc > 0
        leave
        write("} while(got_read);")  // do
        leave
        write("}")  // data != NULL
        leave
        write("} while(got_read);")    // do
        write

        write("if($destLabel$.inputs[$destIndex$].data == NULL) {")
        enter
        write("char *buf;")
        write("uint32_t c = APQ_StartRead($queue$, &buf);")
        write("if(c > 0) {")
        enter
        write("$destLabel$.inputs[$destIndex$].data = ($type$*)buf;")
        write("$destLabel$.inputs[$destIndex$].count = c;")
        leave
        write("}")
        leave
        write("}")

        write("if($destLabel$.inputs[$destIndex$].data != NULL) {")
        enter
        write("$destLabel$.clock.count += 1;")
        write("ap_$destName$_push(&$destLabel$.priv, $destIndex$, " +
              "$destLabel$.inputs[$destIndex$].data, " +
              "$destLabel$.inputs[$destIndex$].count);")
        write("return true;")
        leave
        write("} else {")
        enter
        write("if(!updated) {")
        enter
        for (sender <- senders) {
            write(sender.label + "_allocate(0);")
            write(sender.label + "_send(0);")
        }
        leave
        write("}")
        write("return false;")
        leave
        write("}")

        leave
        write("}")

        // "release"
        write("static void $label$_release(int count)")
        write("{")
        enter
        write("APQ_FinishRead($queue$, count);")
        leave
        write("}")

    }

}

