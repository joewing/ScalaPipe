package scalapipe.gen

import scalapipe._

/** Edge generator for edges mapped between the CPU and FPGA simulation. */
private[scalapipe] class SimulationEdgeGenerator(
        val sp: ScalaPipe
    ) extends EdgeGenerator(Platforms.HDL) {

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
            val label = s.label
            write(s"close(stream$label);")
            write(s"""unlink(\"stream$label\");""")
        }
        leave
        write("}")

    }

    override def emitInit(streams: Traversable[Stream]) {

        // Create and open the FIFOs.
        for (s <- streams) {
            val label = s.label
            val depth = s.depth
            val vtype = s.valueType
            val queue = s"q_$label"

            // Create the FIFO.
            write(s"""unlink(\"stream$label\");""")
            write(s"""if(mkfifo(\"stream$label\", 0600) < 0) {""")
            enter
            write(s"""perror(\"could not create FIFO\\n\");""")
            write(s"exit(-1);");
            leave
            write(s"}")

            // Open the FIFO.
            if (s.sourceKernel.device.platform == platform) {
                // Edge from the device.
                write(s"""stream$label = open(\"stream$label\", """ +
                      s"O_RDONLY | O_NONBLOCK | O_SYNC);")
            } else {
                // Edge to the device.
                write(s"""stream$label = open(\"stream$label\", """ +
                      s"O_RDWR | O_SYNC);")
            }
            write(s"if(stream$label < 0) {")
            enter
            write(s"""perror(\"could not open FIFO\");""")
            write(s"exit(-1);")
            leave
            write(s"}")

            // Create the buffer.
            write(s"$queue = (APQ*)malloc(APQ_GetSize($depth, " +
                  s"sizeof($vtype)));")
            write(s"APQ_Initialize($queue, $depth, sizeof($vtype));")

        }

        // Start the simulation.
        write(s"sim_pid = fork();")
        write(s"if(sim_pid == 0) {")
        enter
        write(s"""int rc = execlp(\"vvp\", \"vvp\", \"hdl\", NULL);""")
        write(s"if(rc < 0) {")
        enter
        write(s"""perror(\"could not run hdl\");""")
        write(s"exit(-1);");
        leave
        write(s"}")
        leave
        write(s"} else {")
        enter
        write(s"atexit(stopSimulation);")
        leave
        write(s"}")

    }

    override def emitDestroy(streams: Traversable[Stream]) {

    }

    private def writeSendFunctions(device: Device, stream: Stream) {

        val label = stream.label
        val queue = "q_" + stream.label
        val vtype = stream.valueType
        val fd = "stream" + stream.label

        val itemCount = stream.valueType match {
            case avt: ArrayValueType    => avt.length
            case _                            => 1
        }

        val itemSize = stream.valueType match {
            case avt: ArrayValueType    => avt.itemType.bits / 8
            case _                            => stream.valueType.bits / 8
        }

        // Globals.
        write(s"static int stream$label = -1;")
        write(s"static APQ *$queue = NULL;")

        // "get_free"
        write(s"static int ${label}_get_free()")
        write(s"{")
        enter
        write(s"return APQ_GetFree($queue);")
        leave
        write(s"}")

        // "is_empty"
        write(s"static int ${label}_is_empty()")
        write(s"{")
        enter
        write(s"return APQ_IsEmpty($queue);")
        leave
        write(s"}")

        // "allocate"
        write(s"static pthread_mutex_t ${label}_mutex = " +
              s"PTHREAD_MUTEX_INITIALIZER;")
        write(s"static void *${label}_allocate(int count)")
        write(s"{")
        enter
        write(s"pthread_mutex_lock(&${label}_mutex);")
        write(s"void *ptr =  APQ_StartWrite($queue, count);")
        write(s"if(XUNLIKELY(!ptr)) {")
        enter
        write(s"pthread_mutex_unlock(&${label}_mutex);")
        leave
        write(s"}")
        write(s"return ptr;")
        leave
        write(s"}")

        // "send"
        write(s"static void ${label}_send(int count)")
        write(s"{")
        enter
        write(s"APQ_FinishWrite($queue, count);")
        write(s"char *data;")
        write(s"const uint32_t c = APQ_StartRead($queue, &data);")
        write(s"sim_write($fd, data, sizeof($vtype), c);")
        write(s"APQ_FinishRead($queue, c);")
        write(s"pthread_mutex_unlock(&${label}_mutex);")
        leave
        write(s"}")

    }

    private def writeReceiveFunctions(device: Device,
                                      stream: Stream,
                                      senders: Traversable[Stream]) {

        val label = stream.label
        val queue = "q_" + stream.label
        val fd = "stream" + stream.label
        val vtype = stream.valueType
        val destLabel = stream.destKernel.label
        val destIndex = stream.destIndex
        val destName = stream.destKernel.kernelType.name

        // Globals.
        write(s"static int stream$label = -1;")
        write(s"static APQ *$queue = NULL;")

        // "process"
        write(s"static bool ${label}_process()")
        write(s"{")
        enter
        write(s"char *data;")
        write(s"bool got_read;")
        write(s"bool updated = false;")
        write(s"do {")
        enter
        write(s"got_read = false;")
        write(s"data = APQ_StartWrite($queue, 1);")
        write(s"if(data != NULL) {")
        enter
        write(s"ssize_t offset = 0;")
        write(s"do {")
        enter
        write(s"ssize_t rc = read($fd, &data[offset], " +
              s"sizeof($vtype) - offset);")
        write(s"if(rc > 0) {")
        enter
        write(s"got_read = true;")
        write(s"updated = true;")
        write(s"offset += rc;")
        write(s"if(offset == sizeof($vtype)) {")
        enter
        write(s"APQ_FinishWrite($queue, 1);")
        write(s"break;")
        leave
        write(s"}")  // offset == sizeof($vtype)
        leave
        write(s"}")  // rc > 0
        leave
        write(s"} while(got_read);")  // do
        leave
        write(s"}")  // data != NULL
        leave
        write(s"} while(got_read);")    // do
        write

        write(s"if($destLabel.inputs[$destIndex].data == NULL) {")
        enter
        write(s"char *buf;")
        write(s"uint32_t c = APQ_StartRead($queue, &buf);")
        write(s"if(c > 0) {")
        enter
        write(s"$destLabel.inputs[$destIndex].data = ($vtype*)buf;")
        write(s"$destLabel.inputs[$destIndex].count = c;")
        leave
        write(s"}")
        leave
        write(s"}")

        write(s"if($destLabel.inputs[$destIndex].data != NULL) {")
        enter
        write(s"$destLabel.clock.count += 1;")
        write(s"ap_${destName}_push(&${destLabel}.priv, $destIndex, " +
              s"$destLabel.inputs[$destIndex].data, " +
              s"$destLabel.inputs[$destIndex].count);")
        write(s"return true;")
        leave
        write(s"} else {")
        enter
        write(s"if(!updated) {")
        enter
        for (sender <- senders) {
            write(s"${sender.label}_allocate(0);")
            write(s"${sender.label}_send(0);")
        }
        leave
        write(s"}")
        write(s"return false;")
        leave
        write(s"}")

        leave
        write(s"}")

        // "release"
        write(s"static void ${label}_release(int count)")
        write(s"{")
        enter
        write(s"APQ_FinishRead(${queue}, count);")
        leave
        write(s"}")

    }

}
