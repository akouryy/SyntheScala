`default_nettype none
module top();

  reg clk, r_enable, controlArr;
  wire[0:0] init_i = 1'd0;
  wire controlArrWEnable_a = 1'd0;
  wire[0:0] controlArrAddr_a;
  wire controlArrRData_a;
  wire controlArrWData_a;
  wire w_enable;
  wire[1:0] result;

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
