`default_nettype none
module main (
  input wire clk, r_enable, controlArr,
  input wire[9:0] init_i_t_a,
  input wire signed[63:0] init_acc_t_a,
  input wire controlArrWEnable_a,
  input wire[9:0] controlArrAddr_a,
  output wire signed[26:0] controlArrRData_a,
  input wire signed[26:0] controlArrWData_a,
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
  reg[63:0] reg3;
  wire[63:0] stationReg3;
  reg[63:0] reg4;
  wire[63:0] stationReg4;
  reg[63:0] reg5;
  wire[63:0] stationReg5;
  reg[63:0] reg6;
  wire[63:0] stationReg6;
  reg[63:0] reg7;
  wire[63:0] stationReg7;
  reg[63:0] reg8;
  wire[63:0] stationReg8;
  reg[63:0] reg9;
  wire[63:0] stationReg9;
  reg[63:0] reg10;
  wire[63:0] stationReg10;
  reg[63:0] reg11;
  wire[63:0] stationReg11;
  reg[63:0] reg12;
  wire[63:0] stationReg12;
  reg[63:0] reg13;
  wire[63:0] stationReg13;
  reg[63:0] reg14;
  wire[63:0] stationReg14;
  reg[63:0] reg15;
  wire[63:0] stationReg15;

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
    stateR == 5'd2 ? reg0[9:0] :
    stateR == 5'd6 ? reg7[9:0] :
    stateR == 5'd12 ? reg10[9:0] :
    'x;
  assign in1_Bin2 =
    stateR == 5'd7 ? {reg9[63], reg9[26:0]} :
    stateR == 5'd9 ? {reg9[63], reg9[26:0]} :
    stateR == 5'd13 ? {reg14[63], reg14[26:0]} :
    stateR == 5'd15 ? {reg14[63], reg14[26:0]} :
    'x;
  assign in0_Bin0 =
    stateR == 5'd1 ? reg0[9:0] :
    stateR == 5'd3 ? reg7[9:0] :
    stateR == 5'd7 ? reg10[9:0] :
    stateR == 5'd13 ? reg6[9:0] :
    'x;
  assign in0_Bin3 =
    stateR == 5'd10 ? reg1 :
    stateR == 5'd12 ? reg1 :
    stateR == 5'd14 ? reg1 :
    stateR == 5'd16 ? reg1 :
    'x;
  assign in1_Bin3 =
    stateR == 5'd10 ? reg13 :
    stateR == 5'd12 ? reg13 :
    stateR == 5'd14 ? reg13 :
    stateR == 5'd16 ? reg9 :
    'x;
  assign in1_Bin1 =
    stateR == 5'd2 ? 1'd1 :
    stateR == 5'd6 ? 1'd1 :
    stateR == 5'd12 ? 1'd1 :
    'x;
  assign in0_Bin2 =
    stateR == 5'd7 ? reg11 :
    stateR == 5'd9 ? reg11 :
    stateR == 5'd13 ? reg0 :
    stateR == 5'd15 ? reg0 :
    'x;
  assign in1_Bin0 =
    stateR == 5'd1 ? 10'd1000 :
    stateR == 5'd3 ? 10'd1000 :
    stateR == 5'd7 ? 10'd1000 :
    stateR == 5'd13 ? 10'd1000 :
    'x;

  assign arrWEnable_a =
    controlArr ? controlArrWEnable_a :
    stateR == 5'd2 ? 1'd0 :
    stateR == 5'd3 ? 1'd0 :
    stateR == 5'd6 ? 1'd0 :
    stateR == 5'd7 ? 1'd0 :
    stateR == 5'd12 ? 1'd0 :
    stateR == 5'd13 ? 1'd0 :
    1'd0;
  assign arrAddr_a =
    controlArr ? controlArrAddr_a :
    stateR == 5'd2 ? reg0[9:0] :
    stateR == 5'd3 ? reg0[9:0] :
    stateR == 5'd6 ? reg7[9:0] :
    stateR == 5'd7 ? reg7[9:0] :
    stateR == 5'd12 ? reg10[9:0] :
    stateR == 5'd13 ? reg10[9:0] :
    'x;
  assign arrWData_a =
    controlArr ? controlArrWData_a :
    'x;
  assign controlArrRData_a = controlArr ? arrRData_a : 'x;

  assign stationReg0 =
    stateR == 5'd12 ? {{37{arrRData_a[26]}}, arrRData_a} :
    stateR == 5'd14 ? {{37{arrRData_a[26]}}, arrRData_a} :
    reg0;
  assign stationReg1 =
    stateR == 5'd10 ? out0_Bin3 :
    stateR == 5'd12 ? out0_Bin3 :
    stateR == 5'd14 ? out0_Bin3 :
    reg1;
  assign stationReg2 =
    reg2;
  assign stationReg3 =
    reg3;
  assign stationReg4 =
    reg4;
  assign stationReg5 =
    reg5;
  assign stationReg6 =
    stateR == 5'd1 ? {63'd0, out0_Bin0} :
    stateR == 5'd12 ? {54'd0, out0_Bin1} :
    reg6;
  assign stationReg7 =
    stateR == 5'd2 ? {54'd0, out0_Bin1} :
    reg7;
  assign stationReg8 =
    stateR == 5'd3 ? {63'd0, out0_Bin0} :
    reg8;
  assign stationReg9 =
    stateR == 5'd3 ? {{37{arrRData_a[26]}}, arrRData_a} :
    stateR == 5'd13 ? out0_Bin2 :
    stateR == 5'd15 ? out0_Bin2 :
    reg9;
  assign stationReg10 =
    stateR == 5'd6 ? {54'd0, out0_Bin1} :
    reg10;
  assign stationReg11 =
    stateR == 5'd6 ? {{37{arrRData_a[26]}}, arrRData_a} :
    stateR == 5'd8 ? {{37{arrRData_a[26]}}, arrRData_a} :
    stateR == 5'd13 ? {{37{arrRData_a[26]}}, arrRData_a} :
    reg11;
  assign stationReg12 =
    stateR == 5'd7 ? {63'd0, out0_Bin0} :
    reg12;
  assign stationReg13 =
    stateR == 5'd7 ? out0_Bin2 :
    stateR == 5'd9 ? out0_Bin2 :
    stateR == 5'd13 ? {63'd0, out0_Bin0} :
    reg13;
  assign stationReg14 =
    stateR == 5'd7 ? {{37{arrRData_a[26]}}, arrRData_a} :
    reg14;
  assign stationReg15 =
    stateR == 5'd16 ? out0_Bin3 :
    reg15;

  always @(posedge clk) begin
    if(r_enable) begin
      stateR <= '0;
      linkreg <= '1;
      w_enable <= 1'd0;
      reg0 <= {54'd0, init_i_t_a};
      reg1 <= init_acc_t_a;
    end else begin
      case(stateR)
        '1: begin
          w_enable <= 1'd1;
          result <= reg0;
        end
        5'd0: stateR <= 5'd1;
        5'd1: stateR <= (stationReg6) ? 5'd5 : 5'd2;
        5'd2: stateR <= 5'd3;
        5'd3: stateR <= (stationReg8) ? 5'd8 : 5'd6;
        5'd5: stateR <= linkreg;
        5'd6: stateR <= 5'd7;
        5'd7: stateR <= (stationReg12) ? 5'd14 : 5'd12;
        5'd8: stateR <= 5'd9;
        5'd9: stateR <= 5'd10;
        5'd10: stateR <= linkreg;
        5'd12: stateR <= 5'd13;
        5'd13: stateR <= (stationReg13) ? 5'd14 : 5'd12;
        5'd14: stateR <= 5'd15;
        5'd15: stateR <= 5'd16;
        5'd16: stateR <= linkreg;
      endcase
      case(stateR)
        5'd5: reg0 <= stationReg1;
        5'd10: reg0 <= stationReg1;
        5'd13: reg0 <= stationReg7;
        5'd16: reg0 <= stationReg15;
        default: reg0 <= stationReg0;
      endcase
      case(stateR)
        5'd13: reg1 <= stationReg1;
        default: reg1 <= stationReg1;
      endcase
      case(stateR)
        default: reg2 <= stationReg2;
      endcase
      case(stateR)
        5'd13: reg3 <= stationReg2;
        default: reg3 <= stationReg3;
      endcase
      case(stateR)
        default: reg4 <= stationReg4;
      endcase
      case(stateR)
        default: reg5 <= stationReg5;
      endcase
      case(stateR)
        5'd13: reg6 <= stationReg8;
        default: reg6 <= stationReg6;
      endcase
      case(stateR)
        5'd13: reg7 <= stationReg10;
        default: reg7 <= stationReg7;
      endcase
      case(stateR)
        5'd13: reg8 <= stationReg12;
        default: reg8 <= stationReg8;
      endcase
      case(stateR)
        5'd13: reg9 <= stationReg14;
        default: reg9 <= stationReg9;
      endcase
      case(stateR)
        5'd13: reg10 <= stationReg6;
        default: reg10 <= stationReg10;
      endcase
      case(stateR)
        5'd13: reg11 <= stationReg0;
        default: reg11 <= stationReg11;
      endcase
      case(stateR)
        5'd13: reg12 <= stationReg13;
        default: reg12 <= stationReg12;
      endcase
      case(stateR)
        5'd13: reg13 <= stationReg9;
        default: reg13 <= stationReg13;
      endcase
      case(stateR)
        5'd13: reg14 <= stationReg11;
        default: reg14 <= stationReg14;
      endcase
      case(stateR)
        5'd13: reg15 <= stationReg3;
        default: reg15 <= stationReg15;
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
