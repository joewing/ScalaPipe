
module sp_register(clk, rst, din, dout, re, we, avail, full);

    parameter WIDTH = 8;
    parameter ADDR_WIDTH = 0;

    input wire clk;
    input wire rst;
    input wire [WIDTH-1:0] din;
    output wire [WIDTH-1:0] dout;
    input wire re;
    input wire we;
    output wire avail;
    output wire full;

    reg [WIDTH-1:0] mem;
    reg has_data;
    wire do_read;
    wire do_write;

    assign full = has_data;
    assign avail = has_data;
    assign do_read = full & re;
    assign do_write = !has_data & we;

    always @(posedge clk) begin
        if (rst) begin
            has_data <= 0;
        end else begin
            if (do_write) begin
                mem <= din;
                has_data <= 1;
            end
            if (do_read) begin
                has_data <= 0;
            end
        end
    end

    assign dout = mem;

endmodule

module sp_ram(clk, rst, addr, din, dout, mask, re, we, ready);

    parameter WIDTH = 8;
    parameter DEPTH = 2;
    parameter ADDR_WIDTH = 32;

    input wire clk;
    input wire rst;
    input wire [ADDR_WIDTH-1:0] addr;
    input wire [WIDTH-1:0] din;
    output reg [WIDTH-1:0] dout;
    input wire [WIDTH/8-1:0] mask;
    input wire re;
    input wire we;
    output wire ready;

    reg rre;
    reg rwe;
    reg [WIDTH/8-1:0] rmask;
    reg [WIDTH-1:0] rin;
    reg [ADDR_WIDTH-1:0] raddr;
    reg [WIDTH-1:0] data [0:DEPTH-1];
    reg busy;

    always @(posedge clk) begin
        if (rst) begin
            rre <= 0;
            rwe <= 0;
            busy <= 0;
        end else begin
            raddr <= addr;
            rin <= din;
            rmask <= mask;
            rre <= re;
            rwe <= we;
            busy <= re | we;
        end
    end

    always @(posedge clk) begin
        if (rre) begin
            dout <= data[raddr];
        end
    end

    genvar i;
    generate
        for (i = 0; i < WIDTH / 8; i = i + 1) begin : select_bytes
            always @(posedge clk) begin
                if (rwe & rmask[i]) begin
                    data[raddr][i*8+7:i*8] <= rin[i*8+7:i*8];
                end
            end
        end
    endgenerate

    assign ready = ~busy;

endmodule

module sp_fifo(clk, rst, din, dout, re, we, avail, full,
               mem_addr, mem_in, mem_out, mem_re, mem_we, mem_ready);

    parameter WIDTH = 8;
    parameter ADDR_WIDTH = 1;

    input wire clk;
    input wire rst;
    input wire [WIDTH-1:0] din;
    output wire [WIDTH-1:0] dout;
    input wire re;
    input wire we;
    output reg avail;
    output wire full;
    output reg [ADDR_WIDTH-1:0] mem_addr;
    input wire [WIDTH-1:0] mem_in;
    output reg [WIDTH-1:0] mem_out;
    output reg mem_re;
    output reg mem_we;
    input wire mem_ready;

    reg [ADDR_WIDTH-1:0] read_ptr;
    reg [ADDR_WIDTH-1:0] write_ptr;
    reg [ADDR_WIDTH:0] count;
    reg read_pending;
    reg write_pending;

    always @(posedge clk) begin
        mem_re <= 0;
        mem_we <= 0;
        if (rst) begin
            read_ptr <= 0;
            write_ptr <= 0;
            count <= 0;
            read_pending <= 0;
            write_pending <= 0;
            avail <= 0;
        end else if (mem_ready && !mem_re && !mem_we) begin
            if (count != 0 && !avail && !read_pending) begin
                mem_re <= 1;
                mem_addr <= read_ptr;
                count <= count - 1;
                read_pending <= 1;
                if (we) begin
                    mem_out <= din;
                    write_pending <= 1;
                end
            end else if (write_pending) begin
                mem_we <= 1;
                write_pending <= 0;
                mem_addr <= write_ptr;
                write_ptr <= write_ptr + 1;
                count <= count + 1;
            end else if (we) begin
                mem_out <= din;
                mem_we <= 1;
                mem_addr <= write_ptr;
                write_ptr <= write_ptr + 1;
                count <= count + 1;
            end
            if (read_pending) begin
                if (!re || count == 0) begin
                    avail <= 1;
                    read_pending <= 0;
                end
                read_ptr <= read_ptr + 1;
            end
        end else if (we) begin
            mem_out <= din;
            write_pending <= 1;
        end
        if (re) begin
            avail <= 0;
        end
    end

    assign full = count[ADDR_WIDTH] | !mem_ready | mem_we;
    assign dout = mem_in;

