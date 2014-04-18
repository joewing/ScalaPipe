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
        impl(clk, start_in, a_in, b_in, c_out, ready_out);

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
    reg [3:0] state;
    reg [3:0] next_state;
    always @(*) begin
        next_state <= STATE_IDLE;
        if (state == STATE_IDLE) begin
            if (write_pending & !rxf_n) begin
                next_state <= STATE_WRITE;
            end else if (!read_pending & !txe_n) begin
                next_state <= STATE_READ;
            end
        end
    end

    // Manage the USB FIFO.
    always @(posedge clk) begin
        rd_n <= 1;
        wr_n <= 1;
        if (rst) begin
            state <= STATE_IDLE;
        end else begin
            state <= next_state;
            if (state == STATE_READ) begin
                rd_n <= 0;
            end
            if (state == STATE_WRITE) begin
                wr_n <= 0;
            end
        end
    end

    assign usb_data = next_state == STATE_WRITE ? write_buffer : 8'bz;
    assign full = !write_pending;
    assign avail = read_pending;
    assign dout = read_buffer;

endmodule
