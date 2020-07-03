module top();

  reg clk, r_enable;
  wire[31:0] init_a = 32'd123;
  wire[31:0] init_b = 32'd234;
  wire[31:0] init_c = 32'd345;
  wire[31:0] init_d = 32'd456;
  wire[31:0] init_e = 32'd567;
  wire[31:0] init_f = 32'd678;
  wire[31:0] init_g = 32'd789;
  wire w_enable;
  wire[31:0] result;

  main main(clk, r_enable, init_a, init_b, init_c, init_d, init_e, init_f, init_g, w_enable, result);

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
    $write("result = %d\n", result);
    $finish;
  end

endmodule
