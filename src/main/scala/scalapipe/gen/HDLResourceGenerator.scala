package scalapipe.gen

import scalapipe._
import java.io.File

private[scalapipe] abstract class HDLResourceGenerator(
        val sp: ScalaPipe,
        val device: Device
    ) extends ResourceGenerator {

    protected val host  = device.host
    protected val id    = device.index

    protected val streams = sp.streams.filter { s =>
        s.sourceKernel.device == device || s.destKernel.device == device
    }

    protected val inputStreams = streams.filter { s =>
        s.destKernel.device == device && s.sourceKernel.device != device
    }

    protected val outputStreams = streams.filter { s =>
        s.sourceKernel.device == device && s.destKernel.device != device
    }

    protected val internalStreams = streams.filter { s =>
        s.sourceKernel.device == device && s.destKernel.device == device
    }

    protected val kernels = sp.instances.filter { _.device == device }

    protected val ramWidth = sp.parameters.get[Int]('memoryWidth)
    protected val ramAddrWidth = sp.parameters.get[Int]('memoryAddrWidth)

    protected def ramDepth = kernels.map(_.kernelType.ramDepth).sum

    private def getAddrWidth(width: Int): Int = {
        val bytes = round2(width) / 8
        val baseBytes = ramWidth / 8
        if (baseBytes > bytes) {
            val delta = log2(baseBytes) - log2(bytes)
            ramAddrWidth + delta
        } else if (baseBytes < bytes) {
            val delta = log2(bytes) - log2(baseBytes)
            ramAddrWidth - delta
        } else {
            ramAddrWidth
        }
    }

    protected class TimeTrial(val streams: Traversable[Stream]) {

        private def count(f : Measure => Boolean): Int = {
            val measures = streams.flatMap { s => s.measures }
            val filtered = measures.filter { m => f(m) }
            filtered.size
        }

        val amCount = count { m =>
            !m.useQueueMonitor &&
                (m.useInputActivity ||
                 m.useOutputActivity ||
                 m.useFullActivity)
        }
        val qmCount = count { m => m.useQueueMonitor }
        val imCount = count { m =>
            !m.useQueueMonitor && (m.useInterPush || m.useInterPop)
        }
        val lmCount = 0
        val hmCount = 0
        val needTimeTrial =
            amCount > 0 ||
            qmCount > 0 ||
            imCount > 0 ||
            hmCount > 0

    }

    /** Append make rules.
     * The following rules should be emitted:
     *     syn[id]      - Synthesize HDL
     *     build[id]    - Build a bitfile
     *     flow[id]     - Synthesize and build
     *     sim[id]      - Simulate
     *     install[id] - Install the bitfile
     */
    def getRules: String

    /** Emit the files.
     * Sub-classes should override this method and call it to generate
     * the internal components.
     */
    def emit(dir: File) {
        emitInternal(dir)
        emitMemorySpec(dir)
    }

    private def emitMemorySpec(dir: File) {
        val gen = new MemorySpecGenerator(sp, device, true)
        gen.emit(dir)
    }

    private def getDepthBits(stream: Stream): Int = {
        val depth = stream.parameters.get[Int]('fpgaQueueDepth)
        math.ceil(math.log(depth) / math.log(2.0)).toInt
    }

    private def emitRAMSignals(label: String, width: Int) {
        val rwidth = round2(width)
        val maskWidth = rwidth / 8
        val addrWidth = getAddrWidth(rwidth)
        write(s"wire [${addrWidth - 1}:0] ${label}_addr;")
        write(s"wire [${rwidth - 1}:0] ${label}_in;")
        write(s"wire [${rwidth - 1}:0] ${label}_out;")
        write(s"wire [${maskWidth - 1}:0] ${label}_mask;")
        write(s"wire ${label}_re;")
        write(s"wire ${label}_we;")
        write(s"wire ${label}_ready;")
    }

    private def emitBRAM(label: String, width: Int, depth: Int) {
        val rwidth = round2(width)
        val addrWidth = getAddrWidth(rwidth)
        write(s"sp_ram #(.WIDTH($rwidth), .DEPTH($depth), "  +
              s".ADDR_WIDTH($addrWidth))")
        enter
        write(s"$label (")
        enter
        write(s".clk(clk),")
        write(s".rst(rst),")
        write(s".addr(${label}_addr),")
        write(s".din(${label}_in),")
        write(s".dout(${label}_out),")
        write(s".mask(${label}_mask),")
        write(s".re(${label}_re),")
        write(s".we(${label}_we),")
        write(s".ready(${label}_ready)")
        leave
        write(s");")
        leave
    }

    private def emitFIFO(label: String, width: Int, addrWidth: Int) {

        write(s"wire [${width - 1}:0] ${label}_dout;")
        write(s"wire [${width - 1}:0] ${label}_din;")
        write(s"wire ${label}_avail;")
        write(s"wire ${label}_read;")
        write(s"wire ${label}_write;")
        write(s"wire ${label}_full;")

        if (sp.parameters.get[Boolean]('bram)) {
            val depth = 1 << addrWidth
            write(s"sp_fifo #(.WIDTH($width), .DEPTH($depth))")
            enter
            write(s"fifo_${label}(")
            enter
            write(s".clk(clk),")
            write(s".rst(rst),")
            write(s".din(${label}_din),")
            write(s".dout(${label}_dout),")
            write(s".re(${label}_read),")
            write(s".we(${label}_write),")
            write(s".avail(${label}_avail),")
            write(s".full(${label}_full)")
            leave
            write(s");")
            leave
        }

    }

    private def emitKernels {

        for (kernel <- kernels) {
            val label = kernel.label
            val kernelName = s"kernel_${kernel.name}"
            val ramDepth = kernel.kernelType.ramDepth

            if (ramDepth > 0) {
                val wordBytes = ramWidth / 8
                val name = s"ram_${label}"
                write(s"wire [${ramAddrWidth - 1}:0] ${name}_addr;")
                write(s"wire [${ramWidth - 1}:0] ${name}_in;")
                write(s"wire [${ramWidth - 1}:0] ${name}_out;")
                write(s"wire [${wordBytes - 1}:0] ${name}_mask;")
                write(s"wire ${name}_re;")
                write(s"wire ${name}_we;")
                write(s"wire ${name}_ready;")
            }

            write(s"wire ${kernel.label}_running;")
            write(kernelName)
            enter
            if (!kernel.configs.isEmpty) {
                val configs = kernel.configs.map { case (name, value) =>
                    s".$name($value)"
                }.mkString(", ")
                write(s"#($configs)")
            }
            write(s"${label}(")
            enter
            write(s".clk(clk), .rst(rst),")
            write(s".running(${label}_running)")
            for (i <- kernel.getInputs) {
                val destPort = i.destKernel.inputName(i.destPort)
                write(s", .input_$destPort(${i.label}_dout)")
                write(s", .avail_$destPort(${i.label}_avail)")
                write(s", .read_$destPort(${i.label}_read)")
            }
            for (o <- kernel.getOutputs) {
                val srcPort = o.sourceKernel.outputName(o.sourcePort)
                write(s", .output_$srcPort(${o.label}_din)")
                write(s", .write_$srcPort(${o.label}_write)")
                write(s", .full_$srcPort(${o.label}_full)")
            }
            if (ramDepth > 0) {
                val name = s"ram_${label}"
                write(s", .ram_addr(${name}_addr)")
                write(s", .ram_in(${name}_out)")
                write(s", .ram_out(${name}_in)")
                write(s", .ram_mask(${name}_mask)")
                write(s", .ram_re(${name}_re)")
                write(s", .ram_we(${name}_we)")
                write(s", .ram_ready(${name}_ready)")
            }
            leave
            write(");")
            leave
            if (ramDepth > 0 && sp.parameters.get[Boolean]('bram)) {
                emitBRAM(s"ram_${label}", ramWidth, ramDepth)
            }
            write
        }

        // Connect the running signal.
        val runningSignals = kernels.map { k => s"${k.label}_running" }
        val activeSignals = runningSignals :+ "ram_full"
        write("assign running = " + activeSignals.mkString("|") + ";")

    }

    private def emitInternalStreams {
        for (stream <- internalStreams) {

            val label = stream.label
            val width = round2(stream.valueType.bits)
            val addrWidth = getDepthBits(stream)

            // Connect the FIFO.
            emitFIFO(label, width, addrWidth)
            write

            // Add edge instrumentation.
            for (m <- stream.measures) {
                var qmIndex = m.sourceQueueOffset
                var amIndex = m.sourceActivityOffset
                var imIndex = m.sourceInterOffset
                if (m.useQueueMonitor) {
                    write(s"assign qRst[$qmIndex] = rst;")
                    write(s"assign qWr[$qmIndex] = ${stream.label}_write;")
                    write(s"assign qRd[$qmIndex] = ${stream.label}_read " +
                          s" && ${stream.label}_avail;")
                    qmIndex += 1
                }
                if (m.useInputActivity) {
                    write(s"assign amTap[$amIndex] = ${stream.label}_write;")
                    amIndex += 1
                }
                if (m.useOutputActivity) {
                    write(s"assign amTap[$amIndex] = ${stream.label}_read" +
                          s" && ${stream.label}_avail;")
                    amIndex += 1
                }
                if (m.useFullActivity) {
                    write(s"assign amTap[$amIndex] = ${stream.label}_full;")
                    amIndex += 1
                }
                if (m.useInterPush) {
                    write(s"assign imAvail[$imIndex] = 1;")
                    write(s"assign imRead[$imIndex] = ${stream.label}_write;")
                    imIndex += 1
                }
                if (m.useInterPop) {
                    write(s"assign imAvail[$imIndex] = ${stream.label}_avail;")
                    write(s"assign imRead[$imIndex] = ${stream.label}_read" +
                          s" && ${stream.label}_avail;")
                    imIndex += 1
                }
            }

        }
    }

    private def emitInputStreams {
        for (stream <- inputStreams) {
            val label = stream.label
            val width = stream.valueType.bits
            val addrWidth = getDepthBits(stream)
            val srcIndex = stream.index

            // Connect the FIFO.
            emitFIFO(label, width, addrWidth)
            write(s"assign ${label}_din = input${srcIndex}_data;")
            write(s"assign ${label}_write = input${srcIndex}_write;")
            write(s"assign input${srcIndex}_full = ${label}_full;")
            write

            // Add edge instrumentation.
            for (m <- stream.measures) {
                var qmIndex = m.sourceQueueOffset
                var amIndex = m.sourceActivityOffset
                var imIndex = m.sourceInterOffset
                if (m.useQueueMonitor) {
                    write(s"assign qRst[$qmIndex] = rst;")
                    write(s"assign qWr[$qmIndex] = input${srcIndex}_write;")
                    write(s"assign qRd[$qmIndex] = ${label}_read" +
                          s" && ${label}_avail;")
                    qmIndex += 1
                }
                if (m.useInputActivity) {
                    write(s"assign amTap[$amIndex] = input${srcIndex}_write;")
                    amIndex += 1
                }
                if (m.useOutputActivity) {
                    write(s"assign amTap[$amIndex] = " +
                          s"{label}_read && ${label}_avail;")
                    amIndex += 1
                }
                if (m.useFullActivity) {
                    write(s"assign amTap[$amIndex] = input${srcIndex}_full;")
                    amIndex += 1
                }
                if (m.useInterPush) {
                    write(s"assign imAvail[$imIndex] = 1;")
                    write(s"assign imRead[$imIndex] = input${srcIndex}_write;")
                    imIndex += 1
                }
                if (m.useInterPop) {
                    write(s"assign imAvail[$imIndex] = ${label}_avail;")
                    write(s"assign imRead[$imIndex] = " +
                          s"${label}_read && ${label}_avail;")
                    imIndex += 1
                }
            }

        }
    }

    private def emitOutputStreams {
        for (stream <- outputStreams) {

            val label = stream.label
            val width = stream.valueType.bits
            val addrWidth = getDepthBits(stream)
            val destIndex = stream.index

            // Hook up the FIFO.
            emitFIFO(label, width, addrWidth)
            write(s"assign output${destIndex}_data = ${label}_dout;")
            write(s"assign output${destIndex}_avail = ${label}_avail;")
            write(s"assign ${label}_read = output${destIndex}_read;")
            write

            // Add edge instrumentation.
            for (m <- stream.measures) {
                var qmIndex = m.sourceQueueOffset
                var amIndex = m.sourceActivityOffset
                var imIndex = m.sourceInterOffset
                if (m.useQueueMonitor) {
                    write(s"assign qRst[$qmIndex] = rst;")
                    write(s"assign qWr[$qmIndex] = ${stream.label}_write;")
                    write(s"assign qRd[$qmIndex] = output${destIndex}_read" +
                          s" && ${stream.label}_avail;")
                    qmIndex += 1
                }
                if (m.useInputActivity) {
                    write(s"assign amTap[$amIndex] = ${stream.label}_write;")
                    amIndex += 1
                }
                if (m.useOutputActivity) {
                    write(s"assign amTap[$amIndex] = output" +
                          s"${destIndex}_read && ${stream.label}_avail;")
                    amIndex += 1
                }
                if (m.useFullActivity) {
                    write(s"assign amTap[$amIndex] = ${stream.label}_full;")
                    amIndex += 1
                }
                if (m.useInterPush) {
                    write(s"assign imAvail[$imIndex] = 1;")
                    write(s"assign imRead[$imIndex] = ${stream.label}_write;")
                    imIndex += 1
                }
                if (m.useInterPop) {
                    write(s"assign imAvail[$imIndex] = ${stream.label}_avail;")
                    write(s"assign imRead[$imIndex] = " +
                          s"output${destIndex}_read && ${stream.label}_avail;")
                    imIndex += 1
                }
            }
        }
    }

    private def emitMemory {

        if (sp.parameters.get[Boolean]('bram)) {
            write(s"assign ram_addr = 1'bx;")
            write(s"assign ram_din = 1'bx;")
            write(s"assign ram_re = 1'b0;")
            write(s"assign ram_we = 1'b0;")
            write(s"assign ram_mask = 1'bx;")
            return
        }

        write(s"mem m(")
        enter
        write(s".clk(clk),")
        write(s".rst(rst),")
        write(s".port0_addr(ram_addr),")
        write(s".port0_din(ram_data_from_main),")
        write(s".port0_dout(ram_data_to_main),")
        write(s".port0_re(ram_re),")
        write(s".port0_we(ram_we),")
        write(s".port0_mask(ram_mask),")
        write(s".port0_full(ram_full),")
        write(s".port0_avail(ram_avail)")
        for (s <- streams) {
            val port = s"fifo${s.index}"
            val label = s.label
            write(s", .${port}_din(${label}_din)")
            write(s", .${port}_dout(${label}_dout)")
            write(s", .${port}_avail(${label}_avail)")
            write(s", .${port}_full(${label}_full)")
            write(s", .${port}_re(${label}_read)")
            write(s", .${port}_we(${label}_write)")
        }
        val externalStreams = sp.streams.filter { s =>
            s.sourceKernel.device != device && s.destKernel.device != device
        }
        for (s <- externalStreams) {
            val port = s"fifo${s.index}"
            write(s", .${port}_din(1'bx)")
            write(s", .${port}_dout()")
            write(s", .${port}_re(1'b0)")
            write(s", .${port}_we(1'b0)")
            write(s", .${port}_avail()")
            write(s", .${port}_full()")
        }
        for (k <- kernels) {
            val port = s"subsystem${k.index}"
            if (k.kernelType.ramDepth > 0) {
                val label = s"ram_${k.label}"
                write(s", .${port}_addr(${label}_addr)")
                write(s", .${port}_out(${label}_out)")
                write(s", .${port}_in(${label}_in)")
                write(s", .${port}_mask(${label}_mask)")
                write(s", .${port}_re(${label}_re)")
                write(s", .${port}_we(${label}_we)")
                write(s", .${port}_ready(${label}_ready)")
            }
        }
        leave
        write(s");")

    }

    private def emitInternal(dir: File) {

        val tt = new TimeTrial(streams)
        val mainDataWidth = sp.parameters.get[Int]('dramDataWidth)
        val mainAddrWidth = sp.parameters.get[Int]('dramAddrWidth)
        val mainMaskBits = mainDataWidth / 8

        write(s"module fpga$id(")
        enter
        write(s"input wire clk,")
        write(s"input wire rst,")
        write(s"output wire [${mainAddrWidth - 1}:0] ram_addr,")
        write(s"input wire [${mainDataWidth - 1}:0] ram_data_from_main,")
        write(s"output wire [${mainDataWidth - 1}:0] ram_data_to_main,")
        write(s"output wire [${mainMaskBits - 1}:0] ram_mask,")
        write(s"output wire ram_re,")
        write(s"output wire ram_we,")
        write(s"input wire ram_full,")
        write(s"input wire ram_avail,")
        for (i <- inputStreams) {
            val index = i.index
            val width = i.valueType.bits
            write(s"input wire [${width - 1}:0] input${index}_data,")
            write(s"input wire input${index}_write,")
            write(s"output wire input${index}_full,")
        }
        for (o <- outputStreams) {
            val index = o.index
            val width = o.valueType.bits
            write(s"output wire [${width - 1}:0] output${index}_data,")
            write(s"output wire output${index}_avail,")
            write(s"input wire output${index}_read,")
        }
        write(s"output wire running")
        if (tt.amCount > 0) {
            write(s", output wire [${tt.amCount - 1}:0] amTap")
        }
        if (tt.lmCount > 0) {
            write(s", output wire [${tt.lmCount - 1}:0] lmTap")
        }
        if (tt.qmCount > 0) {
            write(s", output wire [${tt.qmCount - 1}:0] qRst")
            write(s", output wire [${tt.qmCount - 1}:0] qWr")
            write(s", output wire [${tt.qmCount - 1}:0] qRd")
        }
        if (tt.imCount > 0) {
            write(s", output wire [${tt.imCount - 1}:0] imAvail")
            write(s", output wire [${tt.imCount - 1}:0] imRead")
        }
        leave
        write(");")
        enter
        write

        emitInputStreams
        emitOutputStreams
        emitInternalStreams
        emitKernels
        emitMemory

        leave
        write("endmodule")
        val filename = s"fpga_${device.label}.v"
        writeFile(dir, filename)

    }

}
