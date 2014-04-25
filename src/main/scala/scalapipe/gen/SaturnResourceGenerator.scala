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

    private def swapOutput(byte: Int, bit: Int, name: String) {
        val bottom = bit
        val top = bottom + 7
        write(s"${byte}: usb_output <= $name[$top:$bottom];")
    }

    private def swapInput(byte: Int, bit: Int, name: String) {
        val bottom = bit
        val top = bottom + 7
        write(s"${byte}: ${name}[$top:$bottom] <= usb_input;")
    }

    private def swapBus(name: String,
                        func: (Int, Int, String) => Unit,
                        vtype: ValueType,
                        offset: Int = 0) {
        vtype match {
            case at: ArrayValueType =>
                for (i <- 0 until at.length) {
                    val nextOffset = offset + i * ((at.itemType.bits + 7) / 8)
                    swapBus(name, func, at.itemType, nextOffset)
                }
            case st: StructValueType =>
                var nextOffset = offset
                for ((ft, i) <- st.fieldTypes.zipWithIndex) {
                    swapBus(name, func, ft, nextOffset)
                    nextOffset += (ft.bits + 7) / 8
                }
            case _ =>
                val bytes = (vtype.bits + 7) / 8
                val bitOffset = offset * 8
                for (i <- 0 until bytes) {
                    func(offset + i, bitOffset + (bytes - i - 1) * 8, name)
                }
        }
    }

    private def emitTopFile(dir: File) {

        write(s"module top$id(")
        enter
        write(s"input wire sysclk,")

        write(s"inout wire [7:0] usb_data,")
        write(s"input wire usb_rxf_n,")
        write(s"input wire usb_txe_n,")
        write(s"output wire usb_rd_n,")
        write(s"output wire usb_wr_n,")
        write(s"output wire usb_siwu,")

        write(s"inout wire [15:0] dram_dq,")
        write(s"output wire [12:0] dram_a,")
        write(s"output wire [1:0] dram_ba,")
        write(s"output wire dram_cke,")
        write(s"output wire dram_ras_n,")
        write(s"output wire dram_cas_n,")
        write(s"output wire dram_we_n,")
        write(s"output wire dram_dm,")
        write(s"inout wire dram_udqs,")
        write(s"inout wire dram_rzq,")
        write(s"output wire dram_udm,")
        write(s"inout wire dram_dqs,")
        write(s"output wire dram_ck,")
        write(s"output wire dram_ck_n,")
        write(s"output reg led")

        leave
        write(s");")
        enter

        write(s"assign usb_siwu = 1; // Not used")

        write(s"wire clk;           // 100 MHz")
        write(s"wire rst;")

        // Blink the LED when running.
        write(s"reg [24:0] blink;")
        write(s"always @(posedge clk) begin")
        enter
        write(s"if (rst) blink <= 0;")
        write(s"else blink <= blink + 1;")
        leave
        write(s"end")

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
        write(s".rxf_n(usb_rxf_n),")
        write(s".txe_n(usb_txe_n),")
        write(s".rd_n(usb_rd_n),")
        write(s".wr_n(usb_wr_n),")
        write(s".din(usb_output),")
        write(s".write(usb_write),")
        write(s".full(usb_full),")
        write(s".dout(usb_input),")
        write(s".read(usb_read),")
        write(s".avail(usb_avail)")
        leave
        write(s");")

        // Protocol:
        //  FPGA->host:
        //      1 byte for each host->device stream; high bit is "full"
        //          indicator and lower 7 bits are the stream ID.
        //      1 byte device->host stream indicator for transfer.
        //          0 if no data, 255 if no kernels running.
        //      device->host data (little endian).
        // host->FPGA:
        //      1 byte stream indicator for host->device transfer.
        //          0 if no stream.
        //      host->device data (little endian).

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
        write(s"wire running;")
        write(s"reg got_stop;")

        // State machine for USB communication.
        val acceptStateOffset = 1
        val sendStateOffset = acceptStateOffset + inputStreams.size
        val sentinelState = sendStateOffset + outputStreams.size
        val readState = sentinelState + 1
        val readStateOffset = readState + 1
        val stopState = readStateOffset + 255
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
        write(s"got_stop <= 0;")
        write(s"state <= 0;")
        leave
        write(s"end else begin")
        enter
        write(s"case (state)")
        enter
        write(s"0: // Wait for signal from software.")
        enter
        write(s"if (usb_avail) begin")
        enter
        write(s"usb_read <= 1;")
        write(s"state <= 1;")
        leave
        write(s"end")
        leave

        // Inform the host which ports are accepting input.
        for ((i, offset) <- inputStreams.zipWithIndex) {
            val state = acceptStateOffset + offset
            val index = i.index
            write(s"${state}: // Update input $index")
            enter
            write(s"if (!usb_full) begin")
            enter
            write(s"usb_write <= 1;")
            write(s"usb_output[7] <= full$index;")
            write(s"usb_output[6:0] <= ${index};")
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
            write(s"if (!usb_full) begin")
            enter
            write(s"if (avail$index) begin")
            enter

            write(s"case (offset)")
            enter
            swapBus(s"data${index}", swapOutput, o.valueType)
            leave
            write(s"endcase")
            write(s"usb_write <= 1;")
            write(s"if (offset != ${bytes - 1}) begin")
            enter
            write(s"offset <= offset + 1;")
            leave
            write(s"end else begin")
            enter
            write(s"state <= ${readState};")
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

        // Send a sentinel if there's no data for the host.
        write(s"${sentinelState}: // No data for the host.")
        enter
        write(s"if (!usb_full) begin")
        enter
        write(s"usb_write <= 1;")
        write(s"usb_output <= (running | !got_stop) ? 0 : 255;")
        write(s"state <= ${readState};")
        leave
        write(s"end")
        leave

        // Select a read state if there is data from the host.
        write(s"${readState}: // Check for input")
        enter
        write(s"if (usb_avail) begin")
        enter
        write(s"usb_read <= 1;")
        write(s"offset <= 0;")
        write(s"if (usb_input != 0) begin")
        enter
        write(s"state <= usb_input + ${readStateOffset};")
        leave
        write(s"end else begin")
        enter
        write(s"state <= 1;")
        leave
        write(s"end")
        leave
        write(s"end")
        leave

        // Read data from the host.
        for (i <- inputStreams) {
            val index = i.index
            val state = readStateOffset + index
            val bytes = (i.valueType.bits + 7) / 8
            write(s"${state}: // Read input ${index}")
            enter
            write(s"if (usb_avail) begin")
            enter
            write(s"case (offset)")
            enter
            swapBus(s"data${index}", swapInput, i.valueType)
            leave
            write(s"endcase")
            write(s"if (offset != ${bytes - 1}) begin")
            enter
            write(s"offset <= offset + 1;")
            leave
            write(s"end else begin")
            enter
            write(s"state <= 1;")
            write(s"write$index <= 1;")
            leave
            write(s"end") // offset
            leave
            write(s"end else begin") // usb_read
            enter
            write(s"usb_read <= 1;")
            leave
            write(s"end") // avail
            leave
        }

        write(s"${stopState}: // Stop")
        enter
        write(s"got_stop <= 1;")
        leave

        leave
        write(s"endcase")
        leave
        write(s"end")
        leave
        write(s"end")   // always

        write(s"always @(*) begin")
        enter
        write(s"case (state)")
        enter
        write(s"0: led <= 0;")
        write(s"${stopState}: led <= 1;")
        write(s"default: led <= blink[24];")
        leave
        write(s"endcase")
        leave
        write(s"end")

        // Connect the DRAM controller.
        write(s"wire [24:0] ram_addr;")
        write(s"wire [127:0] ram_data_to_main;")
        write(s"wire [127:0] ram_data_from_main;")
        write(s"wire [15:0] ram_mask;")
        write(s"wire ram_we;")
        write(s"wire ram_re;")
        write(s"wire ram_ready;")
        write(s"sp_dram dram(")
        enter
        write(s".dram_dq(dram_dq),")
        write(s".dram_a(dram_a),")
        write(s".dram_ba(dram_ba),")
        write(s".dram_cke(dram_cke),")
        write(s".dram_ras_n(dram_ras_n),")
        write(s".dram_cas_n(dram_cas_n),")
        write(s".dram_we_n(dram_we_n),")
        write(s".dram_dm(dram_dm),")
        write(s".dram_udqs(dram_udqs),")
        write(s".dram_rzq(dram_rzq),")
        write(s".dram_udm(dram_udm),")
        write(s".dram_dqs(dram_dqs),")
        write(s".dram_ck(dram_ck),")
        write(s".dram_ck_n(dram_ck_n),")
        write(s".sys_clk(sysclk),")
        write(s".clk(clk),")
        write(s".rst(rst),")
        write(s".addr(ram_addr),")
        write(s".din(ram_data_to_main),")
        write(s".dout(ram_data_from_main),")
        write(s".mask(ram_mask),")
        write(s".we(ram_we),")
        write(s".re(ram_re),")
        write(s".ready(ram_ready)")
        leave
        write(s");")

        // Instantiate the ScalaPipe kernels.
        write(s"fpga$id sp(")
        enter
        write(s".clk(clk),")
        write(s".rst(rst),")
        write(s".running(running),")
        write(s".ram_addr(ram_addr),")
        write(s".ram_data_to_main(ram_data_to_main),")
        write(s".ram_data_from_main(ram_data_from_main),")
        write(s".ram_mask(ram_mask),")
        write(s".ram_re(ram_re),")
        write(s".ram_we(ram_we),")
        write(s".ram_ready(ram_ready)")
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
