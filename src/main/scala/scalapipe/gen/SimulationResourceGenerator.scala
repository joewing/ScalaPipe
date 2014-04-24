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
        write(s"input wire rst,")
        write(s"output wire running")
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
        write(s"fpga$id sp(.clk(clk), .rst(rst), .running(running)")
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

        write(s"module sim_fifo(")
        enter
        write(s"clk, rst, din, dout, re, we, avail, full);")

        write(s"parameter WIDTH = 8;")
        write(s"parameter ADDR_WIDTH = 8;")
        write(s"input wire clk;")
        write(s"input wire rst;")
        write(s"input wire [WIDTH-1:0] din;")
        write(s"output wire [WIDTH-1:0] dout;")
        write(s"input wire re;")
        write(s"input wire we;")
        write(s"output wire avail;")
        write(s"output wire full;")
        write(s"reg [WIDTH-1:0] mem [0:(1 << ADDR_WIDTH) - 1];")
        write(s"reg [ADDR_WIDTH-1:0] read_ptr;")
        write(s"reg [ADDR_WIDTH-1:0] write_ptr;")
        write(s"reg [ADDR_WIDTH:0] count;")
        write(s"assign full = count[ADDR_WIDTH];")
        write(s"assign avail = count != 0;")
        write(s"wire do_read = re & avail;")
        write(s"wire do_write = we & !full;")
        write(s"always @(posedge clk) begin")
        enter
        write(s"if (rst) begin")
        enter
        write(s"read_ptr <= 0;")
        write(s"write_ptr <= 0;")
        write(s"count <= 0;")
        leave
        write(s"end else begin")
        enter
        write(s"if (do_write) begin")
        enter
        write(s"mem[write_ptr] <= din;")
        write(s"write_ptr <= write_ptr + 1;")
        leave
        write(s"end")
        write(s"if (do_read) begin")
        enter
        write(s"read_ptr <= read_ptr + 1;")
        leave
        write(s"end")
        write(s"case ({do_read, do_write})")
        enter
        write(s"2'b10: count <= count - 1;")
        write(s"2'b01: count <= count + 1;")
        write(s"default: count <= count;")
        leave
        write(s"endcase")
        leave
        write(s"end")
        leave
        write(s"end")
        write(s"assign dout = mem[read_ptr];")
        leave
        write(s"endmodule")

        write(s"module sim_${device.label};")
        enter
        write

        val inputCount = inputStreams.size
        write(s"reg clk;")
        write(s"reg rst;")
        write(s"wire running;")
        write(s"reg stopped = 0;")
        write(s"integer cycles = 0;")
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
            write(s"reg active$index;")
            write(s"reg signed [31:0] count$index;")
            write(s"wire [${width - 1}:0] din$index;")
            write(s"wire write$index;")
            write(s"wire full$index;")
        }
        for (s <- outputStreams) {
            val index = s.index
            val width = s.valueType.bits
            write(s"wire [${width - 1}:0] dout$index;")
            write(s"wire read$index;")
            write(s"wire avail$index;")
        }
        write

        write(s"wrap$id dut(.clk(clk), .rst(rst), .running(running)")
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

        val activeValues = inputStreams.map {
            s => s"active${s.index}"
        }.toSeq :+ "running"
        val activeCheck = activeValues.mkString(" | ")

        write("always @(posedge clk) begin")
        enter
        write(s"if (running) begin")
        enter
        write("cycles <= cycles + 1;")
        write("if ((cycles % 1000) == 0) begin")
        enter
        write("""$display("Cycles: %d", cycles);""")
        leave
        write("end")
        leave
        write(s"end else if (!($activeCheck) & !stopped) begin")
        enter
        write("""$display("Cycles: %d", cycles);""")
        for (s <- outputStreams) {
            val fd = s"stream${s.label}"
            write(s"rc <= $$fputc(0, $fd);")
            write(s"$$fflush($fd);")
        }
        write("stopped <= 1;")
        leave
        write("end")
        leave
        write("end")

        if (!inputStreams.isEmpty) {
            write(s"wire can_read;")
        }
        for (s <- inputStreams) {
            val index = s.index
            val fd = s"stream${s.label}"
            val width = s.valueType.bits
            write(s"reg [${width - 1}:0] fifo_in$index;")
            write(s"wire fifo_avail$index;")
            write(s"wire fifo_full$index;")
            write(s"reg fifo_we$index;")
            write(s"sim_fifo #(.WIDTH($width))")
            enter
            write(s"input${index}_fifo(")
            enter
            write(s".clk(clk),")
            write(s".rst(rst),")
            write(s".din(fifo_in$index),")
            write(s".dout(din$index),")
            write(s".re(write$index),")
            write(s".we(fifo_we$index),")
            write(s".avail(fifo_avail$index),")
            write(s".full(fifo_full$index)")
            leave
            write(s");")
            leave
            write(s"assign write$index = fifo_avail$index & !full$index;")

            write(s"always @(posedge clk) begin")
            enter
            write(s"fifo_we$index <= 0;")
            write(s"if (rst) begin")
            enter
            write(s"count$index <= 0;")
            write(s"active$index <= 1;")
            leave
            write(s"end else if (!fifo_full$index && count$index > 0) begin")
            enter
            for (i <- (width + 7) / 8 until 0 by -1) {
                val top = i * 8 - 1
                val bottom = i * 8 - 8
                write(s"fifo_in$index[$top:$bottom] <= $$fgetc($fd);")
            }
            write(s"fifo_we$index <= 1;")
            write(s"count$index <= count$index - 1;")
            leave
            write(s"end else if (count$index < 0) begin")
            enter
            write(s"active$index <= fifo_avail$index;")
            leave
            write(s"end else if (can_read && active$index) begin")
            enter
            write(s"count$index[7:0] <= $$fgetc($fd);")
            write(s"count$index[15:8] <= $$fgetc($fd);")
            write(s"count$index[23:16] <= $$fgetc($fd);")
            write(s"count$index[31:24] <= $$fgetc($fd);")
            leave
            write(s"end")
            leave
            write(s"end")
            write
        }
        if (!inputStreams.isEmpty) {
            val canReadStr = inputStreams.map { s =>
                s"count${s.index} <= 0"
            }.mkString("&")
            write(s"assign can_read = $canReadStr;")
        }

        for (s <- outputStreams) {
            val index = s.index
            val fd = s"stream${s.label}"
            val width = s.valueType.bits
            write(s"always @(posedge clk) begin")
            enter
            write(s"if(!rst & avail$index) begin")
            enter
            write(s"rc <= $$fputc(1, $fd);")
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
