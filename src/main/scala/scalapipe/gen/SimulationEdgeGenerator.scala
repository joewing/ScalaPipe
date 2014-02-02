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

    private def emitWriteFunction {
        write("static pthread_mutex_t sim_mutex = PTHREAD_MUTEX_INITIALIZER;")
        write("static void sim_write(int fd, const char *data, " +
              "size_t size, uint32_t count)")
        write("{")
        enter
        write("pthread_mutex_lock(&sim_mutex);")
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
        write("pthread_mutex_unlock(&sim_mutex);")
        leave
        write("}")
    }

    private def emitReadFunction {
        write("static uint32_t sim_read(int fd, char *data, " +
              "ssize_t size, uint32_t max_count)")
        write("{")
        enter

        write("uint32_t count = 0;")
        write("ssize_t offset = 0;")
        write("for(;;) {")
        enter

        write("ssize_t rc = read(fd, &data[offset], size - offset);")
        write("if(rc > 0) {")
        enter
        write("offset += rc;")
        write("if(offset == size) {")
        enter
        write("count += 1;")
        write("data += size;")
        write("offset = 0;")
        leave
        write("}")
        leave
        write("}")

        write("if(offset == 0) {")
        enter
        write("int temp = 0;")
        write("write(fd, &temp, sizeof(temp));")
        write("return count;")
        leave
        write("}")

        leave
        write("}")
        leave
        write("}")
    }


    override def emitGlobals(streams: Traversable[Stream]) {

        val devices = getDevices(streams)
        val senderStreams = devices.flatMap(d => getSenderStreams(d, streams))
        val receiverStreams = devices.flatMap { d =>
            getReceiverStreams(d, streams)
        }

        if (!senderStreams.isEmpty) {
            emitWriteFunction
        }
        if(!receiverStreams.isEmpty) {
            emitReadFunction
        }

        for (s <- senderStreams) {
            writeSendFunctions(s)
        }

        for (s <- receiverStreams) {
            writeReceiveFunctions(s, senderStreams)
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
            write(s"$queue = (SPQ*)malloc(spq_get_size($depth, " +
                  s"sizeof($vtype)));")
            write(s"spq_init($queue, $depth, sizeof($vtype));")

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

    private def writeSendFunctions(stream: Stream) {

        val label = stream.label
        val queue = "q_" + stream.label
        val vtype = stream.valueType
        val fd = "stream" + stream.label

        val itemCount = stream.valueType match {
            case avt: ArrayValueType    => avt.length
            case _                      => 1
        }

        val itemSize = stream.valueType match {
            case avt: ArrayValueType    => avt.itemType.bits / 8
            case _                      => stream.valueType.bits / 8
        }

        // Globals.
        write(s"static int stream$label = -1;")
        write(s"static SPQ *$queue = NULL;")

        // "get_free"
        write(s"static int ${label}_get_free()")
        write(s"{")
        enter
        write(s"return spq_get_free($queue);")
        leave
        write(s"}")

        // "allocate"
        write(s"static void *${label}_allocate()")
        write(s"{")
        enter
        write(s"return spq_start_write($queue, 1);")
        leave
        write(s"}")

        // "send"
        write(s"static void ${label}_send()")
        write(s"{")
        enter
        write(s"spq_finish_write($queue, 1);")
        write(s"char *data;")
        write(s"const uint32_t c = spq_start_read($queue, &data);")
        write(s"sim_write($fd, data, sizeof($vtype), c);")
        write(s"spq_finish_read($queue, c);")
        leave
        write(s"}")

    }

    private def writeReceiveFunctions(stream: Stream,
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
        write(s"static SPQ *$queue = NULL;")

        // "get_available"
        write(s"static int ${label}_get_available()")
        write(s"{")
        enter
        write(s"uint32_t c = spq_get_free($queue);")
        write(s"char *ptr = spq_start_blocking_write($queue, c);")
        write(s"c = sim_read($fd, ptr, sizeof($vtype), c);")
        write(s"spq_finish_write($queue, c);")
        write(s"return spq_get_used($queue);")
        leave
        write(s"}")

        // "read_value"
        write(s"static void *${label}_read_value()")
        write(s"{")
        enter
        for (s <- senders) {
            val sfd = s"stream${s.label}"
            write(s"sim_write($sfd, NULL, 0, 0);")
        }
        write(s"uint32_t c = spq_get_free($queue);")
        write(s"char *ptr = spq_start_blocking_write($queue, c);")
        write(s"c = sim_read($fd, ptr, sizeof($vtype), c);")
        write(s"spq_finish_write($queue, c);")
        write(s"if(spq_start_read($queue, &ptr) > 0) {")
        enter
        write(s"return ptr;")
        leave
        write(s"}")
        write(s"return NULL;")
        leave
        write(s"}")

        // "release"
        write(s"static void ${label}_release()")
        write(s"{")
        enter
        write(s"spq_finish_read($queue, 1);")
        leave
        write(s"}")
    }

}
