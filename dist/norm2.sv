`default_nettype none
module main (
  input wire clk, r_enable, controlArr,
  input wire[9:0] init_i,
  input wire signed[63:0] init_acc,
  input wire controlArrWEnable_a,
  input wire[9:0] controlArrAddr_a,
  output wire signed[26:0] controlArrRData_a,
  input wire signed[26:0] controlArrWData_a,
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

  assign in0_Bin1 =
    stateR == 4'd3 ? reg0[9:0] :
    'x;
  assign in1_Bin1 =
    stateR == 4'd3 ? 1'd1 :
    'x;
  assign in0_Bin2 =
    stateR == 4'd6 ? reg3 :
    'x;
  assign in1_Bin2 =
    stateR == 4'd6 ? {reg0[63], reg0[26:0]} :
    'x;
  assign in0_Bin0 =
    stateR == 4'd1 ? reg0[9:0] :
    'x;
  assign in1_Bin0 =
    stateR == 4'd1 ? 10'd1000 :
    'x;
  assign in0_Bin3 =
    stateR == 4'd7 ? reg1 :
    'x;
  assign in1_Bin3 =
    stateR == 4'd7 ? reg0 :
    'x;

  assign arrWEnable_a =
    controlArr ? controlArrWEnable_a :
    stateR == 4'd3 ? 1'd0 :
    stateR == 4'd4 ? 1'd0 :
    1'd0;
  assign arrAddr_a =
    controlArr ? controlArrAddr_a :
    stateR == 4'd3 ? reg0[9:0] :
    stateR == 4'd4 ? reg0[9:0] :
    'x;
  assign arrWData_a =
    controlArr ? controlArrWData_a :
    'x;
  assign controlArrRData_a = controlArr ? arrRData_a : 'x;

  assign stationReg0 =
    stateR == 4'd4 ? {{37{arrRData_a[26]}}, arrRData_a} :
    stateR == 4'd6 ? out0_Bin2 :
    reg0;
  assign stationReg1 =
    stateR == 4'd7 ? out0_Bin3 :
    reg1;
  assign stationReg2 =
    stateR == 4'd1 ? {63'd0, out0_Bin0} :
    stateR == 4'd3 ? {54'd0, out0_Bin1} :
    reg2;
  assign stationReg3 =
    stateR == 4'd5 ? {{37{arrRData_a[26]}}, arrRData_a} :
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
      case(stateR)
        4'd2: reg0 <= stationReg1;
        4'd7: reg0 <= stationReg2;
        default: reg0 <= stationReg0;
      endcase
      case(stateR)
        4'd7: reg1 <= stationReg1;
        default: reg1 <= stationReg1;
      endcase
      case(stateR)
        default: reg2 <= stationReg2;
      endcase
      case(stateR)
        default: reg3 <= stationReg3;
      endcase
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
  reg signed[26:0] mem [0:1023];
  always @(posedge clk) begin
    if(arrWEnable_a) begin
      mem[arrAddr_a] <= arrWData_a;
    end
    delayedRAddr <= arrAddr_a;
  end
  assign arrRData_a = mem[delayedRAddr];
endmodule
`default_nettype wire
