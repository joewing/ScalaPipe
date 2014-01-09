
package autopipe.gen

import autopipe._
import java.io.File

private[autopipe] class SimulationResourceGenerator(
        _ap: AutoPipe,
        _device: Device
    ) extends HDLResourceGenerator(_ap, _device) {

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

        val blocks = ap.blocks.filter(_.device == device)
        val btypes = blocks.map(_.blockType).toSet
        val funcs = ap.functions.filter(_.platform == device.platform)
        val names = funcs.map(_.name) ++ btypes.map(_.name)
        val base = "sim.v fp.v int.v fpga_x.v fpga_wrap.v"
        val bstr = names.foldLeft(base) { (s, name) =>
            s + " " ++ name + "-dir/" + name + ".v"
        }

        write("SIM_BLOCKS =" + bstr)
        write("TARGETS += hdl")
        write

        write("build: hdl")
        write

        write("hdl: $(SIM_BLOCKS)")
        write("\tiverilog -o hdl $(SIM_BLOCKS)")
        write

        write("sim: hdl compile")
        write("\techo finish | ./proc_localhost")
        write("\tgtkwave dump.vcd")
        write

        getOutput
    }

    override def emit(dir: File) {
        super.emit(dir)
        emitWrapFile(dir)
        emitSimFile(dir)
    }

    private def emitWrapFile(dir: File) {

        val inputStreams = ap.streams.filter { s =>
            s.destBlock.device == device && s.sourceBlock.device != device
        }
        val outputStreams = ap.streams.filter { s =>
            s.sourceBlock.device == device && s.destBlock.device != device
        }

        // Write the FIFO module (in this case, just a register).
        write("module ap_fifo(clk, rst, din, dout, re, we, avail, empty, full);")
        enter
        write
        write("parameter WIDTH = 8;")
        write("parameter ADDR_WIDTH = 1;")
        write
        write("input wire clk;")
        write("input wire rst;")
        write("input wire [WIDTH-1:0] din;")
        write("output wire [WIDTH-1:0] dout;")
        write("input wire re;")
        write("input wire we;")
        write("output wire avail;")
        write("output wire empty;")
        write("output wire full;")
        write
        write("reg [WIDTH-1:0] mem;")
        write("reg has_data;")
        write("wire do_read;")
        write("wire do_write;")
        write
        write("assign full = has_data;")
        write("assign avail = has_data;")
        write("assign empty = !has_data;")
        write("assign do_read = full & re;")
        write("assign do_write = empty & we;")
        write
        write("always @(posedge clk) begin")
        enter
        write("if (rst) begin")
        enter
        write("has_data <= 0;")
        leave
        write("end else begin")
        enter
        write("if (do_write) begin")
        enter
        write("mem <= din;")
        write("has_data <= 1;")
        leave
        write("end")
        write("if (do_read) begin")
        enter
        write("has_data <= 0;")
        leave
        write("end")
        leave
        write("end")
        leave
        write("end")
        write
        write("assign dout = mem;")
        write
        leave
        write("endmodule")
        write

        // Wrapper around the ScalaPipe blocks.
        write("module XModule(")
        enter
        write("input wire clk,")
        write("input wire rst")
        for (i <- inputStreams) {
            reset
            set("index", i.index)
            set("width", i.valueType.bits)
            write(",input wire [$width - 1$:0] din$index$")
            write(",input wire write$index$")
            write(",output wire full$index$")
        }
        for (o <- outputStreams) {
            reset
            set("index", o.index)
            set("width", o.valueType.bits)
            write(",output wire [$width - 1$:0] dout$index$")
            write(",input wire read$index$")
            write(",output wire avail$index$")
        }
        leave
        write(");")
        enter
        write

        // Input signals.
        for (i <- inputStreams) {
            reset
            set("index", i.index)
            set("width", i.valueType.bits)
            write("wire [$width - 1$:0] data$index$;")
            write("wire do_write$index$ = !full$index$ && write$index$;")
            for (b <- 0 until i.valueType.bits / 8) {
                set("a", b * 8)
                set("b", i.valueType.bits - b * 8 - 8)
                write("assign data$index$[$a + 7$:$a$] = din$index$[$b + 7$:$b$];")
            }
        }
        write

        // Output signals.
        for (o <- outputStreams) {
            reset
            set("index", o.index)
            set("width", o.valueType.bits)
            write("wire [$width - 1$:0] data$index$;")
            write("wire do_read$index$ = avail$index$ && read$index$;")
            for (b <- 0 until o.valueType.bits / 8) {
                set("a", b * 8)
                set("b", o.valueType.bits - b * 8 - 8)
                write("assign dout$index$[$a + 7$:$a$] = data$index$[$b + 7$:$b$];")
            }
        }
        write

        // Instantiate the ScalaPipe blocks.
        write("fpga0 x(.clk(clk), .rst(rst)")
        enter
        for (i <- inputStreams) {
            reset
            set("index", i.index)
            write(", .I$index$input(data$index$), " +
                    ".I$index$write(do_write$index$), " +
                    ".I$index$afull(full$index$)")
        }
        for (o <- outputStreams) {
            reset
            set("index", o.index)
            write(", .O$index$output(data$index$), " +
                    ".O$index$avail(avail$index$), " +
                    ".O$index$read(do_read$index$)")
        }
        leave
        write(");")
        write

        leave
        write("endmodule")

        writeFile(dir, "fpga_wrap.v")

    }

    private def emitSimFile(dir: File) {

        val inputStreams = ap.streams.filter { s =>
            s.destBlock.device == device && s.sourceBlock.device != device
        }
        val outputStreams = ap.streams.filter { s =>
            s.sourceBlock.device == device && s.destBlock.device != device
        }

        write("module sim;")
        enter
        write

        write("reg clk;")
        write("reg rst;")
        write("integer rc;")
        write
        for (s <- inputStreams ++ outputStreams) {
            reset
            set("fd", "stream" + s.label)
            write("integer $fd$;")
        }
        write

        for (s <- inputStreams) {
            reset
            set("index", s.index)
            set("width", s.valueType.bits)
            write("reg [31:0] count$index$;")
            write("reg [$width - 1$:0] din$index$;")
            write("wire write$index$;")
            write("wire full$index$;")
            write("reg got_data$index$;")
            write("reg sent_data$index$;")
        }
        for (s <- outputStreams) {
            reset
            set("index", s.index)
            set("width", s.valueType.bits)
            write("wire [$width - 1$:0] dout$index$;")
            write("wire read$index$;")
            write("wire avail$index$;")
        }
        write

        write("XModule dut(.clk(clk), .rst(rst)")
        enter
        for (s <- inputStreams) {
            reset
            set("index", s.index)
            write(", .din$index$(din$index$)")
            write(", .write$index$(write$index$)")
            write(", .full$index$(full$index$)")
        }
        for (s <- outputStreams) {
            reset
            set("index", s.index)
            write(", .dout$index$(dout$index$)")
            write(", .read$index$(read$index$)")
            write(", .avail$index$(avail$index$)")
        }
        leave
        write(");")
        write

        write("initial begin")
        enter
        write
        for (s <- inputStreams) {
            reset
            set("fd", "stream" + s.label)
            write("$fd$ = $$fopen(\"$fd$\", \"rb\");")
            write("if($fd$ == 0) begin")
            enter
            write("$$display(\"could not open $fd$\");")
            write("$$finish;")
            leave
            write("end")
        }
        for (s <- outputStreams) {
            reset
            set("fd", "stream" + s.label)
            write("$fd$ = $$fopen(\"$fd$\", \"wb\");")
            write("if($fd$ == 0) begin")
            enter
            write("$$display(\"could not open $fd$\");")
            write("$$finish;")
            leave
            write("end")
        }
        write

        write("$dumpvars;")
        write

        write("clk <= 0;")
        write("rst <= 1;")
        write("#10 clk <= 1; #10 clk <= 0;")
        write("rst <= 0;")
        write
        write("forever begin")
        enter
        write("#10 clk <= !clk;")
        leave
        write("end")
        leave
        write("end")
        write

        for (s <- inputStreams) {
            reset
            set("index", s.index)
            set("fd", "stream" + s.label)
            write("always @(posedge clk) begin")
            enter
            write("got_data$index$ <= 0;")
            write("sent_data$index$ <= got_data$index$;")
            write("if (rst) begin")
            enter
            write("count$index$ <= 0;")
            leave
            write("end else if (!full$index$ && count$index$ > 0) begin")
            enter
            for (i <- (s.valueType.bits + 7) / 8 until 0 by -1) {
                set("i", i * 8)
                write("din$index$[$i - 1$:$i - 8$] <= $$fgetc($fd$);")
            }
            write("got_data$index$ <= 1;")
            write("count$index$ <= count$index$ - 1;")
            leave
            write("end else if (!full$index$ && count$index$ == 0) begin")
            enter
            write("count$index$[7:0] <= $$fgetc($fd$);")
            write("count$index$[15:8] <= $$fgetc($fd$);")
            write("count$index$[23:16] <= $$fgetc($fd$);")
            write("count$index$[31:24] <= $$fgetc($fd$);")
            leave
            write("end")
            leave
            write("end")
            write("assign write$index$ = got_data$index$ & !sent_data$index$;")
            write
        }

        for (s <- outputStreams) {
            reset
            set("index", s.index)
            set("fd", "stream" + s.label)
            set("width", s.valueType.bits)
            write("always @(posedge clk) begin")
            enter
            write("if(!rst & avail$index$) begin")
            enter
            for (i <- (s.valueType.bits + 7) / 8 until 0 by -1) {
                set("i", i * 8)
                write("rc <= $$fputc(dout$index$[$i - 1$:$i - 8$], $fd$);")
            }
            write("$$fflush($fd$);")
            leave
            write("end")
            leave
            write("end")
            write("assign read$index$ = !rst & avail$index$;")
            write
        }

        leave
        write("endmodule")

        writeFile(dir, "sim.v")

    }

}

