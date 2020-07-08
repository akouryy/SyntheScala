`default_nettype none
module top();

  reg clk, r_enable, controlArr;
  wire[9:0] init_a = 10'd123;
  wire[9:0] init_b = 10'd234;
  wire[9:0] init_c = 10'd345;
  wire[9:0] init_d = 10'd456;
  wire[12:0] init_e = 13'd567;
  wire[9:0] init_f = 10'd678;
  wire[12:0] init_g = 13'd789;
  wire w_enable;
  wire[12:0] result;

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
    $write("ans = 3192, result = %d\n", result);
    $finish;
  end

endmodule
