package autopipe.gen

import autopipe._
import java.io.File

private[autopipe] class SmartFusionResourceGenerator(
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

    override def getRules: String = ""

    override def emit(dir: File) {
        super.emit(dir)
        emitWrapFile(dir)
        emitTopFile(dir)
    }

    private def emitWrapFile(dir: File) {

        val inputStreams = ap.streams.filter { s =>
            s.destKernel.device == device && s.sourceKernel.device != device
        }
        val outputStreams = ap.streams.filter { s =>
            s.sourceKernel.device == device && s.destKernel.device != device
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

        // Wrapper around the ScalaPipe kernels.
        write("module XModule(")
        enter
        write("input wire clk,")
        write("input wire rst")
        for (i <- inputStreams) {
            reset
            set("index", i.index)
            write(",input wire [31:0] din$index$")
            write(",input wire write$index$")
            write(",output reg [31:0] count$index$")
        }
        for (o <- outputStreams) {
            reset
            set("index", o.index)
            write(",output wire [31:0] dout$index$")
            write(",input wire read$index$")
            write(",output reg [31:0] count$index$")
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
            write("wire full$index$;")
            write("wire do_write$index$ = !full$index$ && count$index$ == 0;")
        }
        write

        // Output signals.
        for (o <- outputStreams) {
            reset
            set("index", o.index)
            set("width", o.valueType.bits)
            write("wire [$width - 1$:0] data$index$;")
            write("wire avail$index$;")
            write("wire do_read$index$ = avail$index$ && count$index$ == 0;")
        }
        write

        // Instantiate the ScalaPipe kernels.
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

        // Input decoders.
        for (i <- inputStreams) {

            val width = align(i.valueType.bits)
            val cmax = width / 32
            val cwidth = log2(cmax + 1)

            reset
            set("index", i.index)
            set("width", width)
            set("cmax", cmax)
            set("cwidth", cwidth)

            write("reg [$cwidth - 1$:0] next_count$index$;")
            write("reg [$width - 1$:0] buffer$index$;")
            write("always @(*) begin")
            enter
            write("if (do_write$index$) begin")
            enter
            write("next_count$index$ <= $cmax$;")
            leave
            write("end else if (write$index$) begin")
            enter
            write("next_count$index$ <= count$index$ - 1;")
            leave
            write("end else begin")
            enter
            write("next_count$index$ <= count$index$;")
            leave
            write("end")
            leave
            write("end")
            write("always @(posedge clk) begin")
            enter
            write("if (rst) begin")
            enter
            write("count$index$ <= $cmax$;")
            leave
            write("end else begin")
            enter
            write("if (write$index$) begin")
            enter
            write("buffer$index$[31:0] <= din$index$;")
            if (width > 32) {
                write("buffer$index$[$width - 1$:32] <= " +
                        "buffer$index$[$width - 32 - 1$:0];")
            }
            leave
            write("end")
            write("count$index$ <= next_count$index$;")
            leave
            write("end")
            leave
            write("end")
            write
            write("assign data$index$ = buffer$index$;")
            write
        }

        // Output encoders.
        for (o <- outputStreams) {
            val width = align(o.valueType.bits)
            val cmax = width / 32
            val cwidth = log2(cmax + 1)

            reset
            set("index", o.index)
            set("width", width)
            set("cmax", cmax)
            set("cwidth", log2(cmax + 1))

            write("reg [$cwidth - 1$:0] next_count$index$;")
            write("reg [$width - 1$:0] buffer$index$;")
            write("always @(*) begin")
            enter
            write("if (do_read$index$) begin")
            enter
            write("next_count$index$ <= $cmax$;")
            leave
            write("end else if (read$index$) begin")
            enter
            write("next_count$index$ <= count$index$ - 1;")
            leave
            write("end else begin")
            enter
            write("next_count$index$ <= count$index$;")
            leave
            write("end")
            leave
            write("end")
            write("always @(posedge clk) begin")
            enter
            write("if (rst) begin")
            enter
            write("count$index$ <= 0;")
            leave
            write("end else begin")
            enter
            write("if (do_read$index$) begin")
            enter
            write("buffer$index$ <= data$index$;")
            leave
            if (width > 32) {
                write("end else if (read$index$) begin")
                enter
                write("buffer$index$[$width - 1$:32] <= " +
                        "buffer$index$[$width - 32 - 1$:0];")
                leave
            }
            write("end")
            write("count$index$ <= next_count$index$;")
            leave
            write("end")
            leave
            write("end")
            write
            write("assign dout$index$ = " +
                    "buffer$index$[$width - 1$:$width - 32$];")
            write
        }

        leave
        write("endmodule")

        writeFile(dir, "fpga_wrap.v")

    }

    private def emitTopFile(dir: File) {

        val inputStreams = ap.streams.filter { s =>
            s.destKernel.device == device && s.sourceKernel.device != device
        }
        val outputStreams = ap.streams.filter { s =>
            s.sourceKernel.device == device && s.destKernel.device != device
        }

        write("module sp_interface(")
        enter
        write("input wire PRESETn,")
        write("input wire PCLK,")
        write("input wire PSELx,")
        write("input wire PENABLE,")
        write("input wire PWRITE,")
        write("input wire [4:0] PADDR,")
        write("input wire [31:0] PWDATA,")
        write("output reg [31:0] PRDATA,")
        write("output wire PREADY,")
        write("output wire PSLVERR")
        leave
        write(");")
        enter
        write

        write("localparam COMMAND_ADDR    = 0;")
        write("localparam PORT_ADDR        = 4;")
        write("localparam COUNT_ADDR      = 8;")
        write("localparam DATA_ADDR        = 12;")

        write("assign PREADY = 1;")
        write("assign PSLVERR = 0;")
        write

        // Control wires.
        write("wire do_read = PSELx & PENABLE & !PWRITE;")
        write("wire do_write = PSELx & PENABLE & PWRITE;")
        write("wire write_command = do_write && PADDR == COMMAND_ADDR;")
        write("wire write_port = do_write && PADDR == PORT_ADDR;")
        write("wire read_count = do_read && PADDR == COUNT_ADDR;")
        write("wire write_data = do_write && PADDR == DATA_ADDR;")
        write("wire read_data = do_read && PADDR == DATA_ADDR;")
        write

        // Handle resets and clock.
        write("reg x_reset;")
        write("reg running;")
        write("reg was_running;")
        write("wire clk = running & PCLK;")
        write("always @(posedge PCLK or negedge PRESETn) begin")
        enter
        write("if (!PRESETn) begin")
        enter
        write("x_reset <= 1;")
        write("running <= 0;")
        write("was_running <= 0;")
        leave
        write("end else if (write_command) begin")
        enter
        write("x_reset <= 1;")
        write("running <= PWDATA == 1;")
        write("was_running <= 0;")
        leave
        write("end else begin")
        enter
        write("x_reset <= !was_running;")
        write("was_running <= running;")
        leave
        write("end")
        leave
        write("end")

        // Determine the port ID.
        write("reg [31:0] port_id;")
        write("always @(posedge clk) begin")
        enter
        write("if (write_port) begin")
        enter
        write("port_id <= PWDATA;")
        leave
        write("end")
        leave
        write("end")
        write

        // Create the write signals.
        for (i <- inputStreams) {
            reset
            set("index", i.index)
            write("wire write$index$ = write_data && port_id == $index$;")
        }
        write

        // Create the read signals.
        for (o <- outputStreams) {
            reset
            set("index", o.index)
            write("wire read$index$ = read_data && port_id == $index$;")
            write("wire [31:0] dout$index$;")
        }
        write

        // Create the count wires.
        for (s <- inputStreams ++ outputStreams) {
            reset
            set("index", s.index)
            write("wire [31:0] count$index$;")
        }
        write

        // Instantiate the module.
        write("XModule X(.clk(clk), .rst(x_reset)")
        enter
        for (i <- inputStreams) {
            reset
            set("index", i.index)
            write(", .din$index$(PWDATA), " +
                    ".write$index$(write$index$), " +
                    ".count$index$(count$index$)")
        }
        for (o <- outputStreams) {
            reset
            set("index", o.index)
            write(", .dout$index$(dout$index$), " +
                    ".read$index$(read$index$), " +
                    ".count$index$(count$index$)")
        }
        leave
        write(");")
        write

        // Drive PRDATA.
        write("always @(posedge clk) begin")
        enter
        write("if (PSELx & !PENABLE & !PWRITE) begin")
        enter
        write("if (PADDR == COUNT_ADDR) begin")
        enter
        write("case (port_id)")
        enter
        for (s <- inputStreams ++ outputStreams) {
            reset
            set("index", s.index)
            write("$index$: PRDATA <= count$index$;")
        }
        leave
        write("endcase")
        leave
        write("end else begin")
        enter
        write("case (port_id)")
        enter
        for (o <- outputStreams) {
            reset
            set("index", o.index)
            write("$index$: PRDATA <= dout$index$;")
        }
        leave
        write("endcase")
        leave
        write("end")
        leave
        write("end")
        leave
        write("end")
        write

        leave
        write("endmodule")

        writeFile(dir, "sp_interface.v")

    }

}
