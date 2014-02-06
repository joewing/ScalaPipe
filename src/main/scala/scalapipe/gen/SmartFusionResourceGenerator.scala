package scalapipe.gen

import scalapipe._
import java.io.File

private[scalapipe] class SmartFusionResourceGenerator(
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

    override def getRules: String = ""

    override def emit(dir: File) {
        super.emit(dir)
        emitWrapFile(dir)
        emitTopFile(dir)
    }

    private def emitWrapFile(dir: File) {

        val inputStreams = sp.streams.filter { s =>
            s.destKernel.device == device && s.sourceKernel.device != device
        }
        val outputStreams = sp.streams.filter { s =>
            s.sourceKernel.device == device && s.destKernel.device != device
        }

        // Wrapper around the ScalaPipe kernels.
        write(s"module XModule(")
        enter
        write(s"input wire clk,")
        write(s"input wire rst")
        for (i <- inputStreams) {
            write(s",input wire [31:0] din${i.index}")
            write(s",input wire write${i.index}")
            write(s",output reg [31:0] count${i.index}")
        }
        for (o <- outputStreams) {
            write(s",output wire [31:0] dout${o.index}")
            write(s",input wire read${o.index}")
            write(s",output reg [31:0] count${o.index}")
        }
        leave
        write(s");")
        enter
        write

        // Input signals.
        for (i <- inputStreams) {
            val width = i.valueType.bits
            write(s"wire [${width - 1}:0] data${i.index};")
            write(s"wire full${i.index};")
            write(s"wire do_write${i.index} = !full${i.index}" +
                  s" && count${i.index} == 0;")
        }
        write

        // Output signals.
        for (o <- outputStreams) {
            val width = o.valueType.bits
            write(s"wire [${width - 1}:0] data${o.index};")
            write(s"wire avail${o.index};")
            write(s"wire do_read${o.index} = avail${o.index}" +
                  s" && count${o.index} == 0;")
        }
        write

        // Instantiate the ScalaPipe kernels.
        write(s"fpga0 x(.clk(clk), .rst(rst)")
        enter
        for (i <- inputStreams) {
            write(s", .I${i.index}input(data${i.index}), " +
                  s".I${i.index}write(do_write${i.index}), " +
                  s".I${i.index}afull(full${i.index})")
        }
        for (o <- outputStreams) {
            write(s", .O${o.index}output(data${o.index}), " +
                  s".O${o.index}avail(avail${o.index}), " +
                  s".O${o.index}read(do_read${o.index})")
        }
        leave
        write(s");")
        write

        // Input decoders.
        for (i <- inputStreams) {

            val width = align(i.valueType.bits)
            val cmax = width / 32
            val cwidth = log2(cmax + 1)

            write(s"reg [${cwidth - 1}:0] next_count${i.index};")
            write(s"reg [${width - 1}:0] buffer${i.index};")
            write(s"always @(*) begin")
            enter
            write(s"if (do_write${i.index}) begin")
            enter
            write(s"next_count${i.index} <= $cmax;")
            leave
            write(s"end else if (write${i.index}) begin")
            enter
            write(s"next_count${i.index} <= count${i.index} - 1;")
            leave
            write(s"end else begin")
            enter
            write(s"next_count${i.index} <= count${i.index};")
            leave
            write(s"end")
            leave
            write(s"end")
            write(s"always @(posedge clk) begin")
            enter
            write(s"if (rst) begin")
            enter
            write(s"count${i.index} <= $cmax;")
            leave
            write(s"end else begin")
            enter
            write(s"if (write${i.index}) begin")
            enter
            write(s"buffer${i.index}[31:0] <= din${i.index};")
            if (width > 32) {
                write(s"buffer${i.index}[${width - 1}:32] <= " +
                      s"buffer${i.index}[${width - 32 - 1}:0];")
            }
            leave
            write(s"end")
            write(s"count${i.index} <= next_count${i.index};")
            leave
            write(s"end")
            leave
            write(s"end")
            write
            write(s"assign data${i.index} = buffer${i.index};")
            write
        }

        // Output encoders.
        for (o <- outputStreams) {
            val width = align(o.valueType.bits)
            val cmax = width / 32
            val cwidth = log2(cmax + 1)

            write(s"reg [${cwidth - 1}:0] next_count${o.index};")
            write(s"reg [${width - 1}:0] buffer${o.index};")
            write(s"always @(*) begin")
            enter
            write(s"if (do_read${o.index}) begin")
            enter
            write(s"next_count${o.index} <= $cmax;")
            leave
            write(s"end else if (read${o.index}) begin")
            enter
            write(s"next_count${o.index} <= count${o.index} - 1;")
            leave
            write(s"end else begin")
            enter
            write(s"next_count${o.index} <= count${o.index};")
            leave
            write(s"end")
            leave
            write(s"end")
            write(s"always @(posedge clk) begin")
            enter
            write(s"if (rst) begin")
            enter
            write(s"count${o.index} <= 0;")
            leave
            write(s"end else begin")
            enter
            write(s"if (do_read${o.index}) begin")
            enter
            write(s"buffer${o.index} <= data${o.index};")
            leave
            if (width > 32) {
                write(s"end else if (read${o.index}) begin")
                enter
                write(s"buffer${o.index}[${width - 1}:32] <= " +
                        "buffer${o.index}[${width - 32 - 1}:0];")
                leave
            }
            write(s"end")
            write(s"count${o.index} <= next_count${o.index};")
            leave
            write(s"end")
            leave
            write(s"end")
            write
            write(s"assign dout${o.index} = " +
                  s"buffer${o.index}[${width - 1}:${width - 32}];")
            write
        }

        leave
        write(s"endmodule")

        writeFile(dir, "fpga_wrap.v")

    }

    private def emitTopFile(dir: File) {

        val inputStreams = sp.streams.filter { s =>
            s.destKernel.device == device && s.sourceKernel.device != device
        }
        val outputStreams = sp.streams.filter { s =>
            s.sourceKernel.device == device && s.destKernel.device != device
        }

        write(s"module sp_interface(")
        enter
        write(s"input wire PRESETn,")
        write(s"input wire PCLK,")
        write(s"input wire PSELx,")
        write(s"input wire PENABLE,")
        write(s"input wire PWRITE,")
        write(s"input wire [4:0] PADDR,")
        write(s"input wire [31:0] PWDATA,")
        write(s"output reg [31:0] PRDATA,")
        write(s"output wire PREADY,")
        write(s"output wire PSLVERR")
        leave
        write(s");")
        enter
        write

        write(s"localparam COMMAND_ADDR    = 0;")
        write(s"localparam PORT_ADDR        = 4;")
        write(s"localparam COUNT_ADDR      = 8;")
        write(s"localparam DATA_ADDR        = 12;")

        write(s"assign PREADY = 1;")
        write(s"assign PSLVERR = 0;")
        write

        // Control wires.
        write(s"wire do_read = PSELx & PENABLE & !PWRITE;")
        write(s"wire do_write = PSELx & PENABLE & PWRITE;")
        write(s"wire write_command = do_write && PADDR == COMMAND_ADDR;")
        write(s"wire write_port = do_write && PADDR == PORT_ADDR;")
        write(s"wire read_count = do_read && PADDR == COUNT_ADDR;")
        write(s"wire write_data = do_write && PADDR == DATA_ADDR;")
        write(s"wire read_data = do_read && PADDR == DATA_ADDR;")
        write

        // Handle resets and clock.
        write(s"reg x_reset;")
        write(s"reg running;")
        write(s"reg was_running;")
        write(s"wire clk = running & PCLK;")
        write(s"always @(posedge PCLK or negedge PRESETn) begin")
        enter
        write(s"if (!PRESETn) begin")
        enter
        write(s"x_reset <= 1;")
        write(s"running <= 0;")
        write(s"was_running <= 0;")
        leave
        write(s"end else if (write_command) begin")
        enter
        write(s"x_reset <= 1;")
        write(s"running <= PWDATA == 1;")
        write(s"was_running <= 0;")
        leave
        write(s"end else begin")
        enter
        write(s"x_reset <= !was_running;")
        write(s"was_running <= running;")
        leave
        write(s"end")
        leave
        write(s"end")

        // Determine the port ID.
        write(s"reg [31:0] port_id;")
        write(s"always @(posedge clk) begin")
        enter
        write(s"if (write_port) begin")
        enter
        write(s"port_id <= PWDATA;")
        leave
        write(s"end")
        leave
        write(s"end")
        write

        // Create the write signals.
        for (i <- inputStreams) {
            write(s"wire write${i.index} = write_data" +
                  s"&& port_id == ${i.index};")
        }
        write

        // Create the read signals.
        for (o <- outputStreams) {
            write(s"wire read${o.index} = read_data && port_id == ${o.index};")
            write(s"wire [31:0] dout${o.index};")
        }
        write

        // Create the count wires.
        for (s <- inputStreams ++ outputStreams) {
            write(s"wire [31:0] count${s.index};")
        }
        write

        // Instantiate the module.
        write(s"XModule X(.clk(clk), .rst(x_reset)")
        enter
        for (i <- inputStreams.map(_.index)) {
            write(s", .din$i(PWDATA), " +
                  s".write$i(write$i), " +
                  s".count$i(count$i)")
        }
        for (o <- outputStreams.map(_.index)) {
            write(s", .dout$o(dout$o), " +
                  s".read$o(read$o), " +
                  s".count$o(count$o)")
        }
        leave
        write(s");")
        write

        // Drive PRDATA.
        write(s"always @(posedge clk) begin")
        enter
        write(s"if (PSELx & !PENABLE & !PWRITE) begin")
        enter
        write(s"if (PADDR == COUNT_ADDR) begin")
        enter
        write(s"case (port_id)")
        enter
        for (s <- inputStreams ++ outputStreams) {
            val index = s.index
            write(s"$index: PRDATA <= count$index;")
        }
        leave
        write(s"endcase")
        leave
        write(s"end else begin")
        enter
        write(s"case (port_id)")
        enter
        for (o <- outputStreams.map(_.index)) {
            write(s"$o: PRDATA <= dout$o;")
        }
        leave
        write(s"endcase")
        leave
        write(s"end")
        leave
        write(s"end")
        leave
        write(s"end")
        write

        leave
        write(s"endmodule")

        writeFile(dir, "sp_interface.v")

    }

}
