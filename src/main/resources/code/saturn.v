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
    output wire rd_n,
    output wire wr_n,
    input wire [7:0] din,
    input wire write,
    output wire full,
    output wire [7:0] dout,
    input wire read,
    output wire avail
);

    parameter STATE_IDLE = 0;
    parameter STATE_WRITE = 1;
    parameter STATE_READ = 2;

    // Handle USB writes to the internal buffer.
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
            if (!wr_n) begin
                write_pending <= 0;
            end
        end
    end

    // Handle reads from the internal buffer.
    reg read_pending;
    reg [7:0] read_buffer;
    always @(posedge clk) begin
        if (rst) begin
            read_pending <= 0;
        end else begin
            if (read) begin
                read_pending <= 0;
            end
            if (!rd_n) begin
                read_buffer <= usb_data;
                read_pending <= 1;
            end
        end
    end

    // Determine the next state.
    reg [1:0] state;
    reg [1:0] next_state;
    always @(*) begin
        next_state <= STATE_IDLE;
        if (state == STATE_IDLE) begin
            if (write_pending & !txe_n) begin
                next_state <= STATE_WRITE;
            end else if (!read_pending & !rxf_n) begin
                next_state <= STATE_READ;
            end
        end
    end

    // Manage the USB FIFO.
    always @(posedge clk) begin
        if (rst) begin
            state <= STATE_IDLE;
        end else begin
            state <= next_state;
        end
    end

    assign rd_n = state == STATE_READ ? 1'b0 : 1'b1;
    assign wr_n = state == STATE_WRITE ? 1'b0 : 1'b1;
    assign usb_data = next_state == STATE_WRITE ? write_buffer : 8'bz;
    assign full = write_pending;
    assign avail = read_pending;
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
    output wire rst,
    input wire [24:0] addr,
    input wire [127:0] din,
    output wire [127:0] dout,
    input wire [15:0] mask,
    input wire we,
    input wire re,
    output wire ready

);

    reg waiting;

    wire calib_done;
    wire cmd_en = we | re;
    wire [2:0] cmd_instr = re ? 3'b001 : 3'b000;
    wire [5:0] cmd_bl = 0;
    wire [29:0] cmd_byte_addr = {addr, 4'b0000};
    wire cmd_full;

    wire wr_en = we;
    wire rd_en;
    wire rd_empty;
    wire [127:0] rd_data;
    reg [127:0] read_buffer;

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
        .c3_sys_rst_i(rst),
        .c3_calib_done(calib_done),
        .c3_clk0(clk),
        .c3_rst0(rst),

        .c3_p0_cmd_clk(clk),
        .c3_p0_cmd_en(cmd_en),
        .c3_p0_cmd_instr(cmd_instr),
        .c3_p0_cmd_bl(cmd_bl),
        .c3_p0_cmd_byte_addr(cmd_byte_addr),
        .c3_p0_cmd_empty(),
        .c3_p0_cmd_full(cmd_full),

        .c3_p0_wr_clk(clk),
        .c3_p0_wr_en(wr_en),
        .c3_p0_wr_mask(~mask),
        .c3_p0_wr_data(din),
        .c3_p0_wr_full(),
        .c3_p0_wr_empty(),
        .c3_p0_wr_count(),
        .c3_p0_wr_underrun(),
        .c3_p0_wr_error(),

        .c3_p0_rd_clk(clk),
        .c3_p0_rd_en(rd_en),
        .c3_p0_rd_data(rd_data),
        .c3_p0_rd_full(),
        .c3_p0_rd_empty(rd_empty),
        .c3_p0_rd_count(),
        .c3_p0_rd_overflow(),
        .c3_p0_rd_error()

    );

    always @(posedge clk) begin
        if (rst) begin
            waiting <= 0;
        end else begin
            if (re) begin
                waiting <= 1;
            end else if (!rd_empty) begin
                waiting <= 0;
                read_buffer <= rd_data;
            end
        end
    end

    assign rd_en = !rd_empty;
    assign dout = read_buffer;
    assign ready = !waiting & !cmd_full & calib_done;

endmodule
