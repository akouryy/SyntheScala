module top();

  reg clk, r_enable;
  wire[31:0] init_n = 32'd40;
  wire[31:0] init_a = 32'd1;
  wire[31:0] init_b = 32'd0;
  wire w_enable;
  wire[31:0] result;

  main main(clk, r_enable, init_n, init_a, init_b, w_enable, result);

  initial begin
    clk = 0;
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
