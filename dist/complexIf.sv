`default_nettype none
module main (
  input wire clk, r_enable, controlArr,
  input wire init_i,
  input wire controlArrWEnable_a,
  input wire[0:0] controlArrAddr_a,
  output wire controlArrRData_a,
  input wire controlArrWData_a,
  output reg w_enable,
  output reg[1:0] result
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

  wire in0_Bin0;
  wire in1_Bin0;
  wire out0_Bin0 = in0_Bin0 == in1_Bin0;
  wire[1:0] in0_Bin1;
  wire[1:0] in1_Bin1;
  wire[1:0] out0_Bin1 = in0_Bin1 + in1_Bin1;

  wire arrWEnable_a;
  wire arrAddr_a;
  wire arrRData_a;
  wire arrWData_a;
  arr_a arr_a(.*);

  assign in0_Bin1 =
    stateR == 4'd7 ? 2'd1 :
    'x;
  assign in1_Bin1 =
    stateR == 4'd7 ? reg0[1:0] :
    'x;
  assign in0_Bin0 =
    stateR == 4'd1 ? reg0[0:0] :
    'x;
  assign in1_Bin0 =
    stateR == 4'd1 ? 1'd0 :
    'x;

  assign arrWEnable_a =
    controlArr ? controlArrWEnable_a :
    stateR == 4'd3 ? 1'd1 :
    1'd0;
  assign arrAddr_a =
    controlArr ? controlArrAddr_a :
    stateR == 4'd3 ? reg2[0:0] :
    'x;
  assign arrWData_a =
    controlArr ? controlArrWData_a :
    stateR == 4'd3 ? reg0[0:0] :
    'x;
  assign controlArrRData_a = controlArr ? arrRData_a : 'x;

  assign stationReg0 =
    stateR == 4'd4 ? reg3 :
    stateR == 4'd5 ? 64'd2 :
    stateR == 4'd6 ? reg0 :
    stateR == 4'd7 ? {62'd0, out0_Bin1} :
    stateR == 4'd8 ? reg0 :
    reg0;
  assign stationReg1 =
    stateR == 4'd1 ? 64'd1 :
    reg1;
  assign stationReg2 =
    stateR == 4'd1 ? {63'd0, out0_Bin0} :
    stateR == 4'd2 ? 64'd0 :
    reg2;
  assign stationReg3 =
    stateR == 4'd2 ? 64'd1 :
    reg3;

  always @(posedge clk) begin
    if(r_enable) begin
      stateR <= '0;
      linkreg <= '1;
      w_enable <= 1'd0;
      reg0 <= {63'd0, init_i};
    end else begin
      case(stateR)
        '1: begin
          w_enable <= 1'd1;
          result <= reg0[1:0];
        end
        4'd0: stateR <= 4'd1;
        4'd1: stateR <= (stationReg2) ? 4'd2 : 4'd5;
        4'd2: stateR <= 4'd3;
        4'd3: stateR <= 4'd4;
        4'd4: stateR <= 4'd7;
        4'd5: stateR <= 4'd6;
        4'd6: stateR <= 4'd7;
        4'd7: stateR <= 4'd8;
        4'd8: stateR <= linkreg;
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
  input wire[0:0] arrAddr_a,
  output wire arrRData_a,
  input wire arrWData_a
);
  reg[0:0] delayedRAddr;
  reg mem [0:0];
  always @(posedge clk) begin
    if(arrWEnable_a) begin
      mem[arrAddr_a] <= arrWData_a;
    end
    delayedRAddr <= arrWEnable_a ? 'x : arrAddr_a;
  end
  assign arrRData_a = mem[delayedRAddr];
endmodule
`default_nettype wire
