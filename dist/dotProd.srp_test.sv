`default_nettype none
module top();

  reg clk, r_enable, controlArr;
  wire[9:0] init_i_t_a = 10'd0;
  wire signed[63:0] init_acc_t_a = 64'd0;
  wire w_enable;
  wire signed[63:0] result;

  reg controlArrWEnable_a;
  reg[9:0] controlArrAddr_a;
  wire signed[26:0] controlArrRData_a;
  reg signed[26:0] controlArrWData_a;
  reg controlArrWEnable_b;
  reg[9:0] controlArrAddr_b;
  wire signed[26:0] controlArrRData_b;
  reg signed[26:0] controlArrWData_b;

  main main(.*);

  longint clkcnt = 0;
  initial begin
    $dumpfile("dotProd.srp.vcd");
    $dumpvars(0, main);

    clk <= 0;
    forever #2 begin
      clk <= ~clk;
      if(clk) clkcnt += 1;
    end
  end

  longint ans, da, db;
  integer i;
  initial begin
    controlArr <= 1;
    r_enable <= 0;
    #1
    for(i = 0; i < 1000; i = i + 1) begin
      controlArrWEnable_a <= 1;
      controlArrWEnable_b <= 1;
      controlArrAddr_a <= i;
      controlArrAddr_b <= i;
      da = $urandom_range(0, (1 << 27) - 1) - (1 << 26);
      db = $urandom_range(0, (1 << 27) - 1) - (1 << 26);
      controlArrWData_a <= da;
      controlArrWData_b <= db;
      ans += {32'd0, da} * db;
      #4 ;
    end

    controlArr <= 0;
    r_enable <= 1;
    clkcnt = 0;
    #4
    r_enable <= 0;
  end

  always @(posedge clk) begin
    if(w_enable) begin
      $write("ans = %d, result = %d\n", ans, $signed(result));
      $write("time = %d\n", clkcnt);
      $finish;
    end
  end

endmodule
