
package autopipe.gen

import autopipe._
import java.io.File

private[autopipe] abstract class HDLResourceGenerator(val ap: AutoPipe,
                                                                        val device: Device)
    extends ResourceGenerator {

    protected val host = device.host
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

    private def emitInternal(dir: File) {

        val streams = ap.streams.filter { s =>
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

        write("module fpga" + id + "(")
        enter
        write("input wire clk,")
        write("input wire rst")
        for (i <- inStreams) {
            val index = i.index
            val width = i.valueType.bits
            write(",")
            write("input wire [" + (width - 1) + ":0] I" + index + "input,")
            write("input wire I" + index + "write,")
            write("output wire I" + index + "afull")
        }
        for (o <- outStreams) {
            val index = o.index
            val width = o.valueType.bits
            write(",")
            write("output wire [" + (width - 1) + ":0] O" + index + "output,")
            write("output wire O" + index + "avail,")
            write("input wire O" + index + "read")
        }
        if (tt.amCount > 0) {
            write(",")
            write("output wire [" + (tt.amCount - 1) + ":0] amTap")
        }
        if (tt.lmCount > 0) {
            write(",")
            write("output wire [" + (tt.lmCount - 1) + ":0] lmTap")
        }
        if (tt.qmCount > 0) {
            write(",")
            write("output wire [" + (tt.qmCount - 1) + ":0] qRst,")
            write("output wire [" + (tt.qmCount - 1) + ":0] qWr,")
            write("output wire [" + (tt.qmCount - 1) + ":0] qRd")
        }
        if (tt.imCount > 0) {
            write(",")
            write("output wire [" + (tt.imCount - 1) + ":0] imAvail,")
            write("output wire [" + (tt.imCount - 1) + ":0] imRead")
        }
        leave
        write(");")
        enter
        write

        // Wires
        for (i <- inStreams) {
            val valueType = i.valueType.baseType
            val width = i.valueType.bits
            write("wire [" + (width - 1) + ":0] " + i.label + "_input;")
            write("wire [" + (width - 1) + ":0] " + i.label + "_dout;")
            write("wire " + i.label + "_avail;")
            write("wire " + i.label + "_read;")
            write("wire " + i.label + "_empty;")
        }
        for (o <- outStreams) {
            val valueType = o.valueType.baseType
            val width = o.valueType.bits
            write("wire [" + (width - 1) + ":0] " + o.label + "_output;")
            write("wire [" + (width - 1) + ":0] " + o.label + "_din;")
            write("wire " + o.label + "_write;")
            write("wire " + o.label + "_full;")
            write("wire " + o.label + "_empty;")
        }
        for (i <- internalStreams) {
            val valueType = i.valueType.baseType
            val width = i.valueType.bits
            write("wire [" + (width - 1) + ":0] " + i.label + "_input;")
            write("wire [" + (width - 1) + ":0] " + i.label + "_output;")
            write("wire [" + (width - 1) + ":0] " + i.label + "_dout;")
            write("wire [" + (width - 1) + ":0] " + i.label + "_din;")
            write("wire " + i.label + "_avail;")
            write("wire " + i.label + "_read;")
            write("wire " + i.label + "_write;")
            write("wire " + i.label + "_full;")
            write("wire " + i.label + "_empty;")
        }
        write

        // Instantiate kernels.
        for (kernel <- ap.kernels if kernel.device == device) {
            val kernelName = "kernel_" + kernel.name
            write(kernelName)
            enter
            val configs = kernel.getConfigs
            if (!configs.isEmpty) {
                write("#(" + configs.foldLeft("")({ (a, c) =>
                    (if (a.isEmpty) "" else ", ") +
                    "." + c._1 + "(" + c._2 + ")"
                }) + ")")
            }
            write(kernel.label + "(")
            enter
            write(".clk(clk), .rst(rst)")
            for (i <- kernel.getInputs) {
                val destPort = i.destKernel.inputName(i.destPort)
                write(",")
                write(".input_" + destPort + "(" + i.label + "_input),")
                write(".avail_" + destPort + "(" + i.label + "_avail),")
                write(".read_"  + destPort + "(" + i.label + "_read)")
            }
            for (o <- kernel.getOutputs) {
                val srcPort = o.sourceKernel.outputName(o.sourcePort)
                write(",")
                write(".output_" + srcPort + "(" + o.label + "_output),")
                write(".write_"  + srcPort + "(" + o.label + "_write),")
                write(".afull_"  + srcPort + "(" + o.label + "_full)")
            }
            leave
            write(");")
            leave
            for (i <- kernel.getInputs) {
                write("assign " + i.label + "_avail = " +
                        "!" + i.label + "_empty;")
            }
            write
        }

        // Connect internal streams.
        for (stream <- internalStreams) {
            val width = stream.valueType.bits
            val addrWidth = stream.getDepthBits

            // Hook up the FIFO.
            write("ap_fifo #(.WIDTH(" + width + "), " +
                    ".ADDR_WIDTH(" + addrWidth + "))")
            enter
            write("fifo_" + stream.label + "(")
            enter
            write(".clk(clk), .rst(rst),")
            write(".din("    + stream.label + "_din),")
            write(".dout("  + stream.label + "_dout),")
            write(".re("     + stream.label + "_read),")
            write(".we("     + stream.label + "_write),")
            write(".empty(" + stream.label + "_empty),")
            write(".full("  + stream.label + "_full)")
            leave
            write(");")
            leave
            write("assign " + stream.label + "_din = " +
                    stream.label + "_output;")
            write("assign " + stream.label + "_input = " +
                    stream.label + "_dout;")
            write

            // Add edge instrumentation.
            for (m <- stream.measures) {
                var qmIndex = m.sourceQueueOffset
                var amIndex = m.sourceActivityOffset
                var imIndex = m.sourceInterOffset
                if (m.useQueueMonitor) {
                    write("assign qRst[" + qmIndex + "] = rst;")
                    write("assign qWr[" + qmIndex + "] = " +
                            stream.label + "_write;")
                    write("assign qRd[" + qmIndex + "] = " +
                            stream.label + "_read " +
                            " && !" + stream.label + "_empty;")
                    qmIndex += 1
                }
                if (m.useInputActivity) {
                    write("assign amTap[" + amIndex + "] = " +
                            stream.label + "_write;")
                    amIndex += 1
                }
                if (m.useOutputActivity) {
                    write("assign amTap[" + amIndex + "] = " +
                            stream.label + "_read " +
                            "&& !" + stream.label + "_empty;")
                    amIndex += 1
                }
                if (m.useFullActivity) {
                    write("assign amTap[" + amIndex + "] = " +
                            stream.label + "_full;")
                    amIndex += 1
                }
                if (m.useInterPush) {
                    write("assign imAvail[" + imIndex + "] = 1;")
                    write("assign imRead[" + imIndex + "] = " +
                            stream.label + "_write;")
                    imIndex += 1
                }
                if (m.useInterPop) {
                    write("assign imAvail[" + imIndex + "] = !" +
                            stream.label + "_empty;")
                    write("assign imRead[" + imIndex + "] = " +
                            stream.label + "_read " +
                            "&& !" + stream.label + "_empty;")
                    imIndex += 1
                }
            }

        }

        // Connect input streams.
        for (stream <- inStreams) {
            val width = stream.valueType.bits
            val addrWidth = stream.getDepthBits
            val srcIndex = stream.index

            // Hook up the FIFO.
            write("ap_fifo #(.WIDTH(" + width + "), " +
                    ".ADDR_WIDTH(" + addrWidth + "))")
            enter
            write("fifo_" + stream.label + "(")
            enter
            write(".clk(clk), .rst(rst),")
            write(".din(I"    + srcIndex + "input),")
            write(".dout("  + stream.label + "_dout),")
            write(".re("     + stream.label + "_read),")
            write(".we(I"     + srcIndex + "write),")
            write(".empty(" + stream.label + "_empty),")
            write(".full(I"  + srcIndex + "afull)")
            leave
            write(");")
            leave
            write("assign " + stream.label + "_input = " +
                    stream.label + "_dout;")
            write

            // Add edge instrumentation.
            for (m <- stream.measures) {
                var qmIndex = m.sourceQueueOffset
                var amIndex = m.sourceActivityOffset
                var imIndex = m.sourceInterOffset
                if (m.useQueueMonitor) {
                    write("assign qRst[" + qmIndex + "] = rst;")
                    write("assign qWr[" + qmIndex + "] = I" + srcIndex + "write;")
                    write("assign qRd[" + qmIndex + "] = " +
                            stream.label + "_read " +
                            "&& !" + stream.label + "_empty;")
                    qmIndex += 1
                }
                if (m.useInputActivity) {
                    write("assign amTap[" + amIndex + "] = I" + srcIndex + "write;")
                    amIndex += 1
                }
                if (m.useOutputActivity) {
                    write("assign amTap[" + amIndex + "] = " +
                            stream.label + "_read " +
                            "&& !" + stream.label + "_empty;")
                    amIndex += 1
                }
                if (m.useFullActivity) {
                    write("assign amTap[" + amIndex + "] = I" + srcIndex + "full;")
                    amIndex += 1
                }
                if (m.useInterPush) {
                    write("assign imAvail[" + imIndex + "] = 1;")
                    write("assign imRead[" + imIndex + "] = I" +
                            srcIndex + "write;")
                    imIndex += 1
                }
                if (m.useInterPop) {
                    write("assign imAvail[" + imIndex + "] = !" +
                            stream.label + "_empty;")
                    write("assign imRead[" + imIndex + "] = " +
                            stream.label + "_read " +
                            "&& !" + stream.label + "_empty;")
                    imIndex += 1
                }
            }

        }

        // Connect output streams.
        for (stream <- outStreams) {

            val width = stream.valueType.bits
            val addrWidth = stream.getDepthBits
            val destIndex = stream.index

            // Hook up the FIFO.
            write("ap_fifo #(.WIDTH(" + width + "), " +
                    ".ADDR_WIDTH(" + addrWidth + "))")
            enter
            write("fifo_" + stream.label + "(")
            enter
            write(".clk(clk), .rst(rst),")
            write(".din("    + stream.label + "_din),")
            write(".dout(O"  + destIndex + "output),")
            write(".re(O"     + destIndex + "read),")
            write(".we("     + stream.label + "_write),")
            write(".empty(" + stream.label + "_empty),")
            write(".full("  + stream.label + "_full)")
            leave
            write(");")
            leave
            write("assign " + stream.label + "_din = " +
                    stream.label + "_output;")
            write("assign O" + destIndex + "avail = !" + stream.label + "_empty;")
            write

            // Add edge instrumentation.
            for (m <- stream.measures) {
                var qmIndex = m.sourceQueueOffset
                var amIndex = m.sourceActivityOffset
                var imIndex = m.sourceInterOffset
                if (m.useQueueMonitor) {
                    write("assign qRst[" + qmIndex + "] = rst;")
                    write("assign qWr[" + qmIndex + "] = " +
                            stream.label + "_write;")
                    write("assign qRd[" + qmIndex + "] = O" + destIndex + "read " +
                            "&& !" + stream.label + "_empty;")
                    qmIndex += 1
                }
                if (m.useInputActivity) {
                    write("assign amTap[" + amIndex + "] = " +
                            stream.label + "_write;")
                    amIndex += 1
                }
                if (m.useOutputActivity) {
                    write("assign amTap[" + amIndex + "] = O" +
                            destIndex + "read " +
                            "&& !" + stream.label + "_empty;")
                    amIndex += 1
                }
                if (m.useFullActivity) {
                    write("assign amTap[" + amIndex + "] = " +
                            stream.label + "_full;")
                    amIndex += 1
                }
                if (m.useInterPush) {
                    write("assign imAvail[" + imIndex + "] = 1;")
                    write("assign imRead[" + imIndex + "] = " +
                            stream.label + "_write;")
                    imIndex += 1
                }
                if (m.useInterPop) {
                    write("assign imAvail[" + imIndex + "] = not " +
                            stream.label + "_empty;")
                    write("assign imRead[" + imIndex + "] = O" +
                            destIndex + "read " +
                            "&& !" + stream.label + "_empty;")
                    imIndex += 1
                }
            }

        }

        leave
        write("endmodule")
        writeFile(dir, "fpga_x.v") // FIXME: name needs to be id/host unique

    }

}

