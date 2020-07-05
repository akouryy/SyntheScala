module top();

  reg clk, r_enable, controlArr;
  wire[63:0] init_i = 64'd0;
  wire[63:0] init_acc = 64'd0;
  wire w_enable;
  wire[0:0] result;

  reg controlArrWEnable_a;
  reg[9:0] controlArrAddr_a;
  wire signed[63:0] controlArrRData_a;
  reg signed[63:0] controlArrWData_a;

  main main(.*);

  initial begin
    $dumpfile("accumulate.vcd");
    $dumpvars(0, main);

    clk <= 0;
    forever #2 clk <= ~clk;
  end

  longint da, ans[0:999], acc;
  integer i;
  initial begin
    acc = 0;
    controlArr = 1;
    #1
    for(i = 0; i < 1000; i += 1) begin
      controlArrWEnable_a <= 1;
      controlArrAddr_a <= i;
      da = $urandom_range(0, (1 << 31) - 1);
      controlArrWData_a <= da;
      acc += da;
      ans[i] = acc;
      #4 ;
    end

    i = 0;
    controlArr <= 0;
    r_enable <= 1;
    #4
    r_enable <= 0;
  end

  always @(posedge clk) begin
    if(w_enable) begin
      controlArr <= 1;
      if(i < 1000) begin
        controlArrWEnable_a <= 0;
        controlArrAddr_a <= i;
      end
      if(1 <= i && i <= 1000) begin
        $write("ans = %d, result = %d\n", ans[i - 1], controlArrRData_a);
        if(ans[i - 1] != controlArrRData_a) begin
          $write("wrong");
          $finish;
        end
      end
      if(i == 1000) $finish;
      i += 1;
    end
  end

endmodule