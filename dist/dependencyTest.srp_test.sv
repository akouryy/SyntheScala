`default_nettype none
module top();

  reg clk, r_enable, controlArr;
  wire[63:0] init_i = 64'hffff_ffff_ffff_fff9;
  wire w_enable;
  wire[63:0] result;

  reg controlArrWEnable_a;
  reg[0:0] controlArrAddr_a;
  wire signed[63:0] controlArrRData_a;
  reg signed[63:0] controlArrWData_a;

  main main(.*);

  initial begin
    $dumpfile("dependencyTest.srp.vcd");
    $dumpvars(0, main);

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
    $write("ans = 21, result = %d\n", $signed(result));
    $finish;
  end

endmodule
