`default_nettype none
module top();

  reg clk, r_enable, controlArr;
  wire[63:0] init_i = 64'd1;
  wire w_enable;
  wire[63:0] result;

  main main(.*);

  initial begin
    clk = 0;
    controlArr = 0;
    forever #2 clk = ~clk;
  end

  initial begin
    r_enable = 0;
    #3 r_enable = 1;
    #4 r_enable = 0;
  end

  always @(posedge w_enable) begin
    $write("result = %d\n", result);
    $finish;
  end

endmodule