`default_nettype none
module top();

  reg clk, r_enable, controlArr;
  wire[5:0] init_n = 6'd40;
  wire[31:0] init_a = 32'd1;
  wire[31:0] init_b = 32'd0;
  wire w_enable;
  wire[31:0] result;

  main main(.*);

  initial begin
    $dumpfile("fib.vcd");
    $dumpvars(0, main);

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
    $write("fib(%d) = 165580141, result = %d\n", init_n, result);
    $finish;
  end

endmodule
