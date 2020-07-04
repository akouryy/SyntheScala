`default_nettype none
module main (
  input wire clk, r_enable,
  input wire[63:0] init_n,
  input wire[63:0] init_a,
  input wire[63:0] init_b,
  output reg w_enable,
  output reg[63:0] result
);
  reg[3:0] state;
  reg[3:0] linkreg;
  reg[31:0] reg2;
  reg[31:0] reg4;
  reg[31:0] reg3;
  reg[31:0] reg1;
  reg[31:0] reg0;

  wire[31:0] in0_Bin0;
  wire[31:0] in1_Bin0;
  wire[31:0] out0_Bin0 = in0_Bin0 + in1_Bin0;
  wire[5:0] in0_Bin1;
  wire[0:0] in1_Bin1;
  wire[0:0] out0_Bin1 = in0_Bin1 == in1_Bin1;
  wire[5:0] in0_Bin2;
  wire[0:0] in1_Bin2;
  wire[5:0] out0_Bin2 = in0_Bin2 - in1_Bin2;


  assign in0_Bin1 =
    state == 4'd2 ? reg0 :
    'x;
  assign in1_Bin1 =
    state == 4'd2 ? reg3 :
    'x;
  assign in0_Bin2 =
    state == 4'd6 ? reg0 :
    'x;
  assign in1_Bin2 =
    state == 4'd6 ? reg3 :
    'x;
  assign in0_Bin0 =
    state == 4'd1 ? reg0 :
    state == 4'd6 ? reg1 :
    'x;
  assign in1_Bin0 =
    state == 4'd1 ? reg4 :
    state == 4'd6 ? reg2 :
    'x;

  always @(posedge clk) begin
    if(r_enable) begin
      state <= '0;
      linkreg <= '1;
      w_enable <= 1'd0;
      reg0 <= init_n;
      reg1 <= init_a;
      reg2 <= init_b;
    end else begin
      case(state)
        '1: begin
          w_enable <= 1'd1;
          result <= reg0;
        end
        4'd5: state <= linkreg;
        4'd2: state <= 4'd3;
        4'd6: state <= 4'd7;
        4'd3: state <= reg3 ? 4'd5 : 4'd6;
        4'd1: state <= 4'd2;
        4'd7: state <= 4'd0;
        4'd0: state <= 4'd1;
      endcase
      case(state)
        4'd1: reg0 <= out0_Bin0;
        4'd5: reg0 <= reg1;
        4'd6: reg0 <= out0_Bin2;
        4'd7: reg0 <= reg0;
      endcase
      case(state)
        4'd7: reg1 <= reg2;
      endcase
      case(state)
        4'd6: reg2 <= out0_Bin0;
        4'd7: reg2 <= reg1;
      endcase
      case(state)
        4'd0: reg3 <= 32'd0;
        4'd2: reg3 <= out0_Bin1;
        4'd3: reg3 <= reg3 ? reg3 : 32'd1;
      endcase
      case(state)
        4'd0: reg4 <= 32'd0;
      endcase
    end
  end
endmodule // main
`default_nettype wire
