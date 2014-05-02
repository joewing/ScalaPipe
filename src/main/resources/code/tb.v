
`include "scalapipe.v"
`include "simulation.v"

module tb;

    integer seed = 5;
    real E = 0.001;
    integer max_iterations = 100;

    integer i;
    real ra, rb;
    real radd_result, rsub_result;
    real rmul_result, rdiv_result;
    real rsqrt_result;
    reg signed [31:0] a;
    reg signed [31:0] b;
    reg clk = 0;
    reg rst = 1;

    wire [31:0] add_result;
    wire add_ready;
    wire [31:0] sub_result;
    wire sub_ready;
    wire [31:0] mul_result;
    wire mul_ready;
    wire [31:0] div_result;
    wire div_ready;
    wire [31:0] sqrt_result;
    wire sqrt_ready;

    wire signed [31:0] imul_result;
    wire imul_ready;
    wire signed [31:0] sdiv_result;
    wire sdiv_ready;

    wire ready = add_ready & sub_ready & mul_ready & div_ready & sqrt_ready;
    wire iready = imul_ready & sdiv_ready;

    reg [31:0] avalues[0:5];
    reg [31:0] bvalues[0:5];

    function [63:0] f2d;
        input [31:0] x;
        begin
            f2d = {x[31],
                   (x[30:23] == 0) ? 11'b0 : ({3'b0,x[30:23]} + 11'd896),
                   x[22:0], 29'b0};
        end
    endfunction

    function [7:0] expd2f;
        input [10:0] x;
        begin
            expd2f = x[7:0];
        end
    endfunction

    function [31:0] d2f;
        input [63:0] x;
        begin
            d2f = {x[63],
                   (x[62:52] == 0) ? 8'b0 : expd2f(x[62:52] - 11'd896),
                   x[51:29]};
        end
    endfunction

    function real fasd;
        input real x;
        begin
            fasd = $bitstoreal(f2d(d2f($realtobits(x))));
        end
    endfunction

    initial begin

        $dumpvars;

        a = 0;
        b = 0;

        // Test floating point functions.
        for (i = 0; i < max_iterations; i = i + 1) begin

            ra = $itor($random(seed)) / $itor($random(seed));
            rb = $itor($random(seed)) / $itor($random(seed));
            a = d2f($realtobits(ra));
            b = d2f($realtobits(rb));

            rst <= 1;
            #10 clk <= 1; #10 clk <= 0;
            rst <= 0;
            while (!ready) begin
                #10 clk <= 1; #10 clk <= 0;
            end
            radd_result = $bitstoreal(f2d(add_result));
            rsub_result = $bitstoreal(f2d(sub_result));
            rmul_result = $bitstoreal(f2d(mul_result));
            rdiv_result = $bitstoreal(f2d(div_result));
            rsqrt_result = $bitstoreal(f2d(sqrt_result));
            $display("%f + %f = %f (%f)",
                        $bitstoreal(f2d(a)), $bitstoreal(f2d(b)),
                        radd_result, ra + rb);
            $display("%f - %f = %f (%f)",
                        $bitstoreal(f2d(a)), $bitstoreal(f2d(b)),
                        rsub_result, ra - rb);
            $display("%f * %f = %f (%f)",
                        $bitstoreal(f2d(a)), $bitstoreal(f2d(b)),
                        rmul_result, ra * rb);
            $display("%f / %f = %f (%f)",
                        $bitstoreal(f2d(a)), $bitstoreal(f2d(b)),
                        rdiv_result, ra / rb);
            $display("sqrt(%f) = %f (%f)",
                        $bitstoreal(f2d(a)),
                        rsqrt_result, fasd($sqrt(ra)));
            if ($abs(radd_result - (ra + rb)) > $abs(E * radd_result)) begin
                $display("add error");
                $stop;
            end
            if ($abs(rsub_result - (ra - rb)) > $abs(E * rsub_result)) begin
                $display("sub error");
                $stop;
            end
            if ($abs(rmul_result - (ra * rb)) > $abs(E * rmul_result)) begin
                $display("mul error");
                $stop;
            end
            if ($abs(rdiv_result - (ra / rb)) > $abs(E * rdiv_result)) begin
                $display("div error");
                $stop;
            end
            if ($abs(rsqrt_result - $sqrt(ra)) > $abs(E * rsqrt_result)) begin
                $display("sqrt error");
                $stop;
            end
        end

        // Test integer functions.
        for (i = 0; i < 1; i = i + 1) begin

            a = -1234124124; //$random(seed);
            b = 134123; //$random(seed);

            rst <= 1;
            #10 clk <= 1; #10 clk <= 0;
            rst <= 0;
            while (!iready) begin
                #10 clk <= 1; #10 clk <= 0;
            end
            $display("%d * %d = %d (%d)", a, b, imul_result, a * b);
            $display("%d / %d = %d (%d)", a, b, sdiv_result, a / b);
            if (imul_result != (a * b)) begin
                $display("mul error");
                $stop;
            end
            if (sdiv_result != (a / b)) begin
                $display("div error");
                $stop;
            end
        end

    end

    sp_addF32 add(clk, rst, a, b, add_result, add_ready);
    sp_subF32 sub(clk, rst, a, b, sub_result, sub_ready);
    sp_mulF32 mul(clk, rst, a, b, mul_result, mul_ready);
    sp_divF32 div(clk, rst, a, b, div_result, div_ready);
    sp_sqrtF32 sqrt(clk, rst, a, sqrt_result, sqrt_ready);

    sp_mulI #(.WIDTH(32)) imul(clk, rst, a, b, imul_result, imul_ready);
    sp_divS #(.WIDTH(32)) sdiv(clk, rst, a, b, sdiv_result, sdiv_ready);

endmodule
