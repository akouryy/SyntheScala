`default_nettype none
module top();

  reg clk, r_enable, controlArr;
  wire[5:0] init_n_t_a = 6'd40;
  wire[31:0] init_a_t_a = 32'd1;
  wire[31:0] init_b_t_a = 32'd0;
  wire w_enable;
  wire[31:0] result;

  main main(.*);

  longint clkcnt = 0;
  initial begin
    $dumpfile("fib.srp.vcd");
    $dumpvars(0, main);

    clk = 0;
    controlArr = 0;
    forever #2 begin
      clk <= ~clk;
      if(clk) clkcnt += 1;
    end
  end

  initial begin
    r_enable = 0;
    #5 r_enable = 1;
    #4 r_enable = 0;
  end

  always @(posedge w_enable) begin
    $write("fib(%d) = 165580141, result = %d\n", init_n_t_a, result);
    $write("time = %d\n", clkcnt);
    $finish;
  end

endmodule
