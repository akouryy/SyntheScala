`default_nettype none
module main (
  input wire clk, r_enable, controlArr,
  input wire[9:0] init_a,
  input wire[9:0] init_b,
  input wire[9:0] init_c,
  input wire[9:0] init_d,
  input wire[12:0] init_e,
  input wire[9:0] init_f,
  input wire[12:0] init_g,
  output reg w_enable,
  output reg[12:0] result
);
  reg[2:0] state;
  reg[2:0] linkreg;
  reg[63:0] reg0;
  reg[63:0] reg1;
  reg[63:0] reg2;
  reg[63:0] reg3;
  reg[63:0] reg4;
  reg[63:0] reg5;
  reg[63:0] reg6;

  wire[12:0] in0_Bin0;
  wire[12:0] in1_Bin0;
  wire[12:0] out0_Bin0 = in0_Bin0 + in1_Bin0;
  wire[9:0] in0_Bin1;
  wire[12:0] in1_Bin1;
  wire[12:0] out0_Bin1 = in0_Bin1 + in1_Bin1;


  assign in0_Bin1 =
    state == 3'd1 ? reg3[9:0] :
    'x;
  assign in1_Bin1 =
    state == 3'd1 ? reg4[12:0] :
    'x;
  assign in0_Bin0 =
    state == 3'd1 ? reg5[12:0] :
    state == 3'd2 ? reg4[12:0] :
    state == 3'd3 ? reg2[12:0] :
    state == 3'd4 ? reg1[12:0] :
    state == 3'd5 ? reg0[12:0] :
    'x;
  assign in1_Bin0 =
    state == 3'd1 ? reg6[12:0] :
    state == 3'd2 ? reg3[12:0] :
    state == 3'd3 ? reg3[12:0] :
    state == 3'd4 ? reg2[12:0] :
    state == 3'd5 ? reg1[12:0] :
    'x;


  always @(posedge clk) begin
    if(r_enable) begin
      state <= '0;
      linkreg <= '1;
      w_enable <= 1'd0;
      reg0 <= {54'd0, init_a};
      reg1 <= {54'd0, init_b};
      reg2 <= {54'd0, init_c};
      reg3 <= {54'd0, init_d};
      reg4 <= {51'd0, init_e};
      reg5 <= {54'd0, init_f};
      reg6 <= {51'd0, init_g};
    end else begin
      case(state)
        '1: begin
          w_enable <= 1'd1;
          result <= reg0[12:0];
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
        3'd5: reg0 <= {51'd0, out0_Bin0};
        3'd6: reg0 <= reg0;
      endcase
      case(state)
        3'd4: reg1 <= {51'd0, out0_Bin0};
      endcase
      case(state)
        3'd3: reg2 <= {51'd0, out0_Bin0};
      endcase
      case(state)
        3'd1: reg3 <= {51'd0, out0_Bin0};
        3'd2: reg3 <= {51'd0, out0_Bin0};
      endcase
      case(state)
        3'd1: reg4 <= {51'd0, out0_Bin1};
      endcase
    end
  end
endmodule // main
`default_nettype wire
