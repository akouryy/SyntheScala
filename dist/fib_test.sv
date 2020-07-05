`default_nettype none
module top();

  reg clk, r_enable, controlArr;
  wire[63:0] init_n = 64'd40;
  wire[63:0] init_a = 64'd1;
  wire[63:0] init_b = 64'd0;
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
    $write("result: fib(%d) = %d\n", init_n, result);
    $finish;
  end

endmodule
