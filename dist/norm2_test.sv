module top();

  reg clk, r_enable, controlArr;
  wire[63:0] init_i = 64'd0;
  wire[63:0] init_acc = 64'd0;
  wire w_enable;
  wire[63:0] result;

  reg controlArrWEnable_a;
  reg[9:0] controlArrAddr_a;
  wire signed[26:0] controlArrRData_a;
  reg signed[26:0] controlArrWData_a;

  main main(.*);

  initial begin
    $dumpfile("norm2.vcd");
    $dumpvars(0, main);

    clk <= 0;
    forever #2 clk <= ~clk;
  end

  longint ans, da;
  integer i;
  initial begin
    controlArr = 1;
    #1
    for(i = 0; i < 1000; i = i + 1) begin
      controlArrWEnable_a <= 1;
      controlArrAddr_a <= i;
      da = $urandom_range(0, (1 << 26) - 1);
      controlArrWData_a <= da;
      ans += {32'd0, da} * da;
      #4 ;
    end

    controlArr <= 0;
    r_enable <= 1;
    #4
    r_enable <= 0;
  end

  always @(posedge clk) begin
    if(w_enable) begin
      $write("ans = %d, result = %d\n", ans, result);
      $finish;
    end
  end

endmodule
