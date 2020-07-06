`default_nettype none
module main (
  input wire clk, r_enable, controlArr,
  input wire[63:0] init_i,
  output reg w_enable,
  output reg[63:0] result
);
  reg[3:0] state;
  reg[3:0] linkreg;
  reg[63:0] reg0;
  reg[63:0] reg1;
  reg[63:0] reg2;

  wire in0_Bin0;
  wire in1_Bin0;
  wire out0_Bin0 = in0_Bin0 == in1_Bin0;
  wire[1:0] in0_Bin1;
  wire[1:0] in1_Bin1;
  wire[1:0] out0_Bin1 = in0_Bin1 + in1_Bin1;


  assign in0_Bin1 =
    state == 4'd6 ? reg1[1:0] :
    'x;
  assign in1_Bin1 =
    state == 4'd6 ? reg0[1:0] :
    'x;
  assign in0_Bin0 =
    state == 4'd1 ? reg0[0:0] :
    'x;
  assign in1_Bin0 =
    state == 4'd1 ? reg2[0:0] :
    'x;


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
        4'd5: state <= 4'd6;
        4'd2: state <= reg0 ? 4'd4 : 4'd5;
        4'd6: state <= 4'd7;
        4'd4: state <= 4'd6;
        4'd1: state <= 4'd2;
        4'd7: state <= linkreg;
        4'd0: state <= 4'd1;
      endcase
      case(state)
        4'd1: reg0 <= {63'd0, out0_Bin0};
        4'd2: reg0 <= reg0 ? 64'd1 : 64'd2;
        4'd6: reg0 <= {62'd0, out0_Bin1};
        4'd7: reg0 <= reg0;
      endcase
      case(state)
        4'd0: reg1 <= 64'd1;
      endcase
      case(state)
        4'd0: reg2 <= 64'd0;
      endcase
    end
  end
endmodule // main
`default_nettype wire
