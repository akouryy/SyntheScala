`default_nettype none
module top();

  reg clk, r_enable, controlArr;
  wire[0:0] init_i1 = 1'd1, init_i2 = 1'd0;
  wire controlArrWEnable_a = 1'd0;
  wire[0:0] controlArrAddr_a;
  wire controlArrRData_a;
  wire controlArrWData_a;
  wire w_enable1, w_enable2;
  wire[1:0] result1, result2;

  main main1(
    clk, r_enable, controlArr, init_i1, controlArrWEnable_a, controlArrAddr_a,
    controlArrRData_a, controlArrWData_a, w_enable1, result1
  );
  main main2(
    clk, r_enable, controlArr, init_i2, controlArrWEnable_a, controlArrAddr_a,
    controlArrRData_a, controlArrWData_a, w_enable2, result2
  );

  initial begin
    $dumpfile("complexIf.srp.vcd");
    $dumpvars(0, main1);
    $dumpvars(0, main2);

    clk = 0;
    controlArr = 0;
    forever #2 clk = ~clk;
  end

  initial begin
    r_enable = 0;
    #3 r_enable = 1;
    #4 r_enable = 0;
  end

  always @(posedge w_enable1) begin
    $write("ans1 = 3, result1 = %d\n", result1);
  end

  always @(posedge w_enable2) begin
    $write("ans2 = 2, result2 = %d\n", result2);
  end

  always @(negedge clk) begin
    if(w_enable1 && w_enable2) $finish;
  end

endmodule
