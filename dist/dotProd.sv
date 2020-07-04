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
  reg[31:0] reg3;
  reg[31:0] reg2;
  reg[31:0] reg0;
  reg[31:0] reg1;

  wire[9:0] in0_Bin0;
  wire[9:0] in1_Bin0;
  wire[0:0] out0_Bin0 = in0_Bin0 == in1_Bin0;
  wire[9:0] in0_Bin1;
  wire[0:0] in1_Bin1;
  wire[9:0] out0_Bin1 = in0_Bin1 + in1_Bin1;
  wire signed[63:0] in0_Bin2;
  wire signed[31:0] in1_Bin2;
  wire signed[63:0] out0_Bin2 = in0_Bin2 * in1_Bin2;
  wire signed[63:0] in0_Bin3;
  wire signed[63:0] in1_Bin3;
  wire signed[63:0] out0_Bin3 = in0_Bin3 + in1_Bin3;

  wire[9:0] arrRaddr_a;
  wire signed[31:0] arrRdata_a;
  arr_a arr_a(.*);
  wire[9:0] arrRaddr_b;
  wire signed[31:0] arrRdata_b;
  arr_b arr_b(.*);

  assign in0_Bin1 =
    state == 4'd5 ? reg0 :
    'x;
  assign arrRaddr_b =
    state == 4'd5 ? reg0 :
    'x;
  assign in1_Bin1 =
    state == 4'd5 ? reg2 :
    'x;
  assign in0_Bin2 =
    state == 4'd6 ? reg2 :
    'x;
  assign in1_Bin2 =
    state == 4'd6 ? reg3 :
    'x;
  assign in0_Bin0 =
    state == 4'd1 ? reg0 :
    'x;
  assign in1_Bin0 =
    state == 4'd1 ? reg2 :
    'x;
  assign in0_Bin3 =
    state == 4'd7 ? reg1 :
    'x;
  assign arrRaddr_a =
    state == 4'd5 ? reg0 :
    'x;
  assign in1_Bin3 =
    state == 4'd7 ? reg2 :
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
        4'd5: state <= 4'd6;
        4'd2: state <= reg2 ? 4'd4 : 4'd5;
        4'd8: state <= 4'd0;
        4'd6: state <= 4'd7;
        4'd4: state <= linkreg;
        4'd1: state <= 4'd2;
        4'd7: state <= 4'd8;
        4'd0: state <= 4'd1;
      endcase
      case(state)
        4'd4: reg0 <= reg1;
        4'd5: reg0 <= out0_Bin1;
        4'd8: reg0 <= reg0;
      endcase
      case(state)
        4'd7: reg1 <= out0_Bin3;
        4'd8: reg1 <= reg1;
      endcase
      case(state)
        4'd0: reg2 <= 32'd1000;
        4'd1: reg2 <= out0_Bin0;
        4'd2: reg2 <= reg2 ? reg2 : 32'd1;
        4'd5: reg2 <= arrRdata_a;
        4'd6: reg2 <= out0_Bin2;
      endcase
      case(state)
        4'd5: reg3 <= arrRdata_b;
      endcase
    end
  end
endmodule // main

module arr_a (
  input wire clk,
  input wire[9:0] arrRaddr_a,
  output wire signed[31:0] arrRdata_a
);
  reg signed[31:0] mem [0:999];
  integer i;
  initial begin
    for(i = 0; i < 1000; i = i + 1)
      mem[i] = i;
  end
  assign arrRdata_a = mem[arrRaddr_a];
endmodule

module arr_b (
  input wire clk,
  input wire[9:0] arrRaddr_b,
  output wire signed[31:0] arrRdata_b
);
  reg signed[31:0] mem [0:999];
  integer i;
  initial begin
    for(i = 0; i < 1000; i = i + 1)
      mem[i] = i;
  end
  assign arrRdata_b = mem[arrRaddr_b];
endmodule
`default_nettype wire
