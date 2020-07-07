`default_nettype none
module main (
  input wire clk, r_enable, controlArr,
  input wire[5:0] init_n,
  input wire[31:0] init_a,
  input wire[31:0] init_b,
  output reg w_enable,
  output reg[31:0] result
);
  reg[3:0] stateR;
  reg[3:0] linkreg;
  reg[63:0] reg0;
  reg[63:0] reg1;
  reg[63:0] reg2;
  reg[63:0] reg3;
  reg[63:0] reg4;

  wire[31:0] in0_Bin0;
  wire[31:0] in1_Bin0;
  wire[31:0] out0_Bin0 = in0_Bin0 + in1_Bin0;
  wire[5:0] in0_Bin1;
  wire in1_Bin1;
  wire out0_Bin1 = in0_Bin1 == in1_Bin1;
  wire[5:0] in0_Bin2;
  wire in1_Bin2;
  wire[5:0] out0_Bin2 = in0_Bin2 - in1_Bin2;


  assign in0_Bin1 =
    stateR == 4'd3 ? reg0[5:0] :
    'x;
  assign in1_Bin1 =
    stateR == 4'd3 ? reg3[0:0] :
    'x;
  assign in0_Bin2 =
    stateR == 4'd6 ? reg0[5:0] :
    'x;
  assign in1_Bin2 =
    stateR == 4'd6 ? reg2[0:0] :
    'x;
  assign in0_Bin0 =
    stateR == 4'd2 ? reg0[31:0] :
    stateR == 4'd5 ? reg1[31:0] :
    'x;
  assign in1_Bin0 =
    stateR == 4'd2 ? reg4[31:0] :
    stateR == 4'd5 ? reg2[31:0] :
    'x;


  always @(posedge clk) begin
    if(r_enable) begin
      stateR <= '0;
      linkreg <= '1;
      w_enable <= 1'd0;
      reg0 <= {58'd0, init_n};
      reg1 <= {32'd0, init_a};
      reg2 <= {32'd0, init_b};
    end else begin
      case(stateR)
        '1: begin
          w_enable <= 1'd1;
          result <= reg0[31:0];
        end
        4'd0: stateR <= 4'd1;
        4'd1: stateR <= 4'd2;
        4'd2: stateR <= 4'd3;
        4'd3: stateR <= (out0_Bin1) ? 4'd4 : 4'd5;
        4'd4: stateR <= linkreg;
        4'd5: stateR <= 4'd6;
        4'd6: stateR <= 4'd7;
        4'd7: stateR <= 4'd0;
      endcase
      case(stateR)
        4'd2: reg0 <= {32'd0, out0_Bin0};
        4'd4: reg0 <= reg1;
        4'd6: reg0 <= {58'd0, out0_Bin2};
        4'd7: reg0 <= reg0;
      endcase
      case(stateR)
        4'd7: reg1 <= reg3;
      endcase
      case(stateR)
        4'd5: reg2 <= 64'd1;
        4'd7: reg2 <= reg1;
      endcase
      case(stateR)
        4'd1: reg3 <= 64'd0;
        4'd3: reg3 <= {63'd0, out0_Bin1};
        4'd5: reg3 <= {32'd0, out0_Bin0};
      endcase
      case(stateR)
        4'd1: reg4 <= 64'd0;
      endcase
    end
  end
endmodule // main
`default_nettype wire
