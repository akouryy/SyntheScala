`default_nettype none
module main (
  input wire clk, r_enable, controlArr,
  input wire[9:0] init_i,
  input wire signed[63:0] init_acc,
  input wire controlArrWEnable_a,
  input wire[9:0] controlArrAddr_a,
  output wire signed[26:0] controlArrRData_a,
  input wire signed[26:0] controlArrWData_a,
  input wire controlArrWEnable_b,
  input wire[9:0] controlArrAddr_b,
  output wire signed[26:0] controlArrRData_b,
  input wire signed[26:0] controlArrWData_b,
  output reg w_enable,
  output reg signed[63:0] result
);
  reg[3:0] stateR;
  reg[3:0] linkreg;
  reg[63:0] reg0;
  wire[63:0] stationReg0;
  reg[63:0] reg1;
  wire[63:0] stationReg1;
  reg[63:0] reg2;
  wire[63:0] stationReg2;
  reg[63:0] reg3;
  wire[63:0] stationReg3;

  wire[9:0] in0_Bin0;
  wire[9:0] in1_Bin0;
  wire out0_Bin0 = in0_Bin0 == in1_Bin0;
  wire[9:0] in0_Bin1;
  wire in1_Bin1;
  wire[9:0] out0_Bin1 = in0_Bin1 + in1_Bin1;
  wire signed[63:0] in0_Bin2;
  wire signed[26:0] in1_Bin2;
  wire signed[63:0] out0_Bin2 = in0_Bin2 * in1_Bin2;
  wire signed[63:0] in0_Bin3;
  wire signed[63:0] in1_Bin3;
  wire signed[63:0] out0_Bin3 = in0_Bin3 + in1_Bin3;

  wire arrWEnable_a;
  wire[9:0] arrAddr_a;
  wire signed[26:0] arrRData_a;
  wire signed[26:0] arrWData_a;
  arr_a arr_a(.*);
  wire arrWEnable_b;
  wire[9:0] arrAddr_b;
  wire signed[26:0] arrRData_b;
  wire signed[26:0] arrWData_b;
  arr_b arr_b(.*);

  assign in0_Bin1 =
    stateR == 4'd3 ? reg0[9:0] :
    'x;
  assign in1_Bin1 =
    stateR == 4'd3 ? 1'd1 :
    'x;
  assign in0_Bin2 =
    stateR == 4'd5 ? reg3 :
    'x;
  assign in1_Bin2 =
    stateR == 4'd5 ? {reg2[63], reg2[26:0]} :
    'x;
  assign in0_Bin0 =
    stateR == 4'd1 ? reg0[9:0] :
    'x;
  assign in1_Bin0 =
    stateR == 4'd1 ? 10'd1000 :
    'x;
  assign in0_Bin3 =
    stateR == 4'd6 ? reg1 :
    'x;
  assign in1_Bin3 =
    stateR == 4'd6 ? reg2 :
    'x;

  assign arrWEnable_a =
    controlArr ? controlArrWEnable_a :
    stateR == 4'd3 ? 1'd0 :
    1'd0;
  assign arrAddr_a =
    controlArr ? controlArrAddr_a :
    stateR == 4'd3 ? reg0[9:0] :
    'x;
  assign arrWData_a =
    controlArr ? controlArrWData_a :
    'x;
  assign controlArrRData_a = controlArr ? arrRData_a : 'x;
  assign arrWEnable_b =
    controlArr ? controlArrWEnable_b :
    stateR == 4'd3 ? 1'd0 :
    1'd0;
  assign arrAddr_b =
    controlArr ? controlArrAddr_b :
    stateR == 4'd3 ? reg0[9:0] :
    'x;
  assign arrWData_b =
    controlArr ? controlArrWData_b :
    'x;
  assign controlArrRData_b = controlArr ? arrRData_b : 'x;

  assign stationReg0 =
    stateR == 4'd2 ? reg1 :
    stateR == 4'd3 ? {54'd0, out0_Bin1} :
    stateR == 4'd7 ? reg0 :
    reg0;
  assign stationReg1 =
    stateR == 4'd6 ? out0_Bin3 :
    stateR == 4'd7 ? reg1 :
    reg1;
  assign stationReg2 =
    stateR == 4'd1 ? {63'd0, out0_Bin0} :
    stateR == 4'd4 ? {{37{arrRData_b[26]}}, arrRData_b} :
    stateR == 4'd5 ? out0_Bin2 :
    reg2;
  assign stationReg3 =
    stateR == 4'd4 ? {{37{arrRData_a[26]}}, arrRData_a} :
    reg3;

  always @(posedge clk) begin
    if(r_enable) begin
      stateR <= '0;
      linkreg <= '1;
      w_enable <= 1'd0;
      reg0 <= {54'd0, init_i};
      reg1 <= init_acc;
    end else begin
      case(stateR)
        '1: begin
          w_enable <= 1'd1;
          result <= reg0;
        end
        4'd0: stateR <= 4'd1;
        4'd1: stateR <= (stationReg2) ? 4'd2 : 4'd3;
        4'd2: stateR <= linkreg;
        4'd3: stateR <= 4'd4;
        4'd4: stateR <= 4'd5;
        4'd5: stateR <= 4'd6;
        4'd6: stateR <= 4'd7;
        4'd7: stateR <= 4'd0;
      endcase
      reg0 <= stationReg0;
      reg1 <= stationReg1;
      reg2 <= stationReg2;
      reg3 <= stationReg3;
    end
  end
endmodule // main

module arr_a (
  input wire clk, arrWEnable_a,
  input wire[9:0] arrAddr_a,
  output wire signed[26:0] arrRData_a,
  input wire signed[26:0] arrWData_a
);
  reg[9:0] delayedRAddr;
  reg signed[26:0] mem [0:999];
  always @(posedge clk) begin
    if(arrWEnable_a) begin
      mem[arrAddr_a] <= arrWData_a;
    end
    delayedRAddr <= arrWEnable_a ? 'x : arrAddr_a;
  end
  assign arrRData_a = mem[delayedRAddr];
endmodule

module arr_b (
  input wire clk, arrWEnable_b,
  input wire[9:0] arrAddr_b,
  output wire signed[26:0] arrRData_b,
  input wire signed[26:0] arrWData_b
);
  reg[9:0] delayedRAddr;
  reg signed[26:0] mem [0:999];
  always @(posedge clk) begin
    if(arrWEnable_b) begin
      mem[arrAddr_b] <= arrWData_b;
    end
    delayedRAddr <= arrWEnable_b ? 'x : arrAddr_b;
  end
  assign arrRData_b = mem[delayedRAddr];
endmodule
`default_nettype wire
