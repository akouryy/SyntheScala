`default_nettype none
module top();

  reg clk, r_enable, controlArr;
  wire[63:0] init_a = 64'd123;
  wire[63:0] init_b = 64'd234;
  wire[63:0] init_c = 64'd345;
  wire[63:0] init_d = 64'd456;
  wire[63:0] init_e = 64'd567;
  wire[63:0] init_f = 64'd678;
  wire[63:0] init_g = 64'd789;
  wire w_enable;
  wire[63:0] result;

  main main(.*);

  initial begin
    clk = 0;
    controlArr = 0;
    forever #10 clk = ~clk;
  end

  initial begin
    r_enable = 0;
    #25 r_enable = 1;
    #25 r_enable = 0;
  end

  always @(posedge w_enable) begin
    $write("result = %d\n", result);
    $finish;
  end

endmodule
