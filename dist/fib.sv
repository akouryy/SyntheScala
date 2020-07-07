`default_nettype none
module main (
  input wire clk, r_enable, controlArr,
  input wire[5:0] init_n,
  input wire[31:0] init_a,
  input wire[31:0] init_b,
  output reg w_enable,
  output reg[31:0] result
);
  reg[2:0] stateR;
  reg[2:0] linkreg;
  reg[63:0] reg0;
  wire[63:0] stationReg0;
  reg[63:0] reg1;
  wire[63:0] stationReg1;
  reg[63:0] reg2;
  wire[63:0] stationReg2;
  reg[63:0] reg3;
  wire[63:0] stationReg3;

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
    stateR == 3'd2 ? reg0[5:0] :
    'x;
  assign in1_Bin1 =
    stateR == 3'd2 ? 1'd0 :
    'x;
  assign in0_Bin2 =
    stateR == 3'd4 ? reg0[5:0] :
    'x;
  assign in1_Bin2 =
    stateR == 3'd4 ? 1'd1 :
    'x;
  assign in0_Bin0 =
    stateR == 3'd1 ? reg0[31:0] :
    stateR == 3'd4 ? reg1[31:0] :
    'x;
  assign in1_Bin0 =
    stateR == 3'd1 ? 32'd0 :
    stateR == 3'd4 ? reg2[31:0] :
    'x;

  assign stationReg0 =
    stateR == 3'd1 ? {32'd0, out0_Bin0} :
    stateR == 3'd3 ? reg1 :
    stateR == 3'd4 ? {32'd0, out0_Bin0} :
    stateR == 3'd5 ? reg2 :
    reg0;
  assign stationReg1 =
    stateR == 3'd5 ? reg0 :
    reg1;
  assign stationReg2 =
    stateR == 3'd4 ? {58'd0, out0_Bin2} :
    stateR == 3'd5 ? reg1 :
    reg2;
  assign stationReg3 =
    stateR == 3'd2 ? {63'd0, out0_Bin1} :
    reg3;

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
        3'd0: stateR <= 3'd1;
        3'd1: stateR <= 3'd2;
        3'd2: stateR <= (stationReg3) ? 3'd3 : 3'd4;
        3'd3: stateR <= linkreg;
        3'd4: stateR <= 3'd5;
        3'd5: stateR <= 3'd0;
      endcase
      reg0 <= stationReg0;
      reg1 <= stationReg1;
      reg2 <= stationReg2;
      reg3 <= stationReg3;
    end
  end
endmodule // main
`default_nettype wire
