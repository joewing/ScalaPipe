package scalapipe.gen

import scalapipe._
import java.io.File

private[scalapipe] abstract class HDLResourceGenerator(
        val sp: ScalaPipe,
        val device: Device
    ) extends ResourceGenerator {

    protected val host  = device.host
    protected val id    = device.index

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
    }

    private def getDepthBits: Int = {
        val depth = sp.parameters.get[Int]('fpgaQueueDepth)
        math.ceil(math.log(depth) / math.log(2.0)).toInt
    }

    private def emitInternal(dir: File) {

        val streams = sp.streams.filter { s =>
            s.sourceKernel.device == device || s.destKernel.device == device
        }
        val tt = new TimeTrial(streams)
        val inStreams = streams.filter { s =>
            s.destKernel.device == device && s.sourceKernel.device != device
        }
        val outStreams = streams.filter { s =>
            s.sourceKernel.device == device && s.destKernel.device != device
        }
        val internalStreams = streams.filter { s =>
            s.sourceKernel.device == device && s.destKernel.device == device
        }

        write(s"module fpga$id(")
        enter
        write(s"input wire clk,")
        write(s"input wire rst")
        for (i <- inStreams) {
            val index = i.index
            val width = i.valueType.bits
            write(s",")
            write(s"input wire [${width - 1}:0] I${index}input,")
            write(s"input wire I${index}write,")
            write(s"output wire I${index}afull")
        }
        for (o <- outStreams) {
            val index = o.index
            val width = o.valueType.bits
            write(s",")
            write(s"output wire [${width - 1}:0] O${index}output,")
            write(s"output wire O${index}avail,")
            write(s"input wire O${index}read")
        }
        if (tt.amCount > 0) {
            write(s",")
            write(s"output wire [${tt.amCount - 1}:0] amTap")
        }
        if (tt.lmCount > 0) {
            write(s",")
            write(s"output wire [${tt.lmCount - 1}:0] lmTap")
        }
        if (tt.qmCount > 0) {
            write(s",")
            write(s"output wire [${tt.qmCount - 1}:0] qRst,")
            write(s"output wire [${tt.qmCount - 1}:0] qWr,")
            write(s"output wire [${tt.qmCount - 1}:0] qRd")
        }
        if (tt.imCount > 0) {
            write(s",")
            write(s"output wire [${tt.imCount - 1}:0] imAvail,")
            write(s"output wire [${tt.imCount - 1}:0] imRead")
        }
        leave
        write(");")
        enter
        write

        // Wires
        for (i <- inStreams) {
            val valueType = i.valueType.baseType
            val width = i.valueType.bits
            write(s"wire [${width - 1}:0] ${i.label}_input;")
            write(s"wire [${width - 1}:0] ${i.label}_dout;")
            write(s"wire ${i.label}_avail;")
            write(s"wire ${i.label}_read;")
            write(s"wire ${i.label}_empty;")
        }
        for (o <- outStreams) {
            val valueType = o.valueType.baseType
            val width = o.valueType.bits
            write(s"wire [${width - 1}:0] ${o.label}_output;")
            write(s"wire [${width - 1}:0] ${o.label}_din;")
            write(s"wire ${o.label}_write;")
            write(s"wire ${o.label}_full;")
            write(s"wire ${o.label}_empty;")
        }
        for (i <- internalStreams) {
            val valueType = i.valueType.baseType
            val width = i.valueType.bits
            write(s"wire [${width - 1}:0] ${i.label}_input;")
            write(s"wire [${width - 1}:0] ${i.label}_output;")
            write(s"wire [${width - 1}:0] ${i.label}_dout;")
            write(s"wire [${width - 1}:0] ${i.label}_din;")
            write(s"wire ${i.label}_avail;")
            write(s"wire ${i.label}_read;")
            write(s"wire ${i.label}_write;")
            write(s"wire ${i.label}_full;")
            write(s"wire ${i.label}_empty;")
        }
        write

        // Instantiate kernels.
        for (kernel <- sp.instances if kernel.device == device) {
            val kernelName = s"kernel_${kernel.name}"
            write(kernelName)
            enter
            if (!kernel.configs.isEmpty) {
                val configs = kernel.configs.map { case (name, value) =>
                    s".$name($value)"
                }.mkString(", ")
                write(s"#($configs)")
            }
            write(s"${kernel.label}(")
            enter
            write(s".clk(clk), .rst(rst)")
            for (i <- kernel.getInputs) {
                val destPort = i.destKernel.inputName(i.destPort)
                write(s",")
                write(s".input_$destPort(${i.label}_input),")
                write(s".avail_$destPort(${i.label}_avail),")
                write(s".read_$destPort(${i.label}_read)")
            }
            for (o <- kernel.getOutputs) {
                val srcPort = o.sourceKernel.outputName(o.sourcePort)
                write(s",")
                write(s".output_$srcPort(${o.label}_output),")
                write(s".write_$srcPort(${o.label}_write),")
                write(s".afull_$srcPort(${o.label}_full)")
            }
            leave
            write(");")
            leave
            for (i <- kernel.getInputs) {
                write(s"assign ${i.label}_avail = !${i.label}_empty;")
            }
            write
        }

        // Connect internal streams.
        for (stream <- internalStreams) {
            val width = stream.valueType.bits
            val addrWidth = getDepthBits

            // Hook up the FIFO.
            val fifo = if (addrWidth == 0) "sp_register" else "sp_fifo"
            write(s"$fifo #(.WIDTH($width), .ADDR_WIDTH($addrWidth))")
            enter
            write(s"fifo_${stream.label}(")
            enter
            write(s".clk(clk), .rst(rst),")
            write(s".din(${stream.label}_din),")
            write(s".dout(${stream.label}_dout),")
            write(s".re(${stream.label}_read),")
            write(s".we(${stream.label}_write),")
            write(s".empty(${stream.label}_empty),")
            write(s".full(${stream.label}_full)")
            leave
            write(s");")
            leave
            write(s"assign ${stream.label}_din = ${stream.label}_output;")
            write(s"assign ${stream.label}_input = ${stream.label}_dout;")
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
                          s" && !${stream.label}_empty;")
                    qmIndex += 1
                }
                if (m.useInputActivity) {
                    write(s"assign amTap[$amIndex] = ${stream.label}_write;")
                    amIndex += 1
                }
                if (m.useOutputActivity) {
                    write(s"assign amTap[$amIndex] = ${stream.label}_read" +
                          s" && !${stream.label}_empty;")
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
                    write(s"assign imAvail[$imIndex] = !${stream.label}_empty;")
                    write(s"assign imRead[$imIndex] = ${stream.label}_read" +
                          s" && !${stream.label}_empty;")
                    imIndex += 1
                }
            }

        }

        // Connect input streams.
        for (stream <- inStreams) {
            val width = stream.valueType.bits
            val addrWidth = getDepthBits
            val srcIndex = stream.index

            // Hook up the FIFO.
            val fifo = if (addrWidth == 0) "sp_register" else "sp_fifo"
            write(s"$fifo #(.WIDTH($width), .ADDR_WIDTH($addrWidth))")
            enter
            write(s"fifo_${stream.label}(")
            enter
            write(s".clk(clk), .rst(rst),")
            write(s".din(I${srcIndex}input),")
            write(s".dout(${stream.label}_dout),")
            write(s".re(${stream.label}_read),")
            write(s".we(I${srcIndex}write),")
            write(s".empty(${stream.label}_empty),")
            write(s".full(I${srcIndex}afull)")
            leave
            write(s");")
            leave
            write(s"assign ${stream.label}_input = ${stream.label}_dout;")
            write

            // Add edge instrumentation.
            for (m <- stream.measures) {
                var qmIndex = m.sourceQueueOffset
                var amIndex = m.sourceActivityOffset
                var imIndex = m.sourceInterOffset
                if (m.useQueueMonitor) {
                    write(s"assign qRst[$qmIndex] = rst;")
                    write(s"assign qWr[$qmIndex] = I${srcIndex}write;")
                    write(s"assign qRd[$qmIndex] = ${stream.label}_read" +
                          s" && !${stream.label}_empty;")
                    qmIndex += 1
                }
                if (m.useInputActivity) {
                    write(s"assign amTap[$amIndex] = I" + srcIndex + "write;")
                    amIndex += 1
                }
                if (m.useOutputActivity) {
                    write(s"assign amTap[$amIndex] = " +
                          s"{stream.label}_read && !${stream.label}_empty;")
                    amIndex += 1
                }
                if (m.useFullActivity) {
                    write(s"assign amTap[$amIndex] = I${srcIndex}full;")
                    amIndex += 1
                }
                if (m.useInterPush) {
                    write(s"assign imAvail[$imIndex] = 1;")
                    write(s"assign imRead[$imIndex] = I${srcIndex}write;")
                    imIndex += 1
                }
                if (m.useInterPop) {
                    write(s"assign imAvail[$imIndex] = !${stream.label}_empty;")
                    write(s"assign imRead[$imIndex] = " +
                          s"${stream.label}_read && !${stream.label}_empty;")
                    imIndex += 1
                }
            }

        }

        // Connect output streams.
        for (stream <- outStreams) {

            val width = stream.valueType.bits
            val addrWidth = getDepthBits
            val destIndex = stream.index

            // Hook up the FIFO.
            val fifo = if (addrWidth == 0) "sp_register" else "sp_fifo"
            write(s"$fifo #(.WIDTH($width), .ADDR_WIDTH($addrWidth))")
            enter
            write(s"fifo_${stream.label}(")
            enter
            write(s".clk(clk), .rst(rst),")
            write(s".din(${stream.label}_din),")
            write(s".dout(O${destIndex}output),")
            write(s".re(O${destIndex}read),")
            write(s".we(${stream.label}_write),")
            write(s".empty(${stream.label}_empty),")
            write(s".full(${stream.label}_full)")
            leave
            write(s");")
            leave
            write(s"assign ${stream.label}_din = ${stream.label}_output;")
            write(s"assign O${destIndex}avail = !${stream.label}_empty;")
            write

            // Add edge instrumentation.
            for (m <- stream.measures) {
                var qmIndex = m.sourceQueueOffset
                var amIndex = m.sourceActivityOffset
                var imIndex = m.sourceInterOffset
                if (m.useQueueMonitor) {
                    write(s"assign qRst[$qmIndex] = rst;")
                    write(s"assign qWr[$qmIndex] = ${stream.label}_write;")
                    write(s"assign qRd[$qmIndex] = O${destIndex}read" +
                          s" && !${stream.label}_empty;")
                    qmIndex += 1
                }
                if (m.useInputActivity) {
                    write(s"assign amTap[$amIndex] = ${stream.label}_write;")
                    amIndex += 1
                }
                if (m.useOutputActivity) {
                    write(s"assign amTap[$amIndex] = O" +
                          s"${destIndex}read && !${stream.label}_empty;")
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
                    write(s"assign imAvail[$imIndex] = !${stream.label}_empty;")
                    write(s"assign imRead[$imIndex] = " +
                          s"O${destIndex}read && !${stream.label}_empty;")
                    imIndex += 1
                }
            }

        }

        leave
        write("endmodule")
        val filename = s"fpga_${device.label}.v"
        writeFile(dir, filename)

    }

}
