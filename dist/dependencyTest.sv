`default_nettype none
module main (
  input wire clk, r_enable, controlArr,
  input wire signed[63:0] init_i,
  input wire controlArrWEnable_a,
  input wire[0:0] controlArrAddr_a,
  output wire signed[63:0] controlArrRData_a,
  input wire signed[63:0] controlArrWData_a,
  output reg w_enable,
  output reg signed[63:0] result
);
  reg[4:0] stateR;
  reg[4:0] linkreg;
  reg[63:0] reg0;
  wire[63:0] stationReg0;
  reg[63:0] reg1;
  wire[63:0] stationReg1;
  reg[63:0] reg2;
  wire[63:0] stationReg2;

  wire signed[63:0] in0_Bin0;
  wire signed[0:0] in1_Bin0;
  wire out0_Bin0 = in0_Bin0 >= in1_Bin0;
  wire signed[63:0] in0_Bin1;
  wire signed[63:0] in1_Bin1;
  wire signed[63:0] out0_Bin1 = in0_Bin1 * in1_Bin1;

  wire arrWEnable_a;
  wire arrAddr_a;
  wire signed[63:0] arrRData_a;
  wire signed[63:0] arrWData_a;
  arr_a arr_a(.*);

  assign in0_Bin1 =
    stateR == 5'd10 ? reg0 :
    stateR == 5'd14 ? reg0 :
    'x;
  assign in1_Bin1 =
    stateR == 5'd10 ? $signed(64'd2) :
    stateR == 5'd14 ? (-$signed(64'd3)) :
    'x;
  assign in0_Bin0 =
    stateR == 5'd7 ? reg0 :
    'x;
  assign in1_Bin0 =
    stateR == 5'd7 ? $signed(1'd0) :
    'x;

  assign arrWEnable_a =
    controlArr ? controlArrWEnable_a :
    stateR == 5'd2 ? 1'd1 :
    stateR == 5'd3 ? 1'd1 :
    stateR == 5'd4 ? 1'd1 :
    stateR == 5'd5 ? 1'd0 :
    stateR == 5'd8 ? 1'd0 :
    stateR == 5'd12 ? 1'd0 :
    stateR == 5'd16 ? 1'd1 :
    stateR == 5'd17 ? 1'd0 :
    1'd0;
  assign arrAddr_a =
    controlArr ? controlArrAddr_a :
    stateR == 5'd2 ? reg2[0:0] :
    stateR == 5'd3 ? reg2[0:0] :
    stateR == 5'd4 ? reg2[0:0] :
    stateR == 5'd5 ? reg2[0:0] :
    stateR == 5'd8 ? reg2[0:0] :
    stateR == 5'd12 ? reg2[0:0] :
    stateR == 5'd16 ? reg2[0:0] :
    stateR == 5'd17 ? reg2[0:0] :
    'x;
  assign arrWData_a =
    controlArr ? controlArrWData_a :
    stateR == 5'd2 ? reg0 :
    stateR == 5'd3 ? reg1 :
    stateR == 5'd4 ? reg0 :
    stateR == 5'd16 ? reg0 :
    'x;
  assign controlArrRData_a = controlArr ? arrRData_a : 'x;

  assign stationReg0 =
    stateR == 5'd6 ? arrRData_a :
    stateR == 5'd7 ? {63'd0, out0_Bin0} :
    stateR == 5'd9 ? arrRData_a :
    stateR == 5'd10 ? out0_Bin1 :
    stateR == 5'd11 ? reg0 :
    stateR == 5'd13 ? arrRData_a :
    stateR == 5'd14 ? out0_Bin1 :
    stateR == 5'd15 ? reg0 :
    stateR == 5'd18 ? arrRData_a :
    stateR == 5'd19 ? reg0 :
    reg0;
  assign stationReg1 =
    stateR == 5'd1 ? 64'd1 :
    reg1;
  assign stationReg2 =
    stateR == 5'd1 ? 64'd0 :
    reg2;

  always @(posedge clk) begin
    if(r_enable) begin
      stateR <= '0;
      linkreg <= '1;
      w_enable <= 1'd0;
      reg0 <= init_i;
    end else begin
      case(stateR)
        '1: begin
          w_enable <= 1'd1;
          result <= reg0;
        end
        5'd0: stateR <= 5'd1;
        5'd1: stateR <= 5'd2;
        5'd2: stateR <= 5'd3;
        5'd3: stateR <= 5'd4;
        5'd4: stateR <= 5'd5;
        5'd5: stateR <= 5'd6;
        5'd6: stateR <= 5'd7;
        5'd7: stateR <= (stationReg0) ? 5'd8 : 5'd12;
        5'd8: stateR <= 5'd9;
        5'd9: stateR <= 5'd10;
        5'd10: stateR <= 5'd11;
        5'd11: stateR <= 5'd16;
        5'd12: stateR <= 5'd13;
        5'd13: stateR <= 5'd14;
        5'd14: stateR <= 5'd15;
        5'd15: stateR <= 5'd16;
        5'd16: stateR <= 5'd17;
        5'd17: stateR <= 5'd18;
        5'd18: stateR <= 5'd19;
        5'd19: stateR <= linkreg;
      endcase
      reg0 <= stationReg0;
      reg1 <= stationReg1;
      reg2 <= stationReg2;
    end
  end
endmodule // main

module arr_a (
  input wire clk, arrWEnable_a,
  input wire[0:0] arrAddr_a,
  output wire signed[63:0] arrRData_a,
  input wire signed[63:0] arrWData_a
);
  reg[0:0] delayedRAddr;
  reg signed[63:0] mem [0:0];
  always @(posedge clk) begin
    if(arrWEnable_a) begin
      mem[arrAddr_a] <= arrWData_a;
    end
    delayedRAddr <= arrWEnable_a ? 'x : arrAddr_a;
  end
  assign arrRData_a = mem[delayedRAddr];
endmodule
`default_nettype wire
