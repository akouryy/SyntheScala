module top();

  reg clk, r_enable;
  wire[63:0] init_i = 64'd0;
  wire[63:0] init_acc = 64'd0;
  wire w_enable;
  wire[63:0] result;

  main main(clk, r_enable, init_i, init_acc, w_enable, result);

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
    $write("result: accumulate = %d\n", result);
    $finish;
  end

endmodule
