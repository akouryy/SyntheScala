`default_nettype none
module main (
  input wire clk, r_enable, controlArr,
  input wire[63:0] init_i,
  input wire controlArrWEnable_a,
  input wire[0:0] controlArrAddr_a,
  output wire signed[63:0] controlArrRData_a,
  input wire signed[63:0] controlArrWData_a,
  output reg w_enable,
  output reg[63:0] result
);
  reg[4:0] state;
  reg[4:0] linkreg;
  reg[63:0] reg0;
  reg[63:0] reg1;
  reg[63:0] reg2;
  reg[63:0] reg3;

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
    state == 5'd8 ? reg1 :
    state == 5'd11 ? reg1 :
    'x;
  assign in1_Bin1 =
    state == 5'd8 ? reg0 :
    state == 5'd11 ? reg0 :
    'x;
  assign in0_Bin0 =
    state == 5'd4 ? reg0 :
    'x;
  assign in1_Bin0 =
    state == 5'd4 ? {reg2[63], reg2[0:0]} :
    'x;

  assign arrWEnable_a =
    controlArr ? controlArrWEnable_a :
    state == 5'd1 ? 1'd1 :
    state == 5'd2 ? 1'd1 :
    state == 5'd3 ? 1'd0 :
    state == 5'd7 ? 1'd0 :
    state == 5'd10 ? 1'd0 :
    state == 5'd13 ? 1'd1 :
    state == 5'd14 ? 1'd0 :
    1'd0;
  assign arrAddr_a =
    controlArr ? controlArrAddr_a :
    state == 5'd1 ? reg3[0:0] :
    state == 5'd2 ? reg3[0:0] :
    state == 5'd3 ? reg3[0:0] :
    state == 5'd7 ? reg3[0:0] :
    state == 5'd10 ? reg3[0:0] :
    state == 5'd13 ? reg3[0:0] :
    state == 5'd14 ? reg3[0:0] :
    'x;
  assign arrWData_a =
    controlArr ? controlArrWData_a :
    state == 5'd1 ? reg1 :
    state == 5'd2 ? reg0 :
    state == 5'd13 ? reg0 :
    'x;
  assign controlArrRData_a = controlArr ? arrRData_a : 'x;

  always @(posedge clk) begin
    if(r_enable) begin
      state <= '0;
      linkreg <= '1;
      w_enable <= 1'd0;
      reg0 <= init_i;
    end else begin
      case(state)
        '1: begin
          w_enable <= 1'd1;
          result <= reg0;
        end
        5'd15: state <= linkreg;
        5'd12: state <= 5'd13;
        5'd14: state <= 5'd15;
        5'd10: state <= 5'd11;
        5'd3: state <= 5'd4;
        5'd13: state <= 5'd14;
        5'd0: state <= 5'd1;
        5'd5: state <= reg0 ? 5'd7 : 5'd10;
        5'd2: state <= 5'd3;
        5'd8: state <= 5'd9;
        5'd9: state <= 5'd13;
        5'd11: state <= 5'd12;
        5'd4: state <= 5'd5;
        5'd1: state <= 5'd2;
        5'd7: state <= 5'd8;
      endcase
      case(state)
        5'd3: reg0 <= arrRData_a;
        5'd4: reg0 <= {63'd0, out0_Bin0};
        5'd5: reg0 <= reg0 ? 64'd2 : $unsigned(-$signed(64'd3));
        5'd8: reg0 <= out0_Bin1;
        5'd11: reg0 <= out0_Bin1;
        5'd14: reg0 <= arrRData_a;
        5'd15: reg0 <= reg0;
      endcase
      case(state)
        5'd0: reg1 <= 64'd1;
        5'd7: reg1 <= arrRData_a;
        5'd10: reg1 <= arrRData_a;
      endcase
      case(state)
        5'd0: reg2 <= 64'd0;
      endcase
      case(state)
        5'd0: reg3 <= 64'd0;
      endcase
    end
  end
endmodule // main

module arr_a (
  input wire clk, arrWEnable_a,
  input wire[0:0] arrAddr_a,
  output wire signed[63:0] arrRData_a,
  input wire signed[63:0] arrWData_a
);
  reg signed[63:0] mem [0:0];
  always @(posedge clk) begin
    if(arrWEnable_a) begin
      mem[arrAddr_a] <= arrWData_a;
    end
  end
  assign arrRData_a = /*arrWEnable_a ? 'x :*/ mem[arrAddr_a];
endmodule
`default_nettype wire