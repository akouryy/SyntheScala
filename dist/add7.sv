`default_nettype none
module main (
  input wire clk, r_enable, controlArr,
  input wire[9:0] init_a,
  input wire[9:0] init_b,
  input wire[9:0] init_c,
  input wire[9:0] init_d,
  input wire[12:0] init_e,
  input wire[9:0] init_f,
  input wire[12:0] init_g,
  output reg w_enable,
  output reg[12:0] result
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
  reg[63:0] reg4;
  wire[63:0] stationReg4;
  reg[63:0] reg5;
  wire[63:0] stationReg5;
  reg[63:0] reg6;
  wire[63:0] stationReg6;

  wire[12:0] in0_Bin0;
  wire[12:0] in1_Bin0;
  wire[12:0] out0_Bin0 = in0_Bin0 + in1_Bin0;
  wire[9:0] in0_Bin1;
  wire[12:0] in1_Bin1;
  wire[12:0] out0_Bin1 = in0_Bin1 + in1_Bin1;

  assign in0_Bin1 =
    stateR == 3'd1 ? reg3[9:0] :
    'x;
  assign in1_Bin1 =
    stateR == 3'd1 ? reg4[12:0] :
    'x;
  assign in0_Bin0 =
    stateR == 3'd1 ? reg5[12:0] :
    stateR == 3'd2 ? reg4[12:0] :
    stateR == 3'd3 ? reg2[12:0] :
    stateR == 3'd4 ? reg1[12:0] :
    stateR == 3'd5 ? reg0[12:0] :
    'x;
  assign in1_Bin0 =
    stateR == 3'd1 ? reg6[12:0] :
    stateR == 3'd2 ? reg3[12:0] :
    stateR == 3'd3 ? reg3[12:0] :
    stateR == 3'd4 ? reg2[12:0] :
    stateR == 3'd5 ? reg1[12:0] :
    'x;

  assign stationReg0 =
    stateR == 3'd5 ? {51'd0, out0_Bin0} :
    stateR == 3'd6 ? reg0 :
    reg0;
  assign stationReg1 =
    stateR == 3'd4 ? {51'd0, out0_Bin0} :
    reg1;
  assign stationReg2 =
    stateR == 3'd3 ? {51'd0, out0_Bin0} :
    reg2;
  assign stationReg3 =
    stateR == 3'd1 ? {51'd0, out0_Bin0} :
    stateR == 3'd2 ? {51'd0, out0_Bin0} :
    reg3;
  assign stationReg4 =
    stateR == 3'd1 ? {51'd0, out0_Bin1} :
    reg4;

  always @(posedge clk) begin
    if(r_enable) begin
      stateR <= '0;
      linkreg <= '1;
      w_enable <= 1'd0;
      reg0 <= {54'd0, init_a};
      reg1 <= {54'd0, init_b};
      reg2 <= {54'd0, init_c};
      reg3 <= {54'd0, init_d};
      reg4 <= {51'd0, init_e};
      reg5 <= {54'd0, init_f};
      reg6 <= {51'd0, init_g};
    end else begin
      case(stateR)
        '1: begin
          w_enable <= 1'd1;
          result <= reg0[12:0];
        end
        3'd0: stateR <= 3'd1;
        3'd1: stateR <= 3'd2;
        3'd2: stateR <= 3'd3;
        3'd3: stateR <= 3'd4;
        3'd4: stateR <= 3'd5;
        3'd5: stateR <= 3'd6;
        3'd6: stateR <= linkreg;
      endcase
      reg0 <= stationReg0;
      reg1 <= stationReg1;
      reg2 <= stationReg2;
      reg3 <= stationReg3;
      reg4 <= stationReg4;
    end
  end
endmodule // main
`default_nettype wire
