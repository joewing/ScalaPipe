module sp_addI(a_in, b_in, c_out);

    parameter WIDTH = 32;
    input wire [WIDTH-1:0] a_in;
    input wire [WIDTH-1:0] b_in;
    output wire [WIDTH-1:0] c_out;

    assign c_out = a_in + b_in;

endmodule

module sp_subI(a_in, b_in, c_out);

    parameter WIDTH = 32;
    input wire [WIDTH-1:0] a_in;
    input wire [WIDTH-1:0] b_in;
    output wire [WIDTH-1:0] c_out;

    assign c_out = a_in - b_in;

endmodule

module sp_mul_plat(clk, start_in, a_in, b_in, c_out, ready_out);

    parameter WIDTH         = 24;
    parameter OUTPUT_WIDTH  = WIDTH * 2;
    parameter SHIFT         = 18;

    input wire clk;
    input wire start_in;
    input wire [WIDTH-1:0] a_in;
    input wire [WIDTH-1:0] b_in;
    output wire [OUTPUT_WIDTH-1:0] c_out;
    output wire ready_out;

    sp_mul_impl #(.SHIFT(SHIFT), .WIDTH(WIDTH), .OUTPUT_WIDTH(OUTPUT_WIDTH))
        impl(
            .clk(clk),
            .start_in(start_in),
            .a_in(a_in),
            .b_in(b_in),
            .c_out(c_out),
            .ready_out(ready_out));

endmodule

module sp_usb_sync(
    input wire clk,
    input wire rst,
    inout wire [7:0] usb_data,
    input wire rxf_n,
    input wire txe_n,
    output reg rd_n,
    output reg wr_n,
    input wire [7:0] din,
    input wire write,
    output wire full,
    output wire [7:0] dout,
    input wire read,
    output wire avail
);

    parameter STATE_IDLE = 0;
    parameter STATE_WRITE1 = 1;
    parameter STATE_WRITE2 = 2;
    parameter STATE_WRITE3 = 3;
    parameter STATE_WRITE4 = 4;
    parameter STATE_READ1 = 5;
    parameter STATE_READ2 = 6;
    parameter STATE_READ3 = 7;
    parameter STATE_READ4 = 8;
    parameter STATE_READ5 = 9;
    parameter STATE_READ6 = 10;
    parameter STATE_WAIT = 11;

    // Synchronize rxf and txe.
    reg rxf1, rxf2;
    reg txe1, txe2;
    always @(posedge clk) begin
        if (rst) begin
            {rxf1, rxf2} <= 2'b00;
            {txe1, txe2} <= 2'b00;
        end else begin
            {rxf2, rxf1} <= {rxf1, ~rxf_n};
            {txe2, txe1} <= {txe1, ~txe_n};
        end
    end
    wire rxf = rxf1 & rxf2;
    wire txe = txe1 & txe2;

    // Handle USB writes to the internal buffer.
    wire write_reset;
    reg write_pending;
    reg [7:0] write_buffer;
    always @(posedge clk) begin
        if (rst) begin
            write_pending <= 0;
        end else begin
            if (write) begin
                write_buffer <= din;
                write_pending <= 1;
            end
            if (write_reset) begin
                write_pending <= 0;
            end
        end
    end

    // Handle reads from the internal buffer.
    wire read_reset;
    reg read_pending;
    reg [7:0] read_buffer;
    always @(posedge clk) begin
        if (rst) begin
            read_pending <= 0;
        end else begin
            if (read) begin
                read_pending <= 0;
            end
            if (read_reset) begin
                read_pending <= 1;
            end
        end
    end

    // 100 MHz means each clock tick is 10ns.
    // Write-path requirements:
    //      5ns from do_write to !wr_n      (1 clock)
    //      5ns from !wr_n to !do_write     (1 clock)
    //      30ns from !wr_n to wr_n         (3 clocks)
    //      14ns from wr_n to txe valid     (2 + 1 clocks)
    // Read-path requirements:
    //      14ns from !rd_n to data valid   (2 clocks)
    //      30ns from !rd_n to rd_n         (3 clocks)
    //      14ns from rd_n to rxf valid     (2 + 1 clocks)

    reg [3:0] state;
    reg [3:0] next_state;
    always @(*) begin
        case (state)
            STATE_IDLE:
                if (write_pending && txe) begin
                    next_state <= STATE_WRITE1;
                end else if (!read_pending && rxf) begin
                    next_state <= STATE_READ1;
                end else begin
                    next_state <= STATE_IDLE;
                end
            STATE_WRITE1: next_state <= STATE_WRITE2;
            STATE_WRITE2: next_state <= STATE_WRITE3;
            STATE_WRITE3: next_state <= STATE_WRITE4;
            STATE_WRITE4: next_state <= STATE_WAIT;
            STATE_READ1: next_state <= STATE_READ2;
            STATE_READ2: next_state <= STATE_READ3;
            STATE_READ3: next_state <= STATE_READ4;
            STATE_READ4: next_state <= STATE_READ5;
            STATE_READ5: next_state <= STATE_READ6;
            STATE_READ6: next_state <= STATE_WAIT;
            STATE_WAIT: next_state <= STATE_IDLE;
            default: next_state <= STATE_IDLE;
        endcase
    end

    // State machine.
    always @(posedge clk) begin
        if (rst) begin
            state <= STATE_IDLE;
        end else begin
            state <= next_state;
        end
    end

    // Manage the USB FIFO control signals.
    reg do_write;
    always @(posedge clk) begin
        if (rst) begin
            rd_n <= 1;
            wr_n <= 1;
            do_write <= 0;
        end else begin
            case (next_state)
                STATE_WRITE1:   wr_n <= 1;
                STATE_WRITE2:   wr_n <= 0;
                STATE_WRITE3:   wr_n <= 0;
                STATE_WRITE4:   wr_n <= 0;
                default:        wr_n <= 1;
            endcase
            case (next_state)
                STATE_WRITE1:   do_write <= 1;
                STATE_WRITE2:   do_write <= 1;
                STATE_WRITE3:   do_write <= 1;
                STATE_WRITE4:   do_write <= 0;
                default:        do_write <= 0;
            endcase
            case (next_state)
                STATE_READ1:    rd_n <= 0;
                STATE_READ2:    rd_n <= 0;
                STATE_READ3:    rd_n <= 0;
                STATE_READ4:    rd_n <= 1;
                STATE_READ5:    rd_n <= 1;
                STATE_READ6:    rd_n <= 1;
                default:        rd_n <= 1;
            endcase
            case (next_state)
                STATE_READ3:    read_buffer <= usb_data;
                default:        read_buffer <= read_buffer;
            endcase
        end
    end

    assign read_reset = state == STATE_READ6;
    assign write_reset = state == STATE_WRITE4;
    assign usb_data = do_write ? write_buffer : 8'bz;
    assign full = write_pending | write;
    assign avail = read_pending & !read;
    assign dout = read_buffer;

