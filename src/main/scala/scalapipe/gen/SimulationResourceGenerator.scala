package scalapipe.gen

import scalapipe._
import java.io.File

private[scalapipe] class SimulationResourceGenerator(
        _sp: ScalaPipe,
        _device: Device
    ) extends HDLResourceGenerator(_sp, _device) {

    // Align i to the smallest multiple of 32.
    private def align(i: Int): Int = (i + 31) & ~31;

    // Compute the log, base two, of a number.
    protected def log2(i: Long, a: Int = 0): Int = {
        if (a < 64 && i > (1 << a)) {
            log2(i, a + 1)
        } else {
            a
        }
    }

    override def getRules: String = {

        val kernelTypes = sp.getKernelTypes(device)
        val names = kernelTypes.map(_.name)
        val label = device.label
        val simName = s"sim_${label}.v"
        val topName = s"fpga_${label}.v"
        val wrapName = s"wrap_${label}.v"
        val base = s"$simName scalapipe.v platform.v $topName $wrapName"
        val kstr = names.foldLeft(base) { (s, name) =>
            s"$s $name/$name.v"
        }

        write(s"SIM_${label}_KERNELS =$kstr")
        write(s"TARGETS += hdl_${label}")
        write

        write(s"build_${label}: hdl_${label}")
        write

        write(s"hdl_${label}: $$(SIM_${label}_KERNELS)")
        write(s"\tiverilog -o hdl_${label} $$(SIM_${label}_KERNELS)")
        write

        write(s"sim: $$(TARGETS) compile")
        write(s"\techo finish | ./proc_localhost")
        write

        getOutput
    }

    override def emit(dir: File) {
        super.emit(dir)
        emitWrapFile(dir)
        emitSimFile(dir)
    }

    private def emitWrapFile(dir: File) {

        val inputStreams = sp.streams.filter { s =>
            s.destKernel.device == device && s.sourceKernel.device != device
        }
        val outputStreams = sp.streams.filter { s =>
            s.sourceKernel.device == device && s.destKernel.device != device
        }

        // Wrapper around the ScalaPipe kernels.
        write(s"module wrap$id(")
        enter
        write(s"input wire clk,")
        write(s"input wire rst")
        for (i <- inputStreams) {
            val index = i.index
            val width = i.valueType.bits
            write(s",input wire [${width - 1}:0] din$index")
            write(s",input wire write$index")
            write(s",output wire full$index")
        }
        for (o <- outputStreams) {
            val index = o.index
            val width = o.valueType.bits
            write(s",output wire [${width - 1}:0] dout$index")
            write(s",input wire read$index")
            write(s",output wire avail$index")
        }
        leave
        write(s");")
        enter
        write

        // Input signals.
        for (i <- inputStreams) {
            val index = i.index
            val width = i.valueType.bits
            write(s"wire [${width - 1}:0] data$index;")
            write(s"wire do_write$index = !full$index && write$index;")
            for (b <- 0 until i.valueType.bits / 8) {
                val dest = b * 8
                val src = i.valueType.bits - b * 8 - 8
                write(s"assign data$index[${dest + 7}:$dest] = " +
                      s"din$index[${src + 7}:$src];")
            }
        }
        write

        // Output signals.
        for (o <- outputStreams) {
            val index = o.index
            val width = o.valueType.bits
            write(s"wire [${width - 1}:0] data$index;")
            write(s"wire do_read$index = avail$index && read$index;")
            for (b <- 0 until o.valueType.bits / 8) {
                val dest = b * 8
                val src = o.valueType.bits - b * 8 - 8
                write(s"assign dout$index[${dest + 7}:$dest] = " +
                      s"data$index[${src + 7}:$src];")
            }
        }
        write

        // Instantiate the ScalaPipe kernels.
        write(s"fpga$id sp(.clk(clk), .rst(rst)")
        enter
        for (i <- inputStreams) {
            val index = i.index
            write(s", .I${index}input(data$index), " +
                  s".I${index}write(do_write$index), " +
                  s".I${index}afull(full$index)")
        }
        for (o <- outputStreams) {
            val index = o.index
            write(s", .O${index}output(data$index), " +
                  s".O${index}avail(avail$index), " +
                  s".O${index}read(do_read$index)")
        }
        leave
        write(s");")
        write

        leave
        write(s"endmodule")

        writeFile(dir, s"wrap_${device.label}.v")

    }

    private def emitSimFile(dir: File) {

        val inputStreams = sp.streams.filter { s =>
            s.destKernel.device == device && s.sourceKernel.device != device
        }
        val outputStreams = sp.streams.filter { s =>
            s.sourceKernel.device == device && s.destKernel.device != device
        }

        write(s"module sim_${device.label};")
        enter
        write

        write(s"reg clk;")
        write(s"reg rst;")
        write(s"integer rc;")
        write
        for (s <- inputStreams ++ outputStreams) {
            val fd = s"stream${s.label}"
            write(s"integer $fd;")
        }
        write

        for (s <- inputStreams) {
            val index = s.index
            val width = s.valueType.bits
            write(s"reg [31:0] count$index;")
            write(s"reg [${width - 1}:0] din$index;")
            write(s"wire write$index;")
            write(s"wire full$index;")
            write(s"reg got_data$index;")
            write(s"reg sent_data$index;")
        }
        for (s <- outputStreams) {
            val index = s.index
            val width = s.valueType.bits
            write(s"wire [${width - 1}:0] dout$index;")
            write(s"wire read$index;")
            write(s"wire avail$index;")
        }
        write

        write(s"wrap$id dut(.clk(clk), .rst(rst)")
        enter
        for (s <- inputStreams.map(_.index)) {
            write(s", .din$s(din$s)")
            write(s", .write$s(write$s)")
            write(s", .full$s(full$s)")
        }
        for (s <- outputStreams.map(_.index)) {
            write(s", .dout$s(dout$s)")
            write(s", .read$s(read$s)")
            write(s", .avail$s(avail$s)")
        }
        leave
        write(s");")
        write

        write(s"initial begin")
        enter
        write
        for (s <- inputStreams) {
            val fd = s"stream${s.label}"
            write(s"""$fd = $$fopen(\"$fd\", \"rb\");""")
            write(s"if($fd == 0) begin")
            enter
            write(s"""$$display(\"could not open $fd\");""")
            write(s"$$finish;")
            leave
            write(s"end")
        }
        for (s <- outputStreams) {
            val fd = s"stream${s.label}"
            write(s"""$fd = $$fopen(\"$fd\", \"wb\");""")
            write(s"if($fd == 0) begin")
            enter
            write(s"""$$display(\"could not open $fd\");""")
            write(s"$$finish;")
            leave
            write(s"end")
        }
        write

        if (sp.parameters.get[Boolean]('wave)) {
            write(s"""$$dumpfile(\"${device.label}.vcd\");""")
            write(s"$$dumpvars;")
            write
        }

        write(s"clk <= 0;")
        write(s"rst <= 1;")
        write(s"#10 clk <= 1; #10 clk <= 0;")
        write(s"rst <= 0;")
        write
        write(s"forever begin")
        enter
        write(s"#10 clk <= !clk;")
        leave
        write(s"end")
        leave
        write(s"end")
        write

        for (s <- inputStreams) {
            val index = s.index
            val fd = s"stream${s.label}"
            write(s"always @(posedge clk) begin")
            enter
            write(s"got_data$index <= 0;")
            write(s"sent_data$index <= got_data$index;")
            write(s"if (rst) begin")
            enter
            write(s"count$index <= 0;")
            leave
            write(s"end else if (!full$index && count$index > 0) begin")
            enter
            for (i <- (s.valueType.bits + 7) / 8 until 0 by -1) {
                val top = i * 8 - 1
                val bottom = i * 8 - 8
                write(s"din$index[$top:$bottom] <= $$fgetc($fd);")
            }
            write(s"got_data$index <= 1;")
            write(s"count$index <= count$index - 1;")
            leave
            write(s"end else if (!full$index && count$index == 0) begin")
            enter
            write(s"count$index[7:0] <= $$fgetc($fd);")
            write(s"count$index[15:8] <= $$fgetc($fd);")
            write(s"count$index[23:16] <= $$fgetc($fd);")
            write(s"count$index[31:24] <= $$fgetc($fd);")
            leave
            write(s"end")
            leave
            write(s"end")
            write(s"assign write$index = got_data$index & !sent_data$index;")
            write
        }

        for (s <- outputStreams) {
            val index = s.index
            val fd = s"stream${s.label}"
            val width = s.valueType.bits
            write(s"always @(posedge clk) begin")
            enter
            write(s"if(!rst & avail$index) begin")
            enter
            for (i <- (s.valueType.bits + 7) / 8 until 0 by -1) {
                val top = i * 8 - 1
                val bottom = i * 8 - 8
                write(s"rc <= $$fputc(dout$index[$top:$bottom], $fd);")
            }
            write(s"$$fflush($fd);")
            leave
            write(s"end")
            leave
            write(s"end")
            write(s"assign read$index = !rst & avail$index;")
            write
        }

        leave
        write(s"endmodule")

        writeFile(dir, s"sim_${device.label}.v")

    }

}
