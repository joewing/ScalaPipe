package scalapipe.gen

import scalapipe._
import java.io.File

private[scalapipe] class SaturnResourceGenerator(
        _sp: ScalaPipe,
        _device: Device
    ) extends HDLResourceGenerator(_sp, _device) {

    override def getRules: String = {
        ""
    }

    override def emit(dir: File) {
        super.emit(dir)
        emitTopFile(dir)
    }

    private def emitTopFile(dir: File) {

        write(s"module top$id(")
        enter
        write(s"input wire clk,")
        write(s"input wire rst,")
        write(s"inout wire [7:0] usb_data,")
        write(s"input wire usb_rxf_n,")
        write(s"input wire usb_txe_n,")
        write(s"output reg usb_rd_n,")
        write(s"output reg usb_wr_n,")
        write(s"output wire siwu")
        leave
        write(s");")
        enter

        write(s"assign siwu = 1; // Not used")

        // Synchronize the USB interface.
        write(s"wire [7:0] usb_input;")
        write(s"reg usb_read;")
        write(s"wire usb_avail;")
        write(s"reg [7:0] usb_output;")
        write(s"reg usb_write;")
        write(s"wire usb_full;")
        write(s"sp_usb_sync usb(")
        enter
        write(s".clk(clk),")
        write(s".rst(rst),")
        write(s".usb_data(usb_data),")
        write(s".rxf_n(rxf_n),")
        write(s".txe_n(txe_n),")
        write(s".rd_n(rd_n),")
        write(s".wr_n(wr_n),")
        write(s".din(usb_output),")
        write(s".write(usb_write),")
        write(s".full(usb_full),")
        write(s".dout(usb_input),")
        write(s".read(usb_read),")
        write(s".avail(usb_avail)")
        leave
        write(s");")

        // Host->FPGA protocol:
        // command (hit bit is type)
        //  input port number (1 byte)
        //  data (n-bytes)

        // FPGA->Host protocol:
        //  command (high bit is type)
        //  type 0:
        //      available input port number (remaining byte)
        //  type 1:
        //      output port number (remaining byte)
        //      data (n-bytes)

        // Signals to the ScalaPipe kernels.
        for (i <- inputStreams) {
            val index = i.index
            val width = i.valueType.bits
            write(s"reg write$index;")
            write(s"wire full$index;")
            write(s"reg [${width - 1}:0] data$index;")
        }

        // Signals from the ScalaPipe kernels.
        for (o <- outputStreams) {
            val index = o.index
            val width = o.valueType.bits
            write(s"wire [${width - 1}:0] data$index;")
            write(s"reg read$index;")
            write(s"wire avail$index;")
        }

        // State machine for USB communication.
        // The first n states inform the host which ports
        // are accepting input.  The next states send data to
        // the host (if there is data available).  Finally, the
        // last states read data from the host.
        val acceptStateOffset = 0
        val sendStateOffset = acceptStateOffset + inputStreams.size
        val readState = sendStateOffset + outputStreams.size
        val readStateOffset = readState + 1
        write(s"reg [31:0] state;")
        write(s"reg [31:0] offset;")
        write(s"always @(posedge clk) begin")
        enter
        for (i <- inputStreams) {
            write(s"write${i.index} <= 0;")
        }
        for (o <- outputStreams) {
            write(s"read${o.index} <= 0;")
        }
        write(s"usb_read <= 0;")
        write(s"usb_write <= 0;")
        write(s"if (rst) begin")
        enter
        write(s"state <= 0;")
        leave
        write(s"end else begin")
        enter
        write(s"case (state)")
        enter

        // Inform the host which ports are accepting input.
        for ((i, offset) <- inputStreams.zipWithIndex) {
            val state = acceptStateOffset + offset
            val index = i.index
            write(s"${state}: // Update input $index")
            enter
            write(s"if (!usb_full & !usb_write) begin")
            enter
            write(s"if (!full$index) begin")
            enter
            write(s"usb_write <= 1;")
            write(s"usb_output <= ${index};")
            leave
            write(s"end")
            write(s"offset <= 0;")
            write(s"state <= ${state + 1};")
            leave
            write(s"end")
            leave
        }

        // Send data to the host.
        for ((o, offset) <- outputStreams.zipWithIndex) {
            val state = sendStateOffset + offset
            val bytes = (o.valueType.bits + 7) / 8
            val index = o.index
            write(s"${state}: // Send output $index")
            enter
            write(s"if (!usb_full & !usb_write) begin")
            enter
            write(s"if (avail$index) begin")
            enter

            write(s"case (offset)")
            enter
            for (byte <- 0 until bytes) {
                val bottom = byte * 8
                val top = bottom + 7
                write(s"${byte}: usb_output <= data${index}[$top:$bottom];")
            }
            leave
            write(s"endcase")
            write(s"usb_write <= 1;")
            write(s"if (offset != ${bytes - 1}) begin")
            enter
            write(s"offset <= offset + 1;")
            leave
            write(s"end else begin")
            enter
            write(s"offset <= 0;")
            write(s"state <= ${state + 1};")
            write(s"read$index <= 1;")
            leave
            write(s"end") // offset
            leave
            write(s"end else begin")
            enter
            write(s"state <= ${state + 1};")
            leave
            write(s"end") // avail
            leave
            write(s"end") // full
            leave
        }

        // Select a read state if there is data from the host.
        write(s"${readState}: // Check for input")
        enter
        write(s"if (usb_avail) begin")
        enter
        write(s"if (!usb_read) begin")
        enter
        write(s"usb_read <= 1;")
        write(s"state <= usb_input + ${readStateOffset};")
        write(s"offset <= 0;")
        leave
        write(s"end")
        leave
        write(s"end else begin")
        enter
        write(s"state <= 0;")
        leave
        write(s"end")
        leave

        // Read data from the host.
        for ((i, offset) <- inputStreams.zipWithIndex) {
            val state = readStateOffset + offset
            val index = i.index
            val bytes = (i.valueType.bits + 7) / 8
            write(s"${state}: // Read input ${index}")
            enter
            write(s"if (usb_avail) begin")
            enter
            write(s"if (usb_read) begin")
            enter
            write(s"case (offset)")
            enter
            for (byte <- 0 until bytes) {
                val bottom = byte * 8
                val top = bottom + 7
                write(s"${byte}: data${index}[$top:$bottom] <= usb_input;")
            }
            leave
            write(s"endcase")
            write(s"if (offset != ${bytes - 1}) begin")
            enter
            write(s"offset <= offset + 1;")
            leave
            write(s"end else begin")
            enter
            write(s"state <= 0;")
            write(s"write$index <= 1;")
            leave
            write(s"end") // offset
            leave
            write(s"end else begin") // usb_read
            enter
            write(s"usb_read <= 1;")
            leave
            write(s"end") // usb_read
            leave
            write(s"end") // avail
            leave
        }

        leave
        write(s"endcase")
        leave
        write(s"end")
        leave
        write(s"end")   // always

        // Connect the DRAM controller.

        // Instantiate the ScalaPipe kernels.
        write(s"fpga$id sp(")
        enter
        write(s".clk(clk),")
        write(s".rst(rst),")
        write(s".running()")
        write(s", .ram_addr(ram_addr)")
        write(s", .ram_in(ram_out)")
        write(s", .ram_out(ram_in)")
        write(s", .ram_mask(ram_mask)")
        write(s", .ram_re(ram_re)")
        write(s", .ram_we(ram_we)")
        write(s", .ram_ready(ram_ready)")
        for (i <- inputStreams) {
            val index = i.index
            write(s", .input${index}_data(data$index)")
            write(s", .input${index}_write(write$index)")
            write(s", .input${index}_full(full$index)")
        }
        for (o <- outputStreams) {
            val index = o.index
            write(s", .output${index}_data(data$index)")
            write(s", .output${index}_read(read$index)")
            write(s", .output${index}_avail(avail$index)")
        }
        leave
        write(s");")

        leave
        write(s"endmodule")
        writeFile(dir, s"top_${device.label}.v")

    }

}
