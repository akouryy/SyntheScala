`default_nettype none
module main (
  input wire clk, r_enable,
  input wire[63:0] init_a,
  input wire[63:0] init_b,
  input wire[63:0] init_c,
  input wire[63:0] init_d,
  input wire[63:0] init_e,
  input wire[63:0] init_f,
  input wire[63:0] init_g,
  output reg w_enable,
  output reg[63:0] result
);
  reg[2:0] state;
  reg[2:0] linkreg;
  reg[31:0] reg2;
  reg[31:0] reg5;
  reg[31:0] reg4;
  reg[31:0] reg6;
  reg[31:0] reg3;
  reg[31:0] reg1;
  reg[31:0] reg0;

  wire[9:0] in0_Bin0;
  wire[9:0] in1_Bin0;
  wire[9:0] out0_Bin0 = in0_Bin0 + in1_Bin0;
  wire[9:0] in0_Bin1;
  wire[9:0] in1_Bin1;
  wire[9:0] out0_Bin1 = in0_Bin1 + in1_Bin1;


  assign in0_Bin1 =
    state == 3'd1 ? reg5 :
    'x;
  assign in1_Bin1 =
    state == 3'd1 ? reg6 :
    'x;
  assign in0_Bin0 =
    state == 3'd1 ? reg3 :
    state == 3'd2 ? reg3 :
    state == 3'd3 ? reg2 :
    state == 3'd4 ? reg1 :
    state == 3'd5 ? reg0 :
    'x;
  assign in1_Bin0 =
    state == 3'd1 ? reg4 :
    state == 3'd2 ? reg4 :
    state == 3'd3 ? reg3 :
    state == 3'd4 ? reg2 :
    state == 3'd5 ? reg1 :
    'x;

  always @(posedge clk) begin
    if(r_enable) begin
      state <= '0;
      linkreg <= '1;
      w_enable <= 1'd0;
      reg0 <= init_a;
      reg1 <= init_b;
      reg2 <= init_c;
      reg3 <= init_d;
      reg4 <= init_e;
      reg5 <= init_f;
      reg6 <= init_g;
    end else begin
      case(state)
        '1: begin
          w_enable <= 1'd1;
          result <= reg0;
        end
        3'd5: state <= 3'd6;
        3'd2: state <= 3'd3;
        3'd6: state <= linkreg;
        3'd3: state <= 3'd4;
        3'd4: state <= 3'd5;
        3'd1: state <= 3'd2;
        3'd0: state <= 3'd1;
      endcase
      case(state)
        3'd5: reg0 <= out0_Bin0;
        3'd6: reg0 <= reg0;
      endcase
      case(state)
        3'd4: reg1 <= out0_Bin0;
      endcase
      case(state)
        3'd3: reg2 <= out0_Bin0;
      endcase
      case(state)
        3'd1: reg3 <= out0_Bin0;
        3'd2: reg3 <= out0_Bin0;
      endcase
      case(state)
        3'd1: reg4 <= out0_Bin1;
      endcase
    end
  end
endmodule // main
`default_nettype wire