endmodule

module sp_dram(

    inout wire [15:0] dram_dq,
    output wire [12:0] dram_a,
    output wire [1:0] dram_ba,
    output wire dram_cke,
    output wire dram_ras_n,
    output wire dram_cas_n,
    output wire dram_we_n,
    output wire dram_dm,
    inout wire dram_udqs,
    inout wire dram_rzq,
    output wire dram_udm,
    inout wire dram_dqs,
    output wire dram_ck,
    output wire dram_ck_n,
    input wire sys_clk,

    output wire clk,
    output reg rst,

    input wire [24:0] addr,
    input wire [127:0] wdata,
    input wire [15:0] mask,
    input wire we,
    input wire re,
    output wire full,
    output wire [127:0] rdata,
    output wire ravail

);

    wire calib_done;
    wire cmd_en = we | re;
    wire [2:0] cmd_instr = re ? 3'b001 : 3'b000;
    wire [5:0] cmd_bl = 0;
    wire [29:0] cmd_byte_addr = {addr, 4'b0000};
    wire dram_rst;
    wire wr_full;
    wire cmd_full;
    wire rd_empty;
    wire rd_en = ravail;

    mig_lpddr mem(

        .mcb3_dram_dq(dram_dq),
        .mcb3_dram_a(dram_a),
        .mcb3_dram_ba(dram_ba),
        .mcb3_dram_cke(dram_cke),
        .mcb3_dram_ras_n(dram_ras_n),
        .mcb3_dram_cas_n(dram_cas_n),
        .mcb3_dram_we_n(dram_we_n),
        .mcb3_dram_dm(dram_dm),
        .mcb3_dram_udqs(dram_udqs),
        .mcb3_rzq(dram_rzq),
        .mcb3_dram_udm(dram_udm),
        .mcb3_dram_dqs(dram_dqs),
        .mcb3_dram_ck(dram_ck),
        .mcb3_dram_ck_n(dram_ck_n),

        .c3_sys_clk(sys_clk),
        .c3_sys_rst_i(1'b0),
        .c3_calib_done(calib_done),
        .c3_clk0(clk),
        .c3_rst0(dram_rst),

        .c3_p0_cmd_clk(clk),
        .c3_p0_cmd_en(cmd_en),
        .c3_p0_cmd_instr(cmd_instr),
        .c3_p0_cmd_bl(cmd_bl),
        .c3_p0_cmd_byte_addr(cmd_byte_addr),
        .c3_p0_cmd_empty(),
        .c3_p0_cmd_full(cmd_full),

        .c3_p0_wr_clk(clk),
        .c3_p0_wr_en(we),
        .c3_p0_wr_mask(~mask),
        .c3_p0_wr_data(wdata),
        .c3_p0_wr_full(wr_full),
        .c3_p0_wr_empty(),
        .c3_p0_wr_count(),
        .c3_p0_wr_underrun(),
        .c3_p0_wr_error(),

        .c3_p0_rd_clk(clk),
        .c3_p0_rd_en(rd_en),
        .c3_p0_rd_data(rdata),
        .c3_p0_rd_full(),
        .c3_p0_rd_empty(rd_empty),
        .c3_p0_rd_count(),
        .c3_p0_rd_overflow(),
        .c3_p0_rd_error()

    );

    reg [4:0] reset_counter = 0;
    always @(posedge clk) begin
        if (reset_counter[4]) begin
            rst <= dram_rst;
        end else begin
            reset_counter <= reset_counter + 1;
            rst <= 1;
        end
    end

    assign full = cmd_full | wr_full;
    assign ravail = ~rd_empty;

endmodule