endmodule

module sp_divU(clk, start, a_in, b_in, c_out, ready_out);

    parameter WIDTH = 24;

    input wire clk;
    input wire start;
    input wire [WIDTH-1:0] a_in;
    input wire [WIDTH-1:0] b_in;
    output wire [WIDTH-1:0] c_out;
    output wire ready_out;

    reg [WIDTH-1:0] a;
    reg [WIDTH-1:0] b;
    reg [2*WIDTH-1:0] result;
    wire [WIDTH:0] sub = result[2*WIDTH-2:WIDTH-1] - b[WIDTH-1:0];

    reg [7:0] count;
    always @(posedge clk) begin
        if (start) begin
            count <= WIDTH;
        end else if (!ready_out) begin
            count <= count - 1;
        end
    end

    always @(posedge clk) begin
        if (start) begin
            a <= a_in;
            b <= b_in;
            result <= a_in;
        end else if (!ready_out) begin
            if (sub[WIDTH]) begin
                result <= {result[2*WIDTH-2:0], 1'b0};
            end else begin
                result <= {sub[WIDTH-1:0], result[WIDTH-2:0], 1'b1};
            end
        end
    end

    assign ready_out = count == 0;
    assign c_out = result[WIDTH-1:0];

endmodule

module sp_divS(clk, start, a_in, b_in, c_out, ready_out);

    parameter WIDTH = 24;

    input wire clk;
    input wire start;
    input wire [WIDTH-1:0] a_in;
    input wire [WIDTH-1:0] b_in;
    output wire [WIDTH-1:0] c_out;
    output wire ready_out;

    reg [WIDTH-1:0] a;
    reg [WIDTH-1:0] b;
    reg [WIDTH-1:0] absb;
    reg [2*WIDTH-1:0] result;
    wire [WIDTH:0] sub = result[2*WIDTH-2:WIDTH-1] - absb[WIDTH-1:0];
    wire neg = a_in[WIDTH-1] ^ b_in[WIDTH-1];
    wire [WIDTH-1:0] nega = -a_in;
    wire [WIDTH-1:0] negb = -b_in;

    reg [7:0] count;
    always @(posedge clk) begin
        if (start) begin
            count <= WIDTH;
        end else if (!ready_out) begin
            count <= count - 1;
        end
    end

    always @(posedge clk) begin
        if (start) begin
            a <= a_in;
            b <= b_in;
            if (a_in[WIDTH-1]) begin
                result <= nega;
            end else begin
                result <= a_in;
            end
            if (b_in[WIDTH-1]) begin
                absb <= negb;
            end else begin
                absb <= b_in;
            end
        end else if (!ready_out) begin
            if (sub[WIDTH]) begin
                result <= {result[2*WIDTH-2:0], 1'b0};
            end else begin
                result <= {sub[WIDTH-1:0], result[WIDTH-2:0], 1'b1};
            end
        end
    end

    assign ready_out = count == 0;
    assign c_out = neg ? -result[WIDTH-1:0] : result[WIDTH-1:0];

endmodule

/** Generic multiplier. */
module sp_mul_impl(clk, start_in, a_in, b_in, c_out, ready_out);

    parameter WIDTH         = 24;
    parameter OUTPUT_WIDTH  = WIDTH * 2;
    parameter SHIFT         = 18;

    // We can't use constant functions in iverilog, so we
    // expand the computation of the bits to process per cycle.
    parameter MAX_SHIFT = SHIFT < WIDTH ? SHIFT : WIDTH;
    parameter BITS0 = MAX_SHIFT;
    parameter BITS1 = WIDTH % BITS0 != 0 ? BITS0 - 1 : BITS0;
    parameter BITS2 = WIDTH % BITS1 != 0 ? BITS1 - 1 : BITS1;
    parameter BITS3 = WIDTH % BITS2 != 0 ? BITS2 - 1 : BITS2;
    parameter BITS4 = WIDTH % BITS3 != 0 ? BITS3 - 1 : BITS3;
    parameter BITS5 = WIDTH % BITS4 != 0 ? BITS4 - 1 : BITS4;
    parameter BITS6 = WIDTH % BITS5 != 0 ? BITS5 - 1 : BITS5;
    parameter BITS7 = WIDTH % BITS6 != 0 ? BITS6 - 1 : BITS6;
    parameter BITS8 = WIDTH % BITS7 != 0 ? BITS7 - 1 : BITS7;
    parameter BITS9 = WIDTH % BITS8 != 0 ? BITS8 - 1 : BITS8;
    parameter BITSA = WIDTH % BITS9 != 0 ? BITS9 - 1 : BITS9;
    parameter BITSB = WIDTH % BITSA != 0 ? BITSA - 1 : BITSA;
    parameter BITSC = WIDTH % BITSB != 0 ? BITSB - 1 : BITSB;

    parameter BITS = BITSC;
    parameter STATE_COUNT = WIDTH / BITS;

    input wire clk;
    input wire start_in;
    input wire [WIDTH-1:0] a_in;
    input wire [WIDTH-1:0] b_in;
    output wire [OUTPUT_WIDTH-1:0] c_out;
    output wire ready_out;

    reg [WIDTH-1:0] a;
    reg [WIDTH-1:0] b;
    reg [OUTPUT_WIDTH-1:0] result;
    reg [31:0] state;

    wire [OUTPUT_WIDTH-1:0] add_sa = b * a[WIDTH-1:WIDTH-BITS];
    wire [OUTPUT_WIDTH-1:0] add_sb = result << BITS;
    wire [OUTPUT_WIDTH-1:0] add_result;
    sp_addI #(.WIDTH(OUTPUT_WIDTH)) add(add_sa, add_sb, add_result);

    always @(posedge clk) begin
        if (start_in) begin
            state <= STATE_COUNT;
            result <= 0;
            a <= a_in;
            b <= b_in;
        end else if (!ready_out) begin
            result <= add_result;
            a <= a << BITS;
            state <= state - 1;
        end
    end

    assign ready_out = state == 0;
    assign c_out = result;

endmodule

module sp_mulI(clk, start_in, a_in, b_in, c_out, ready_out);

    parameter WIDTH = 32;

    input wire clk;
    input wire start_in;
    input wire [WIDTH-1:0] a_in;
    input wire [WIDTH-1:0] b_in;
    output wire [WIDTH-1:0] c_out;
    output wire ready_out;

    sp_mul_plat #(.WIDTH(WIDTH), .OUTPUT_WIDTH(WIDTH))
        impl(clk, start_in, a_in, b_in, c_out, ready_out);

endmodule

module sp_nlz(a_in, b_out);

    parameter WIDTH = 32;

    input wire [WIDTH-1:0] a_in;
    output reg [WIDTH-1:0] b_out;

    reg [WIDTH-1:0] i;

    always @(*) begin
        b_out <= WIDTH;
        for (i = 0; i < WIDTH; i = i + 1) begin
            if (a_in[i] == 1) b_out <= i;
        end
    end

endmodule

module sp_itof(clk, start, a_in, b_out, ready_out);

    parameter WIDTH = 32;
    parameter EXPONENT = 8;
    parameter FRACTION = 23;

    input wire clk;
    input wire start;
    input wire [WIDTH-1:0] a_in;
    output wire [WIDTH-1:0] b_out;
    output wire ready_out;

    wire [WIDTH-1:0] zeros;
    wire [EXPONENT-1:0] exp = (WIDTH - zeros) + (1 << (EXPONENT - 1)) - 2;
    wire [FRACTION:0] frac = a_in << (FRACTION + 1 - (WIDTH - zeros));
    wire sign = a_in[WIDTH-1];

    sp_nlz #(.WIDTH(WIDTH)) nlz(a_in, zeros);

    assign b_out = a_in == 0 ? 0
                     : {sign, exp, frac[FRACTION-1:0]};

    assign ready_out = 1;

endmodule

module sp_ftoi(clk, start, a_in, b_out, ready_out);

    parameter WIDTH = 32;
    parameter EXPONENT = 8;
    parameter FRACTION = 23;

    input wire clk;
    input wire start;
    input wire [WIDTH-1:0] a_in;
    output wire [WIDTH-1:0] b_out;
    output wire ready_out;

    wire [EXPONENT-1:0] exp = a_in[WIDTH-2:WIDTH-1-EXPONENT];
    wire [FRACTION:0] frac = {1'b1, a_in[FRACTION-1:0]};
    wire sign = a_in[WIDTH-1];
    wire [WIDTH-1:0] result = frac << (exp - 127);

    assign b_out = exp == 0 ? 0 : (sign ? -result : result);
    assign ready_out = 1;

endmodule

module sp_addF(clk, start, a_in, b_in, c_out, ready_out);

    parameter WIDTH = 32;
    parameter EXPONENT = 8;
    parameter FRACTION = 23;

    input wire clk;
    input wire start;
    input wire [WIDTH-1:0] a_in;
    input wire [WIDTH-1:0] b_in;
    output wire [WIDTH-1:0] c_out;
    output wire ready_out;

    reg sign;
    reg [EXPONENT-1:0] exp;
    reg [EXPONENT-1:0] expx;
    reg [EXPONENT-1:0] expy;
    reg signed [FRACTION+1:0] aligned_a;
    reg signed [FRACTION+1:0] aligned_b;
    reg [FRACTION+1:0] result;
    reg [2:0] state;

    localparam STATE_IDLE    = 0;
    localparam STATE_ALIGN  = 1;
    localparam STATE_SIGN    = 2;
    localparam STATE_SUM     = 3;
    localparam STATE_NORM    = 4;

    reg [WIDTH-1:0] a;
    reg [WIDTH-1:0] b;

    wire signa = a[WIDTH-1];
    wire signb = b[WIDTH-1];
    wire [EXPONENT-1:0] expa = a[WIDTH-2:WIDTH-1-EXPONENT];
    wire [EXPONENT-1:0] expb = b[WIDTH-2:WIDTH-1-EXPONENT];
    wire [FRACTION:0] fraca = expa == 0 ? 0 : {1'b1, a[FRACTION-1:0]};
    wire [FRACTION:0] fracb = expb == 0 ? 0 : {1'b1, b[FRACTION-1:0]};
    reg sum_sign;

    wire signed [WIDTH-1:0] add_sa = aligned_a;
    wire signed [WIDTH-1:0] add_sb = aligned_b;
    wire signed [WIDTH-1:0] add_result;
    sp_addI #(.WIDTH(WIDTH)) add(add_sa, add_sb, add_result);

    always @(posedge clk) begin
        if (start) begin
            a <= a_in;
            b <= b_in;
            expx <= a_in[WIDTH-2:WIDTH-1-EXPONENT]
                    - b_in[WIDTH-2:WIDTH-1-EXPONENT];
            expy <= b_in[WIDTH-2:WIDTH-1-EXPONENT]
                    - a_in[WIDTH-2:WIDTH-1-EXPONENT];
            state <= STATE_ALIGN;
        end else if (state == STATE_ALIGN) begin
            if (expa > expb) begin
                aligned_a <= fraca;
                aligned_b <= fracb >> expx;
                exp <= expa;
            end else begin
                aligned_a <= fraca >> expy;
                aligned_b <= fracb;
                exp <= expb;
            end
            state <= STATE_SIGN;
        end else if (state == STATE_SIGN) begin
            if (signa == signb) begin
                aligned_a <= aligned_a;
                aligned_b <= aligned_b;
                sum_sign <= signa;
            end else if (signb == 0) begin
                aligned_a <= aligned_b;
                aligned_b <= -aligned_a;
                sum_sign <= signb;
            end else begin
                aligned_a <= aligned_a;
                aligned_b <= -aligned_b;
                sum_sign <= signa;
            end
            state <= STATE_SUM;
        end else if (state == STATE_SUM) begin
            if (add_result[FRACTION+2]) begin
                sign <= 1;
                result <= -add_result;
            end else begin
                sign <= sum_sign;
                result <= add_result;
            end
            state <= STATE_NORM;
        end else if (state == STATE_NORM) begin
            if (result[FRACTION+1] == 1) begin
                result <= {1'b0, result[FRACTION+1:1]};
                exp <= exp + 1;
                state <= STATE_IDLE;
            end else if (result[FRACTION] == 0) begin
                if (result[FRACTION-1:0] == 0) begin
                    result <= 0;
                    exp <= 0;
                    sign <= 0;
                    state <= STATE_IDLE;
                end else begin
                    result <= {result[FRACTION:0], 1'b0};
                    exp <= exp - 1;
                end
            end else begin
                state <= STATE_IDLE;
            end
        end
    end

    assign c_out = {sign, exp, result[FRACTION-1:0]};
    assign ready_out = state == STATE_IDLE;

endmodule

module sp_negF(a_in, b_out);

    parameter WIDTH = 32;

    input wire [WIDTH-1:0] a_in;
    output wire [WIDTH-1:0] b_out;

    assign b_out = {~a_in[WIDTH-1], a_in[WIDTH-2:0]};

endmodule

module sp_subF(clk, start, a_in, b_in, c_out, ready_out);

    parameter WIDTH = 32;
    parameter EXPONENT = 8;
    parameter FRACTION = 23;

    input wire clk;
    input wire start;
    input wire [WIDTH-1:0] a_in;
    input wire [WIDTH-1:0] b_in;
    output wire [WIDTH-1:0] c_out;
    output wire ready_out;

    wire [WIDTH-1:0] b;

    sp_negF #(.WIDTH(WIDTH)) neg(.a_in(b_in), .b_out(b));
    sp_addF #(.WIDTH(WIDTH), .EXPONENT(EXPONENT), .FRACTION(FRACTION))
        add(
            .clk(clk),
            .start(start),
            .a_in(a_in),
            .b_in(b),
            .c_out(c_out),
            .ready_out(ready_out));

endmodule

module sp_mulF(clk, start, a_in, b_in, c_out, ready_out);

    parameter WIDTH = 32;
    parameter EXPONENT = 8;
    parameter FRACTION = 23;

    input wire clk;
    input wire start;
    input wire [WIDTH-1:0] a_in;
    input wire [WIDTH-1:0] b_in;
    output wire [WIDTH-1:0] c_out;
    output wire ready_out;

    localparam STATE_IDLE    = 0;
    localparam STATE_SMULT  = 1;
    localparam STATE_MULT    = 2;
    localparam STATE_NORM    = 3;

    reg [1:0] state;
    reg sign;
    reg [EXPONENT-1:0] exp;
    reg [FRACTION*2+1:0] product;

    wire mul_ready;
    wire start_mul = state == STATE_SMULT;
    wire [FRACTION*2+1:0] mul_result;

    reg [WIDTH-1:0] a;
    reg [WIDTH-1:0] b;
    wire signa = a[WIDTH-1];
    wire signb = b[WIDTH-1];
    wire [EXPONENT-1:0] expa = a[WIDTH-2:WIDTH-1-EXPONENT];
    wire [EXPONENT-1:0] expb = b[WIDTH-2:WIDTH-1-EXPONENT];
    wire [FRACTION:0] fraca = expa == 0 ? 0 : {1'b1, a[FRACTION-1:0]};
    wire [FRACTION:0] fracb = expb == 0 ? 0 : {1'b1, b[FRACTION-1:0]};

    always @(posedge clk) begin
        if (start) begin
            a <= a_in;
            b <= b_in;
            state <= STATE_SMULT;
        end else if (state == STATE_SMULT) begin
            sign <= signa ^ signb;
            exp  <= expa + expb;
            state <= STATE_MULT;
        end else if (state == STATE_MULT) begin
            if (mul_ready) begin
                product <= mul_result;
                exp <= exp - ((1 << (EXPONENT - 1)) - 2);
                state <= STATE_NORM;
            end
        end else if (state == STATE_NORM) begin
            if (product[FRACTION*2+1] == 0) begin
                if (product[FRACTION*2:0] == 0) begin
                    product <= 0;
                    exp <= 0;
                    sign <= 0;
                end else begin
                    product <= {product[FRACTION*2:0], 1'b0};
                    exp <= exp - 1;
                end
                state <= STATE_IDLE;
            end else begin
                state <= STATE_IDLE;
            end
        end
    end

    sp_mul_plat #(.WIDTH(FRACTION+1), .OUTPUT_WIDTH((FRACTION+1)*2))
        mul(clk, start_mul, fraca, fracb, mul_result, mul_ready);

    assign c_out = {sign, exp, product[FRACTION*2:FRACTION+1]};
    assign ready_out = state == STATE_IDLE;

endmodule

module sp_divF(clk, start, a_in, b_in, c_out, ready_out);

    parameter WIDTH = 32;
    parameter EXPONENT = 8;
    parameter FRACTION = 23;

    input wire clk;
    input wire start;
    input wire [WIDTH-1:0] a_in;
    input wire [WIDTH-1:0] b_in;
    output wire [WIDTH-1:0] c_out;
    output wire ready_out;

    reg [WIDTH-1:0] a;
    reg [WIDTH-1:0] b;

    wire signa = a[WIDTH-1];
    wire signb = b[WIDTH-1];
    wire [EXPONENT-1:0] expa = a[WIDTH-2:WIDTH-1-EXPONENT];
    wire [EXPONENT-1:0] expb = b[WIDTH-2:WIDTH-1-EXPONENT];
    wire [FRACTION:0] fraca = {1'b1, a[FRACTION-1:0]};
    wire [FRACTION:0] fracb = {1'b1, b[FRACTION-1:0]};
    wire c = fraca >= fracb;
    reg [FRACTION+1:0] q;
    reg [FRACTION+1:0] r;
    reg signed [FRACTION+2:0] t;
    reg sign;
    reg [EXPONENT-1:0] exp;
    reg [7:0] count;
    reg load_t;
    reg [FRACTION:0] frac;

    wire [WIDTH-1:0] sub1_a = (c ? fraca : (fraca << 1));
    wire [WIDTH-1:0] sub1_b = fracb;
    wire [WIDTH-1:0] sub1_result;
    sp_subI #(.WIDTH(WIDTH)) sub1(sub1_a, sub1_b, sub1_result);

    wire [WIDTH-1:0] sub2_a = {r[FRACTION+1:0], 1'b0};
    wire [WIDTH-1:0] sub2_b = fracb;
    wire [WIDTH-1:0] sub2_result;
    sp_subI #(.WIDTH(WIDTH)) sub2(sub2_a, sub2_b, sub2_result);

    always @(posedge clk) begin
        if (start) begin
            a <= a_in;
            b <= b_in;
            count <= FRACTION + 2;
        end else if (count == FRACTION + 2) begin
            if (expa == 0) begin
                exp <= 0;
                q <= 0;
                sign <= 0;
                count <= 0;
            end else begin
                r <= sub1_result;
                sign <= signa ^ signb;
                exp <= expa - expb + 125 + c;
                q <= 1;
                count <= count - 1;
                load_t <= 1;
            end
        end else if (load_t) begin
            t <= sub2_result;
            frac <= (q >> 1) + q[0];
            load_t <= 0;
        end else if (!ready_out) begin
            if (t[FRACTION+1]) begin
                q <= {q[FRACTION:0], 1'b0};
                r <= {r[FRACTION:0], 1'b0};
            end else begin
                q <= {q[FRACTION:0], 1'b1};
                r <= t[FRACTION+1:0];
            end
            count <= count - 1;
            load_t <= 1;
        end
    end

    assign c_out = ({sign, exp} << FRACTION) + frac;
    assign ready_out = (count == 0) & ~load_t;

endmodule

module sp_sqrtF(clk, start, a_in, b_out, ready_out);

    parameter WIDTH = 32;
    parameter EXPONENT = 8;
    parameter FRACTION = 23;

    input wire clk;
    input wire start;
    input wire [WIDTH-1:0] a_in;
    output wire [WIDTH-1:0] b_out;
    output wire ready_out;

    reg [7:0] count;

    wire [EXPONENT-1:0] expa = a_in[WIDTH-2:WIDTH-1-EXPONENT];
    wire [FRACTION:0] fraca = expa == 0 ? 0 : {1'b1, a_in[FRACTION-1:0]};
    wire c = (expa - 9) & 1;

    localparam IW = 32;

    reg [IW-1:0] q;
    reg [IW-1:0] s;
    reg [IW-1:0] r;
    reg [IW-1:0] t;
    wire [FRACTION:0] q_out = (q >> 1) | (q & 1);
    reg [EXPONENT-1:0] exp;

    reg load_t;

    wire [WIDTH-1:0] sub1_a = r << 1;
    wire [WIDTH-1:0] sub1_b = q << 1;
    wire [WIDTH-1:0] sub1_result;
    sp_subI #(.WIDTH(WIDTH)) sub1(sub1_a, sub1_b, sub1_result);

    wire [WIDTH-1:0] sub2_a = sub1_result;
    wire [WIDTH-1:0] sub2_b = s;
    wire [WIDTH-1:0] sub2_result;
    sp_subI #(.WIDTH(WIDTH)) sub2(sub2_a, sub2_b, sub2_result);

    wire [WIDTH-1:0] add_a = q;
    wire [WIDTH-1:0] add_b = s;
    wire [WIDTH-1:0] add_result;
    sp_addI #(.WIDTH(WIDTH)) add1(add_a, add_b, add_result);

    always @(posedge clk) begin
        if (start) begin
            q <= 1 << (FRACTION + 1);
            s <= 1 << FRACTION;
            r <= (fraca << (c + 1)) - (1 << 24);
            exp <= (expa + 125) >> 1;
            count <= FRACTION + 1;
            load_t <= 1;
        end else if (count > 0 && load_t) begin
            t <= sub2_result;
            load_t <= 0;
        end else if (count > 0) begin
            s <= {1'b0, s[IW-1:1]};
            if (t[IW-1]) begin
                r <= {r[IW-2:0], 1'b0};
            end else begin
                q <= add_result;
                r <= t;
            end
            if (expa == 0) begin
                exp <= 0;
                q <= 0;
                count <= 0;
            end else begin
                count <= count - 1;
            end
            load_t <= 1;
        end
    end

    assign ready_out = count == 0;
    assign b_out = (exp << FRACTION) + q_out;

endmodule

//////////////////////////////////////////////////////////////////////////////
// BINARY32 float operations.
//////////////////////////////////////////////////////////////////////////////

module sp_itof32(clk, start, a_in, b_out, ready_out);
    parameter WIDTH = 32;
    input wire clk;
    input wire start;
    input wire [WIDTH-1:0] a_in;
    output wire [WIDTH-1:0] b_out;
    output wire ready_out;
    sp_itof #(.WIDTH(WIDTH), .EXPONENT(8), .FRACTION(23))
        itof(clk, start, a_in, b_out, ready_out);
endmodule

module sp_addF32(clk, start, a_in, b_in, c_out, ready_out);
    parameter WIDTH = 32;
    input wire clk;
    input wire start;
    input wire [WIDTH-1:0] a_in;
    input wire [WIDTH-1:0] b_in;
    output wire [WIDTH-1:0] c_out;
    output wire ready_out;
    sp_addF #(.WIDTH(WIDTH), .EXPONENT(8), .FRACTION(23))
        add(clk, start, a_in, b_in, c_out, ready_out);
endmodule

module sp_negF32(a_in, b_out);
    parameter WIDTH = 32;
    input wire [WIDTH-1:0] a_in;
    output wire [WIDTH-1:0] b_out;
    sp_negF #(.WIDTH(WIDTH)) neg(a_in, b_out);
endmodule

module sp_subF32(clk, start, a_in, b_in, c_out, ready_out);
    parameter WIDTH = 32;
    input wire clk;
    input wire start;
    input wire [WIDTH-1:0] a_in;
    input wire [WIDTH-1:0] b_in;
    output wire [WIDTH-1:0] c_out;
    output wire ready_out;
    sp_subF #(.WIDTH(WIDTH), .EXPONENT(8), .FRACTION(23))
        sub(clk, start, a_in, b_in, c_out, ready_out);
endmodule

module sp_mulF32(clk, start, a_in, b_in, c_out, ready_out);
    parameter WIDTH = 32;
    input wire clk;
    input wire start;
    input wire [WIDTH-1:0] a_in;
    input wire [WIDTH-1:0] b_in;
    output wire [WIDTH-1:0] c_out;
    output wire ready_out;
    sp_mulF #(.WIDTH(WIDTH), .EXPONENT(8), .FRACTION(23))
        mul(clk, start, a_in, b_in, c_out, ready_out);
endmodule

module sp_divF32(clk, start, a_in, b_in, c_out, ready_out);
    parameter WIDTH = 32;
    input wire clk;
    input wire start;
    input wire [WIDTH-1:0] a_in;
    input wire [WIDTH-1:0] b_in;
    output wire [WIDTH-1:0] c_out;
    output wire ready_out;
    sp_divF #(.WIDTH(WIDTH), .EXPONENT(8), .FRACTION(23))
        div(clk, start, a_in, b_in, c_out, ready_out);
endmodule

module sp_sqrtF32(clk, start, a_in, b_out, ready_out);
    parameter WIDTH = 32;
    input wire clk;
    input wire start;
    input wire [WIDTH-1:0] a_in;
    output wire [WIDTH-1:0] b_out;
    output wire ready_out;
    sp_sqrtF #(.WIDTH(WIDTH), .EXPONENT(8), .FRACTION(23))
        sqrt(clk, start, a_in, b_out, ready_out);
endmodule

//////////////////////////////////////////////////////////////////////////////
// BINARY64 operations.
//////////////////////////////////////////////////////////////////////////////

module sp_itof64(clk, start, a_in, b_out, ready_out);
    parameter WIDTH = 64;
    input wire clk;
    input wire start;
    input wire [WIDTH-1:0] a_in;
    output wire [WIDTH-1:0] b_out;
    output wire ready_out;
    sp_itof #(.WIDTH(WIDTH), .EXPONENT(11), .FRACTION(52))
        itof(clk, start, a_in, b_out, ready_out);
endmodule

module sp_addF64(clk, start, a_in, b_in, c_out, ready_out);
    parameter WIDTH = 64;
    input wire clk;
    input wire start;
    input wire [WIDTH-1:0] a_in;
    input wire [WIDTH-1:0] b_in;
    output wire [WIDTH-1:0] c_out;
    output wire ready_out;
    sp_addF #(.WIDTH(WIDTH), .EXPONENT(11), .FRACTION(52))
        add(clk, start, a_in, b_in, c_out, ready_out);
endmodule

module sp_negF64(a_in, b_out);
    parameter WIDTH = 64;
    input wire [WIDTH-1:0] a_in;
    output wire [WIDTH-1:0] b_out;
    sp_negF #(.WIDTH(WIDTH)) neg(.a_in(a_in), .b_out(b_out));
endmodule

module sp_subF64(clk, start, a_in, b_in, c_out, ready_out);
    parameter WIDTH = 64;
    input wire clk;
    input wire start;
    input wire [WIDTH-1:0] a_in;
    input wire [WIDTH-1:0] b_in;
    output wire [WIDTH-1:0] c_out;
    output wire ready_out;
    sp_subF #(.WIDTH(WIDTH), .EXPONENT(11), .FRACTION(52))
        sub(clk, start, a_in, b_in, c_out, ready_out);
endmodule

module sp_mulF64(clk, start, a_in, b_in, c_out, ready_out);
    parameter WIDTH = 64;
    input wire clk;
    input wire start;
    input wire [WIDTH-1:0] a_in;
    input wire [WIDTH-1:0] b_in;
    output wire [WIDTH-1:0] c_out;
    output wire ready_out;
    sp_mulF #(.WIDTH(WIDTH), .EXPONENT(11), .FRACTION(52))
        mul(clk, start, a_in, b_in, c_out, ready_out);
endmodule

module sp_divF64(clk, start, a_in, b_in, c_out, ready_out);
    parameter WIDTH = 64;
    input wire clk;
    input wire start;
    input wire [WIDTH-1:0] a_in;
    input wire [WIDTH-1:0] b_in;
    output wire [WIDTH-1:0] c_out;
    output wire ready_out;
    sp_divF #(.WIDTH(WIDTH), .EXPONENT(11), .FRACTION(52))
        div(
            .clk(clk),
            .start(start),
            .a_in(a_in),
            .b_in(b_in),
            .c_out(c_out),
            .ready_out(ready_out));
endmodule

module sp_sqrtF64(clk, start, a_in, b_out, ready_out);
    parameter WIDTH = 64;
    input wire clk;
    input wire start;
    input wire [WIDTH-1:0] a_in;
    output wire [WIDTH-1:0] b_out;
    output wire ready_out;
    sp_sqrtF #(.WIDTH(WIDTH), .EXPONENT(11), .FRACTION(52))
        sqrt(
            .clk(clk),
            .start(start),
            .a_in(a_in),
            .b_out(b_out),
            .ready_out(ready_out));
endmodule
