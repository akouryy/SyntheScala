`default_nettype none
module main (
  input wire clk, r_enable, controlArr,
  input wire[9:0] init_i_t_a,
  input wire signed[63:0] init_acc_t_a,
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
  reg[63:0] reg16;
  wire[63:0] stationReg16;
  reg[63:0] reg17;
  wire[63:0] stationReg17;
  reg[63:0] reg18;
  wire[63:0] stationReg18;
  reg[63:0] reg19;
  wire[63:0] stationReg19;
  reg[63:0] reg20;
  wire[63:0] stationReg20;
  reg[63:0] reg21;
  wire[63:0] stationReg21;
  reg[63:0] reg22;
  wire[63:0] stationReg22;
  reg[63:0] reg23;
  wire[63:0] stationReg23;

  wire[9:0] in0_Bin0;
  wire in1_Bin0;
  wire[9:0] out0_Bin0 = in0_Bin0 + in1_Bin0;
  wire[9:0] in0_Bin1;
  wire[9:0] in1_Bin1;
  wire out0_Bin1 = in0_Bin1 == in1_Bin1;
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

  assign in1_Bin2 =
    stateR == 5'd7 ? {reg15[63], reg15[26:0]} :
    stateR == 5'd10 ? {reg15[63], reg15[26:0]} :
    stateR == 5'd11 ? {reg15[63], reg15[26:0]} :
    stateR == 5'd12 ? {reg20[63], reg20[26:0]} :
    stateR == 5'd15 ? {reg20[63], reg20[26:0]} :
    stateR == 5'd16 ? {reg20[63], reg20[26:0]} :
    stateR == 5'd17 ? {reg10[63], reg10[26:0]} :
    'x;
  assign in0_Bin0 =
    stateR == 5'd1 ? reg0[9:0] :
    stateR == 5'd2 ? reg9[9:0] :
    stateR == 5'd5 ? reg11[9:0] :
    stateR == 5'd10 ? reg13[9:0] :
    stateR == 5'd15 ? reg19[9:0] :
    'x;
  assign in1_Bin3 =
    stateR == 5'd8 ? reg17 :
    stateR == 5'd12 ? reg17 :
    stateR == 5'd13 ? reg17 :
    stateR == 5'd15 ? reg17 :
    stateR == 5'd16 ? reg17 :
    stateR == 5'd17 ? reg17 :
    stateR == 5'd18 ? reg23 :
    'x;
  assign in0_Bin1 =
    stateR == 5'd1 ? reg0[9:0] :
    stateR == 5'd2 ? reg9[9:0] :
    stateR == 5'd5 ? reg11[9:0] :
    stateR == 5'd10 ? reg13[9:0] :
    stateR == 5'd15 ? reg19[9:0] :
    'x;
  assign in0_Bin3 =
    stateR == 5'd8 ? reg1 :
    stateR == 5'd12 ? reg1 :
    stateR == 5'd13 ? reg16 :
    stateR == 5'd15 ? reg1 :
    stateR == 5'd16 ? reg1 :
    stateR == 5'd17 ? reg16 :
    stateR == 5'd18 ? reg22 :
    'x;
  assign in1_Bin1 =
    stateR == 5'd1 ? 10'd1000 :
    stateR == 5'd2 ? 10'd1000 :
    stateR == 5'd5 ? 10'd1000 :
    stateR == 5'd10 ? 10'd1000 :
    stateR == 5'd15 ? 10'd1000 :
    'x;
  assign in0_Bin2 =
    stateR == 5'd7 ? reg16 :
    stateR == 5'd10 ? reg16 :
    stateR == 5'd11 ? reg16 :
    stateR == 5'd12 ? reg18 :
    stateR == 5'd15 ? reg18 :
    stateR == 5'd16 ? reg18 :
    stateR == 5'd17 ? reg15 :
    'x;
  assign in1_Bin0 =
    stateR == 5'd1 ? 1'd1 :
    stateR == 5'd2 ? 1'd1 :
    stateR == 5'd5 ? 1'd1 :
    stateR == 5'd10 ? 1'd1 :
    stateR == 5'd15 ? 1'd1 :
    'x;

  assign arrWEnable_a =
    controlArr ? controlArrWEnable_a :
    stateR == 5'd2 ? 1'd0 :
    stateR == 5'd5 ? 1'd0 :
    stateR == 5'd10 ? 1'd0 :
    stateR == 5'd15 ? 1'd0 :
    1'd0;
  assign arrAddr_a =
    controlArr ? controlArrAddr_a :
    stateR == 5'd2 ? reg0[9:0] :
    stateR == 5'd5 ? reg9[9:0] :
    stateR == 5'd10 ? reg11[9:0] :
    stateR == 5'd15 ? reg13[9:0] :
    'x;
  assign arrWData_a =
    controlArr ? controlArrWData_a :
    'x;
  assign controlArrRData_a = controlArr ? arrRData_a : 'x;
  assign arrWEnable_b =
    controlArr ? controlArrWEnable_b :
    stateR == 5'd2 ? 1'd0 :
    stateR == 5'd5 ? 1'd0 :
    stateR == 5'd10 ? 1'd0 :
    stateR == 5'd15 ? 1'd0 :
    1'd0;
  assign arrAddr_b =
    controlArr ? controlArrAddr_b :
    stateR == 5'd2 ? reg0[9:0] :
    stateR == 5'd5 ? reg9[9:0] :
    stateR == 5'd10 ? reg11[9:0] :
    stateR == 5'd15 ? reg13[9:0] :
    'x;
  assign arrWData_b =
    controlArr ? controlArrWData_b :
    'x;
  assign controlArrRData_b = controlArr ? arrRData_b : 'x;

  assign stationReg0 =
    stateR == 5'd15 ? {54'd0, out0_Bin0} :
    reg0;
  assign stationReg1 =
    stateR == 5'd15 ? {63'd0, out0_Bin1} :
    reg1;
  assign stationReg2 =
    stateR == 5'd18 ? out0_Bin3 :
    reg2;
  assign stationReg3 =
    reg3;
  assign stationReg4 =
    reg4;
  assign stationReg5 =
    reg5;
  assign stationReg6 =
    reg6;
  assign stationReg7 =
    reg7;
  assign stationReg8 =
    reg8;
  assign stationReg9 =
    stateR == 5'd1 ? {54'd0, out0_Bin0} :
    reg9;
  assign stationReg10 =
    stateR == 5'd1 ? {63'd0, out0_Bin1} :
    stateR == 5'd15 ? {{37{arrRData_b[26]}}, arrRData_b} :
    stateR == 5'd16 ? {{37{arrRData_b[26]}}, arrRData_b} :
    reg10;
  assign stationReg11 =
    stateR == 5'd2 ? {54'd0, out0_Bin0} :
    reg11;
  assign stationReg12 =
    stateR == 5'd2 ? {63'd0, out0_Bin1} :
    reg12;
  assign stationReg13 =
    stateR == 5'd5 ? {54'd0, out0_Bin0} :
    reg13;
  assign stationReg14 =
    stateR == 5'd5 ? {63'd0, out0_Bin1} :
    reg14;
  assign stationReg15 =
    stateR == 5'd5 ? {{37{arrRData_b[26]}}, arrRData_b} :
    stateR == 5'd6 ? {{37{arrRData_b[26]}}, arrRData_b} :
    stateR == 5'd15 ? {{37{arrRData_a[26]}}, arrRData_a} :
    stateR == 5'd16 ? {{37{arrRData_a[26]}}, arrRData_a} :
    reg15;
  assign stationReg16 =
    stateR == 5'd5 ? {{37{arrRData_a[26]}}, arrRData_a} :
    stateR == 5'd6 ? {{37{arrRData_a[26]}}, arrRData_a} :
    stateR == 5'd8 ? out0_Bin3 :
    stateR == 5'd12 ? out0_Bin3 :
    stateR == 5'd15 ? out0_Bin3 :
    stateR == 5'd16 ? out0_Bin3 :
    reg16;
  assign stationReg17 =
    stateR == 5'd7 ? out0_Bin2 :
    stateR == 5'd10 ? out0_Bin2 :
    stateR == 5'd11 ? out0_Bin2 :
    stateR == 5'd12 ? out0_Bin2 :
    stateR == 5'd15 ? out0_Bin2 :
    stateR == 5'd16 ? out0_Bin2 :
    reg17;
  assign stationReg18 =
    stateR == 5'd10 ? {{37{arrRData_a[26]}}, arrRData_a} :
    stateR == 5'd11 ? {{37{arrRData_a[26]}}, arrRData_a} :
    reg18;
  assign stationReg19 =
    stateR == 5'd10 ? {54'd0, out0_Bin0} :
    reg19;
  assign stationReg20 =
    stateR == 5'd10 ? {{37{arrRData_b[26]}}, arrRData_b} :
    stateR == 5'd11 ? {{37{arrRData_b[26]}}, arrRData_b} :
    reg20;
  assign stationReg21 =
    stateR == 5'd10 ? {63'd0, out0_Bin1} :
    reg21;
  assign stationReg22 =
    stateR == 5'd13 ? out0_Bin3 :
    stateR == 5'd17 ? out0_Bin3 :
    reg22;
  assign stationReg23 =
    stateR == 5'd17 ? out0_Bin2 :
    reg23;

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
        5'd1: stateR <= (stationReg10) ? 5'd4 : 5'd2;
        5'd2: stateR <= (stationReg12) ? 5'd6 : 5'd5;
        5'd4: stateR <= linkreg;
        5'd5: stateR <= (stationReg14) ? 5'd11 : 5'd10;
        5'd6: stateR <= 5'd7;
        5'd7: stateR <= 5'd8;
        5'd8: stateR <= linkreg;
        5'd10: stateR <= (stationReg21) ? 5'd16 : 5'd15;
        5'd11: stateR <= 5'd12;
        5'd12: stateR <= 5'd13;
        5'd13: stateR <= linkreg;
        5'd15: stateR <= (stationReg1) ? 5'd16 : 5'd15;
        5'd16: stateR <= 5'd17;
        5'd17: stateR <= 5'd18;
        5'd18: stateR <= linkreg;
      endcase
      case(stateR)
        5'd4: reg0 <= stationReg1;
        5'd8: reg0 <= stationReg16;
        5'd13: reg0 <= stationReg22;
        5'd15: reg0 <= stationReg9;
        5'd18: reg0 <= stationReg2;
        default: reg0 <= stationReg0;
      endcase
      case(stateR)
        5'd15: reg1 <= stationReg16;
        default: reg1 <= stationReg1;
      endcase
      case(stateR)
        5'd15: reg2 <= stationReg5;
        default: reg2 <= stationReg2;
      endcase
      case(stateR)
        default: reg3 <= stationReg3;
      endcase
      case(stateR)
        default: reg4 <= stationReg4;
      endcase
      case(stateR)
        5'd15: reg5 <= stationReg3;
        default: reg5 <= stationReg5;
      endcase
      case(stateR)
        default: reg6 <= stationReg6;
      endcase
      case(stateR)
        default: reg7 <= stationReg7;
      endcase
      case(stateR)
        5'd15: reg8 <= stationReg6;
        default: reg8 <= stationReg8;
      endcase
      case(stateR)
        5'd15: reg9 <= stationReg11;
        default: reg9 <= stationReg9;
      endcase
      case(stateR)
        5'd15: reg10 <= stationReg12;
        default: reg10 <= stationReg10;
      endcase
      case(stateR)
        5'd15: reg11 <= stationReg13;
        default: reg11 <= stationReg11;
      endcase
      case(stateR)
        5'd15: reg12 <= stationReg14;
        default: reg12 <= stationReg12;
      endcase
      case(stateR)
        5'd15: reg13 <= stationReg19;
        default: reg13 <= stationReg13;
      endcase
      case(stateR)
        5'd15: reg14 <= stationReg21;
        default: reg14 <= stationReg14;
      endcase
      case(stateR)
        5'd15: reg15 <= stationReg20;
        default: reg15 <= stationReg15;
      endcase
      case(stateR)
        5'd15: reg16 <= stationReg18;
        default: reg16 <= stationReg16;
      endcase
      case(stateR)
        5'd15: reg17 <= stationReg17;
        default: reg17 <= stationReg17;
      endcase
      case(stateR)
        5'd15: reg18 <= stationReg15;
        default: reg18 <= stationReg18;
      endcase
      case(stateR)
        5'd15: reg19 <= stationReg0;
        default: reg19 <= stationReg19;
      endcase
      case(stateR)
        5'd15: reg20 <= stationReg10;
        default: reg20 <= stationReg20;
      endcase
      case(stateR)
        5'd15: reg21 <= stationReg1;
        default: reg21 <= stationReg21;
      endcase
      case(stateR)
        5'd15: reg22 <= stationReg2;
        default: reg22 <= stationReg22;
      endcase
      case(stateR)
        5'd15: reg23 <= stationReg8;
        default: reg23 <= stationReg23;
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
