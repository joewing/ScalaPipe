package scalapipe.gen

import scalapipe._

private[scalapipe] class SaturnEdgeGenerator(
        val sp: ScalaPipe
    ) extends EdgeGenerator(Platforms.HDL) {

    override def emitCommon() {
    }

    private def emitProcessFunction(senderStreams: Traversable[Stream],
                                    receiverStreams: Traversable[Stream]) {

        write("static void process_usb()")
        write("{")
        enter
        write("pthread_mutex_lock(&usb_mutex);")

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
        write(s"size_t rc = fread(status, sizeof(status), 1, usb_fd);")
        write(s"if(rc != 1) {")
        enter
        write(s"""perror("device read failed");""")
        write(s"exit(-1);")
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
        write(s"if(usb_active_inputs == 0 && !usb_stopped) {")
        enter
        for (s <- receiverStreams) {
            val destLabel = s.destKernel.label
            write(s"sp_decrement(&${destLabel}.active_inputs);")
        }
        write(s"usb_stopped = true;")
        leave
        write(s"}")
        write(s"break;")
        leave
        for (s <- receiverStreams) {
            val index = s.index
            val label = s.label
            val vtype = s.valueType
            val queue = "q_" + label
            write(s"case $index:")
            enter
            write(s"ptr = spq_start_blocking_write($queue, 1);")
            write(s"rc = fread(ptr, sizeof($vtype), 1, usb_fd);")
            write(s"if(rc != 1) {")
            enter
            write(s"""perror("device read failed");""")
            write(s"exit(-1);")
            leave
            write(s"}")
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
            write(s"fwrite(&ch, 1, 1, usb_fd);")
            write(s"fwrite(ptr, sizeof($vtype), 1, usb_fd);")
            write(s"spq_finish_read($queue, 1);")
            write(s"goto data_sent;")
            leave
            write(s"}")
        }

        // If we got here, there's no data to send.
        write(s"ch = usb_active_inputs == 0 ? 255 : 0;")
        write(s"fwrite(&ch, 1, 1, usb_fd);")

        writeLeft("data_sent:")
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
        write(s"static FILE *usb_fd = NULL;")
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
        val usb_file = "/dev/serial/by-id/usb-FTDI_" +
                       "Saturn_Spartan_6_FPGA_Module_" +
                       "FTXLRCAZ-if01-port0"
        write(s"""usb_fd = fopen("$usb_file", "r+");""")
        write(s"if(usb_fd == NULL) {")
        enter
        write(s"""perror("could not open device");""")
        write(s"exit(-1);")
        leave
        write("}")

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
        write(s"process_usb();")
        write(s"return spq_start_write($queue, 1);")
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
        write(s"process_usb();")
        write(s"void *ptr = NULL;")
        write(s"spq_start_write($queue, 1);")
        write(s"return ptr;")
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
    }

}
