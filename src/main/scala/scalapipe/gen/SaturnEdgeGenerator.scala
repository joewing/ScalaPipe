package scalapipe.gen

import scalapipe._

private[scalapipe] class SaturnEdgeGenerator(
        val sp: ScalaPipe
    ) extends EdgeGenerator(Platforms.HDL) {

    override def emitCommon() {
        write("#include <termios.h>")
        write("#include <unistd.h>")
        write("#include <sys/types.h>")
        write("#include <sys/stat.h>")
        write("#include <fcntl.h>")
        write("#include <errno.h>")
    }

    private def emitProcessFunction(senderStreams: Traversable[Stream],
                                    receiverStreams: Traversable[Stream]) {

        write("static void usb_write(const char *buffer, size_t count)")
        write("{")
        enter
        write("size_t offset = 0;")
        write("while(offset < count) {")
        enter
        write("ssize_t rc = write(usb_fd, &buffer[offset], count - offset);")
        write("if(rc < 0) {")
        enter
        write("if(errno != EAGAIN) {")
        enter
        write("perror(\"device write failed\");")
        write("exit(-1);")
        leave
        write("}")
        leave
        write("} else {")
        enter
        write("offset += rc;")
        leave
        write("}")
        leave
        write("}")
        leave
        write("}")
        write

        write("static char usb_try_read(char *buffer, size_t count)")
        write("{")
        enter
        write("size_t offset;")
        write("ssize_t rc = read(usb_fd, buffer, count);")
        write("if(rc < 0) {")
        enter
        write("if(errno != EAGAIN) {")
        enter
        write("perror(\"device read failed\");")
        write("exit(-1);")
        leave
        write("}")
        write("usleep(100);")
        write("return 0;")
        leave
        write("}")
        write("offset = rc;")
        write("while(offset < count) {")
        enter
        write("rc = read(usb_fd, &buffer[offset], count - offset);")
        write("if(rc < 0) {")
        enter
        write("if(errno != EAGAIN) {")
        enter
        write("perror(\"device read failed\");")
        write("exit(-1);")
        leave
        write("}")
        leave
        write("} else {")
        enter
        write("offset += rc;")
        leave
        write("}")
        leave
        write("}")
        write("return 1;")
        leave
        write("}")
        write

        write("static void usb_read(char *buffer, size_t count)")
        write("{")
        enter
        write("while(!usb_try_read(buffer, count));")
        leave
        write("}")
        write

        write("static void process_usb()")
        write("{")
        enter
        write(s"static char first = 1;")
        write(s"pthread_mutex_lock(&usb_mutex);")
        write(s"if(usb_stopped) {")
        enter
        write("pthread_mutex_unlock(&usb_mutex);")
        write(s"return;")
        leave
        write(s"}")
        write(s"if(first) {")
        enter
        write(s"struct termios ios;")
        write(s"char buffer[256];")
        write(s"tcgetattr(usb_fd, &ios);")
        write(s"cfmakeraw(&ios);")
        write(s"tcsetattr(usb_fd, 0, &ios);")
        write(s"tcflush(usb_fd, TCIOFLUSH);")
        write(s"memset(buffer, 254, sizeof(buffer));")
        write(s"usb_write(buffer, sizeof(buffer));")
        write(s"tcdrain(usb_fd);")
        write(s"tcflush(usb_fd, TCIFLUSH);")
        write(s"buffer[0] = 0;")
        write(s"usb_write(buffer, 1);")
        write(s"first = 0;")
        leave
        write(s"}")

        // Read the status.
        // The first bytes are "full" indicators for each
        // host->device stream.  The most significant bit is set if
        // the stream can not accept data.
        // The last byte indicates a stream ID for a device->host
        // stream.  This byte is 0 if no stream has data or 255 if
        // no kernels are running.
        // Finally, a byte indicating a stream ID for a device->host
        // transfer.  This byte is 0 to indicate no stream.
        val senderCount = senderStreams.size
        write(s"unsigned char status[$senderCount + 1];")
        write(s"if(!usb_try_read((char*)status, sizeof(status))) {")
        enter
        write(s"pthread_mutex_unlock(&usb_mutex);")
        write(s"return;")
        leave
        write(s"}")

        // Read data from the device from the indicated device->host stream.
        write(s"char *ptr = NULL;")
        write(s"switch(status[$senderCount]) {")
        write(s"case 0: // No stream")
        enter
        write(s"break;")
        leave
        write(s"case 255: // Stopped")
        enter
        write(s"{")
        enter
        for (s <- receiverStreams) {
            val destLabel = s.destKernel.label
            write(s"sp_decrement(&${destLabel}.active_inputs);")
        }
        write(s"usb_stopped = true;")
        write("pthread_mutex_unlock(&usb_mutex);")
        write(s"return;")
        leave
        write(s"}")
        leave
        for (s <- receiverStreams) {
            val index = s.index
            val label = s.label
            val vtype = s.valueType
            val queue = "q_" + label
            write(s"case $index:")
            enter
            write(s"ptr = spq_start_blocking_write($queue, 1);")
            write(s"usb_read(ptr, sizeof($vtype));")
            write(s"spq_finish_write($queue, 1);")
            write(s"break;")
            leave
        }
        write(s"default:")
        enter
        write(s"""fprintf(stderr, "Invalid device->host stream: %u\\n", """ +
              s"""status[$senderCount]);""")
        write(s"""exit(-1);""")
        leave
        write(s"}")

        // Select a host->device stream with data available that's
        // accepting data.  If there is no matching stream, we send 0.
        // If there is no data remaining, we send 255.
        write(s"unsigned char ch = 0;")
        write(s"uint32_t count = 0;")
        for (s <- senderStreams) {
            val label = s.label
            val vtype = s.valueType
            val index = s.index
            val queue = "q_" + label
            write(s"count = spq_start_read($queue, &ptr);")
            write(s"if(count > 0 && memchr(status, $index, $senderCount)) {")
            enter
            write(s"ch = $index;")
            write(s"usb_write((char*)&ch, 1);")
            write(s"usb_write(ptr, sizeof($vtype));")
            write(s"spq_finish_read($queue, 1);")
            write(s"goto data_sent;")
            leave
            write(s"}")
        }

        // If we got here, there's no data to send.
        write(s"ch = usb_active_inputs == 0 ? 255 : 0;")
        write(s"usb_write((char*)&ch, 1);")

        writeLeft("data_sent:")
        write(s"tcdrain(usb_fd);")
        write("pthread_mutex_unlock(&usb_mutex);")
        leave
        write("}")
    }

    override def emitGlobals(streams: Traversable[Stream]) {

        val devices = getDevices(streams)
        val senderStreams = devices.flatMap(d => getSenderStreams(d, streams))
        val receiverStreams = devices.flatMap { d =>
            getReceiverStreams(d, streams)
        }
        val senderCount = senderStreams.size

        write(s"static pthread_mutex_t usb_mutex = PTHREAD_MUTEX_INITIALIZER;")
        write(s"static int usb_fd = -1;")
        write(s"static volatile uint32_t usb_active_inputs = $senderCount;")
        write(s"static bool usb_stopped = false;")

        for (s <- senderStreams ++ receiverStreams) {
            val label = s.label
            val queue = s"q_$label"
            write(s"static SPQ *$queue = NULL;")
        }

        emitProcessFunction(senderStreams, receiverStreams)
        senderStreams.foreach(emitSendFunctions)
        receiverStreams.foreach(emitReceiveFunctions)

    }

    override def emitInit(streams: Traversable[Stream]) {

        // Open the USB interface.
        val usb_file1 = "/dev/serial/by-id/usb-FTDI_" +
                        "Saturn_Spartan_6_FPGA_Module_" +
                        "FTXLRCAZ-if01-port0"
        val usb_file2 = "/dev/cu.usbserial-FTXLRCAZB"
        write(s"""usb_fd = open("$usb_file1", O_RDWR | O_SYNC | O_NONBLOCK);""")
        write(s"if(usb_fd < 0) {")
        enter
        write(s"""usb_fd = open("$usb_file2", O_RDWR | O_SYNC | O_NONBLOCK);""")
        leave
        write(s"}")
        write(s"if(usb_fd < 0) {")
        enter
        write(s"""perror("could not open device");""")
        write(s"exit(-1);")
        leave
        write(s"}")

        // Create the FIFOs.
        for (s <- streams) {
            val label = s.label
            val depth = s.parameters.get[Int]('queueDepth)
            val queue = s"q_$label"
            val vtype = s.valueType
            write(s"$queue = (SPQ*)malloc(spq_get_size($depth, " +
                  s"sizeof($vtype)));")
            write(s"spq_init($queue, $depth, sizeof($vtype));")
        }

    }

    private def emitSendFunctions(stream: Stream) {

        val label = stream.label
        val queue = s"q_$label"
        val vtype = stream.valueType

        // "get_free"
        write(s"static int ${label}_get_free()")
        write(s"{")
        enter
        write(s"process_usb();")
        write(s"return spq_get_free($queue);")
        leave
        write(s"}")

        // "allocate"
        write(s"static void *${label}_allocate()")
        write(s"{")
        enter
        write(s"void *ptr = spq_start_write($queue, 1);")
        write(s"if(!ptr) {")
        enter
        write(s"process_usb();")
        leave
        write(s"}")
        write(s"return ptr;")
        leave
        write(s"}")

        // "send"
        write(s"static void ${label}_send()")
        write(s"{")
        enter
        write(s"spq_finish_write($queue, 1);")
        write(s"process_usb();")
        leave
        write(s"}")

        // "finish"
        write(s"static void ${label}_finish()")
        write(s"{")
        enter
        write(s"sp_decrement(&usb_active_inputs);")
        write(s"process_usb();")
        leave
        write(s"}")

    }

    private def emitReceiveFunctions(stream: Stream) {

        val label = stream.label
        val queue = s"q_$label"


        // "get_available."
        write(s"static int ${label}_get_available()")
        write(s"{")
        enter
        write(s"process_usb();")
        write(s"return spq_get_used($queue);")
        leave
        write(s"}")

        // "read_value"
        write(s"static void *${label}_read_value()")
        write(s"{")
        enter
        write(s"char *ptr = NULL;")
        write(s"if(spq_start_read($queue, &ptr) > 0) {")
        enter
        write(s"return ptr;")
        leave
        write(s"}")
        write(s"process_usb();")
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

    override def emitDestroy(streams: Traversable[Stream]) {
        for (s <- streams) {
            val label = s.label
            val queue = s"q_$label"
            write(s"free($queue);")
        }
        write(s"tcflush(usb_fd, TCIOFLUSH);")
        write(s"close(usb_fd);")
    }

}
