
module ap_addI(a_in, b_in, c_out);

   parameter WIDTH = 32;
   input wire [WIDTH-1:0] a_in;
   input wire [WIDTH-1:0] b_in;
   output wire [WIDTH-1:0] c_out;

   generate
      if (WIDTH == 8) begin
         a_add8 add(a_in, b_in, c_out);
      end else if (WIDTH == 16) begin
         a_add16 add(a_in, b_in, c_out);
      end else if (WIDTH == 32) begin
         a_add32 add(a_in, b_in, c_out);
      end else if (WIDTH == 64) begin
         a_add64 add(a_in, b_in, c_out);
      end
   endgenerate

endmodule

module ap_subI(a_in, b_in, c_out);

   parameter WIDTH = 32;
   input wire [WIDTH-1:0] a_in;
   input wire [WIDTH-1:0] b_in;
   output wire [WIDTH-1:0] c_out;

   generate
      if (WIDTH == 8) begin
         a_sub8 sub(a_in, b_in, c_out);
      end else if (WIDTH == 16) begin
         a_sub16 sub(a_in, b_in, c_out);
      end else if (WIDTH == 32) begin
         a_sub32 sub(a_in, b_in, c_out);
      end else if (WIDTH == 64) begin
         a_sub64 sub(a_in, b_in, c_out);
      end
   endgenerate

endmodule

