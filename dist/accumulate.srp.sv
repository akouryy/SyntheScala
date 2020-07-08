`default_nettype none
module main (
  input wire clk, r_enable, controlArr,
  input wire[9:0] init_i_t_a,
  input wire signed[63:0] init_acc_t_a,
  input wire controlArrWEnable_a,
  input wire[9:0] controlArrAddr_a,
  output wire signed[63:0] controlArrRData_a,
  input wire signed[63:0] controlArrWData_a,
  output reg w_enable,
  output reg result
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

  wire[9:0] in0_Bin0;
  wire[9:0] in1_Bin0;
  wire out0_Bin0 = in0_Bin0 == in1_Bin0;
  wire[9:0] in0_Bin1;
  wire in1_Bin1;
  wire[9:0] out0_Bin1 = in0_Bin1 + in1_Bin1;
  wire signed[63:0] in0_Bin2;
  wire signed[63:0] in1_Bin2;
  wire signed[63:0] out0_Bin2 = in0_Bin2 + in1_Bin2;

  wire arrWEnable_a;
  wire[9:0] arrAddr_a;
  wire signed[63:0] arrRData_a;
  wire signed[63:0] arrWData_a;
  arr_a arr_a(.*);

  assign in0_Bin1 =
    stateR == 3'd2 ? reg0[9:0] :
    'x;
  assign in1_Bin1 =
    stateR == 3'd2 ? 1'd1 :
    'x;
  assign in0_Bin2 =
    stateR == 3'd4 ? reg1 :
    'x;
  assign in1_Bin2 =
    stateR == 3'd4 ? reg3 :
    'x;
  assign in0_Bin0 =
    stateR == 3'd1 ? reg0[9:0] :
    stateR == 3'd5 ? reg2[9:0] :
    'x;
  assign in1_Bin0 =
    stateR == 3'd1 ? 10'd1000 :
    stateR == 3'd5 ? 10'd1000 :
    'x;

  assign arrWEnable_a =
    controlArr ? controlArrWEnable_a :
    stateR == 3'd2 ? 1'd0 :
    stateR == 3'd5 ? 1'd1 :
    1'd0;
  assign arrAddr_a =
    controlArr ? controlArrAddr_a :
    stateR == 3'd2 ? reg0[9:0] :
    stateR == 3'd5 ? reg0[9:0] :
    'x;
  assign arrWData_a =
    controlArr ? controlArrWData_a :
    stateR == 3'd5 ? reg1 :
    'x;
  assign controlArrRData_a = controlArr ? arrRData_a : 'x;

  assign stationReg0 =
    stateR == 3'd5 ? {63'd0, out0_Bin0} :
    stateR == 3'd6 ? 64'd0 :
    reg0;
  assign stationReg1 =
    stateR == 3'd4 ? out0_Bin2 :
    reg1;
  assign stationReg2 =
    stateR == 3'd1 ? {63'd0, out0_Bin0} :
    stateR == 3'd2 ? {54'd0, out0_Bin1} :
    reg2;
  assign stationReg3 =
    stateR == 3'd3 ? arrRData_a :
    reg3;

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
          result <= reg0[0:0];
        end
        3'd0: stateR <= 3'd1;
        3'd1: stateR <= (stationReg2) ? 3'd6 : 3'd2;
        3'd2: stateR <= 3'd3;
        3'd3: stateR <= 3'd4;
        3'd4: stateR <= 3'd5;
        3'd5: stateR <= (stationReg0) ? 3'd6 : 3'd2;
        3'd6: stateR <= linkreg;
      endcase
      case(stateR)
        3'd5: reg0 <= stationReg2;
        3'd6: reg0 <= stationReg0;
        default: reg0 <= stationReg0;
      endcase
      case(stateR)
        3'd5: reg1 <= stationReg1;
        default: reg1 <= stationReg1;
      endcase
      case(stateR)
        3'd5: reg2 <= stationReg0;
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
  output wire signed[63:0] arrRData_a,
  input wire signed[63:0] arrWData_a
);
  reg[9:0] delayedRAddr;
  reg signed[63:0] mem [0:999];
  always @(posedge clk) begin
    if(arrWEnable_a) begin
      mem[arrAddr_a] <= arrWData_a;
    end
    delayedRAddr <= arrWEnable_a ? 'x : arrAddr_a;
  end
  assign arrRData_a = mem[delayedRAddr];
endmodule
`default_nettype wire
