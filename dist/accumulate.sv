`default_nettype none
module main (
  input wire clk, r_enable,
  input wire[63:0] init_i,
  input wire[63:0] init_acc,
  output reg w_enable,
  output reg[63:0] result
);
  reg[3:0] state;
  reg[3:0] linkreg;
  reg[31:0] reg0;
  reg[31:0] reg1;
  reg[31:0] reg2;
  reg[31:0] reg3;

  wire[9:0] in0_Bin0;
  wire[9:0] in1_Bin0;
  wire[0:0] out0_Bin0 = in0_Bin0 == in1_Bin0;
  wire[9:0] in0_Bin1;
  wire[0:0] in1_Bin1;
  wire[9:0] out0_Bin1 = in0_Bin1 + in1_Bin1;
  wire signed[63:0] in0_Bin2;
  wire signed[31:0] in1_Bin2;
  wire signed[63:0] out0_Bin2 = in0_Bin2 + in1_Bin2;

  wire arrWEnable_a;
  wire[9:0] arrAddr_a;
  wire signed[31:0] arrRData_a;
  wire signed[31:0] arrWData_a;
  arr_a arr_a(.*);

  assign arrWEnable_a =
    state == 4'd4 ? 32'd0 :
    state == 4'd6 ? 32'd0 :
    state == 4'd8 ? 32'd1 :
    'x;
  assign in0_Bin1 =
    state == 4'd6 ? reg0 :
    'x;
  assign in1_Bin1 =
    state == 4'd6 ? reg2 :
    'x;
  assign arrAddr_a =
    state == 4'd4 ? reg0 :
    state == 4'd6 ? reg0 :
    state == 4'd8 ? reg0 :
    'x;
  assign in0_Bin2 =
    state == 4'd7 ? reg1 :
    'x;
  assign in1_Bin2 =
    state == 4'd7 ? reg2 :
    'x;
  assign in0_Bin0 =
    state == 4'd1 ? reg0 :
    'x;
  assign in1_Bin0 =
    state == 4'd1 ? reg2 :
    'x;
  assign arrWData_a =
    state == 4'd8 ? reg1 :
    'x;

  always @(posedge clk) begin
    if(r_enable) begin
      state <= '0;
      linkreg <= '1;
      w_enable <= 1'd0;
      reg0 <= init_i;
      reg1 <= init_acc;
    end else begin
      case(state)
        '1: begin
          w_enable <= 1'd1;
          result <= reg0;
        end
        4'd5: state <= linkreg;
        4'd2: state <= reg2 ? 4'd4 : 4'd6;
        4'd8: state <= 4'd0;
        4'd6: state <= 4'd7;
        4'd4: state <= 4'd5;
        4'd1: state <= 4'd2;
        4'd7: state <= 4'd8;
        4'd0: state <= 4'd1;
      endcase
      case(state)
        4'd2: reg0 <= reg2 ? 32'd800 : reg0;
        4'd4: reg0 <= arrRData_a;
        4'd5: reg0 <= reg0;
        4'd8: reg0 <= reg3;
      endcase
      case(state)
        4'd7: reg1 <= out0_Bin2;
        4'd8: reg1 <= reg1;
      endcase
      case(state)
        4'd0: reg2 <= 32'd1000;
        4'd1: reg2 <= out0_Bin0;
        4'd2: reg2 <= reg2 ? reg2 : 32'd1;
        4'd6: reg2 <= arrRData_a;
      endcase
      case(state)
        4'd6: reg3 <= out0_Bin1;
      endcase
    end
  end
endmodule // main

module arr_a (
  input wire clk, arrWEnable_a,
  input wire[9:0] arrAddr_a,
  output wire signed[31:0] arrRData_a,
  input wire signed[31:0] arrWData_a
);
  integer i;
  initial begin
    for(i = 0; i < 1000; i = i + 1)
      mem[i] = i;
  end
  reg signed[31:0] mem [0:999];
  always @(posedge clk) begin
    if(arrWEnable_a) begin
      mem[arrAddr_a] <= arrWData_a;
    end
  end
  assign arrRData_a = /*arrWEnable_a ? 'x :*/ mem[arrAddr_a];
endmodule
`default_nettype wire
