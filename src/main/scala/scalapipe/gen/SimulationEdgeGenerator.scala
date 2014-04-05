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
        write("#include <errno.h>")
    }

    private def emitWriteFunction {
        write("static pthread_mutex_t sim_mutex = PTHREAD_MUTEX_INITIALIZER;")
        write("static void sim_write(int fd, const char *data, " +
              "size_t size, int count)")
        write("{")
        enter
        write("pthread_mutex_lock(&sim_mutex);")
        write("const size_t total = size * (count > 0 ? count : 0) + 4;")
        write("char *buffer = (char*)alloca(total);")
        write("*(int*)&buffer[0] = count;")
        write("if(count > 0) {")
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
        write("static int sim_read(int fd, char *data, " +
              "ssize_t size, uint32_t max_count)")
        write("{")
        enter

        write("ssize_t offset = 0;")
        write("char ch = 0;")
        write("ssize_t rc = read(fd, &ch, 1);")
        write("if(rc <= 0) {")
        enter
        write("return 0;")
        leave
        write("} else if(ch == 0) {")
        enter
        write("return -1;")
        leave
        write("}")
        write("while(offset < size) {")
        enter
        write("rc = read(fd, &data[offset], size - offset);")
        write("if(rc > 0) {")
        enter
        write("offset += rc;")
        leave
        write("}")
        leave
        write("}")
        write("return 1;")

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

        for (d <- devices) {
            val label = d.label
            write(s"static pid_t sim_${label}_pid = 0;")
        }
        write("static void stopSimulation()")
        write("{")
        enter
        for (d <- devices) {
            val label = d.label
            write(s"kill(sim_${label}_pid, SIGINT);")
            write(s"waitpid(sim_${label}_pid, NULL, 0);")
        }
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
            val depth = s.parameters.get[Int]('queueDepth)
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
                      s"O_RDWR | O_NONBLOCK | O_SYNC);")
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

        // Start the simulation(s).
        val devices = getDevices(streams)
        for (d <- devices) {
            val label = d.label
            write(s"sim_${label}_pid = fork();")
            write(s"if(sim_${label}_pid == 0) {")
            enter
            write(s"""int rc = execlp(\"vvp\", \"vvp\", """ +
                  s"""\"hdl_${label}\", NULL);""")
            write(s"if(rc < 0) {")
            enter
            write(s"""perror(\"could not run hdl_${label}\");""")
            write(s"exit(-1);");
            leave
            write(s"}")
            leave
            write(s"}")
        }

        write(s"atexit(stopSimulation);")

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
        write(s"static char active$label = 1;")

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

        // "finish"
        write(s"static void ${label}_finish()")
        write(s"{")
        enter
        write(s"active$label = 0;")
        write(s"sim_write(stream$label, NULL, 0, -1);")
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
        write(s"const uint32_t f = spq_get_free($queue);")
        write(s"char *ptr = spq_start_blocking_write($queue, f);")
        write(s"const int c = sim_read($fd, ptr, sizeof($vtype), f);")
        write(s"if(c >= 0) {")
        enter
        write(s"spq_finish_write($queue, c);")
        leave
        write(s"} else if(c < 0) {")
        enter
        write(s"sp_decrement(&${destLabel}.active_inputs);")
        leave
        write(s"}")
        write(s"return spq_get_used($queue);")
        leave
        write(s"}")

        // "read_value"
        write(s"static void *${label}_read_value()")
        write(s"{")
        enter
        write(s"const uint32_t f = spq_get_free($queue);")
        write(s"char *ptr = spq_start_blocking_write($queue, f);")
        write(s"const int c = sim_read($fd, ptr, sizeof($vtype), f);")
        write(s"if(c > 0) {")
        enter
        write(s"spq_finish_write($queue, c);")
        leave
        write(s"} else if(c == 0) {")
        enter
        for (s <- senders) {
            val srcLabel = s.sourceKernel.label
            val sfd = s"stream${s.label}"
            write(s"if(active${s.label}) {")
            enter
            write(s"sim_write($sfd, NULL, 0, 0);")
            leave
            write(s"}")
        }
        leave
        write(s"} else {")
        enter
        write(s"sp_decrement(&$destLabel.active_inputs);")
        leave
        write(s"}")
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
