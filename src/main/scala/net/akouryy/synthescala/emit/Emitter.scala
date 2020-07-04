package net.akouryy.synthescala
package emit

import cdfg.CDFG
import cdfg.bind.Binder.Bindings
import cdfg.bind.Calculator
import cdfg.bind.RegisterAllocator.Allocations
import fsmd._
import scala.collection.immutable

class Emitter(cdfg: CDFG, regs: Allocations, bindings: Bindings, fsmd: FSMD):
  val calculators = immutable.SortedMap.from:
    bindings.valuesIterator.toSeq.distinct.map(c => c.id -> c)

  private def lab2sv(lab: Label): String =
    lab.str.replace("_", "__").replace("@", "_a_")

  private def in(cal: Calculator, i: Int): String = s"in${i}_${cal.shortString}"
  private def out(cal: Calculator, i: Int): String = s"out${i}_${cal.shortString}"

  private def writeEnable(arr: Label): String = s"arrWEnable_${lab2sv(arr)}"
  private def index(arr: Label): String = s"arrAddr_${lab2sv(arr)}"
  private def readData(arr: Label): String = s"arrRData_${lab2sv(arr)}"
  private def writeData(arr: Label): String = s"arrWData_${lab2sv(arr)}"

  private def typ2sv(typ: toki.Type): String = typ match
    case toki.Type.U(w) => s"""[${w - 1}:0]"""
    case toki.Type.S(w) => s""" signed[${w - 1}:0]"""

  private def reg2sv(reg: Register): String = s"reg${reg.id}"

  private def connSrc2sv(src: ConnPort.Src, dst: ConnPort.Dst): String = src match
    case ConnPort.CalcOut(cid, port) => out(calculators(cid), port)
    case ConnPort.ArrReadValue(arr) => readData(arr)
    case ConnPort.Reg(reg) => reg2sv(reg)
    case ConnPort.Const(num) => s"32'd$num"
    case ConnPort.Inherit => connDst2sv(dst)

  private def connDst2sv(dst: ConnPort.Dst): String = dst match
    case ConnPort.CalcIn(cid, port) => in(calculators(cid), port)
    case ConnPort.ArrWriteEnable(arr) => writeEnable(arr)
    case ConnPort.ArrIndex(arr) => index(arr)
    case ConnPort.ArrWriteValue(arr) => writeData(arr)
    case dest: ConnPort.Reg => connSrc2sv(dest, dest)

  private def source2sv(source: Source, dst: ConnPort.Dst): String = source match
    case Source.Always(src) => connSrc2sv(src, dst)
    case Source.Conditional(reg, tru, fls) =>
      s"${reg2sv(reg)} ? ${connSrc2sv(tru, dst)} : ${connSrc2sv(fls, dst)}"

  def emit: String =
    val r = util.IndentedStringBuilder()
    val stateBitLen = (fsmd.fsm.keys.map(_.id).max + 1).width
    val regSet = immutable.SortedSet.from(regs.valuesIterator)

    // header
    r ++= "`default_nettype none"
    r ++= s"module main ("
    r.indent:
      r ++= "input wire clk, r_enable,"
      for p <- cdfg.main.params do
        r ++= s"input wire[63:0] init_${lab2sv(p)},"
      r ++= "output reg w_enable,"
      r ++= "output reg[63:0] result"
    r.indent(");", "endmodule // main"):

      // definitions
      r ++= s"reg[${stateBitLen - 1}:0] state;"
      r ++= s"reg[${stateBitLen - 1}:0] linkreg;"
      for reg <- regSet do
        r ++= s"reg[31:0] ${reg2sv(reg)};"
      r ++= ""

      // definitions: calculator ports
      for cal <- calculators.valuesIterator do
        val inputTypes =
          import Calculator._
          cal match
            case Bin(_, _, lt, rt) => Seq(s"wire${typ2sv(lt)}", s"wire${typ2sv(rt)}")
        val outputs =
          import Calculator._
          cal match
            case cal @ Bin(_, op @ (BinOp.Add | BinOp.Sub | BinOp.Mul | BinOp.Eq), _, _) =>
              Seq(
                s"wire${typ2sv(Bin.retTyp(cal))}" ->
                s"${in(cal, 0)} ${op.operatorString} ${in(cal, 1)}"
              )
            case _ => !!!(cal)
        for (t, i) <- inputTypes.zipWithIndex do
          r ++= s"$t ${in(cal, i)};"
        for ((t, expr), i) <- outputs.zipWithIndex do
          r ++= s"$t ${out(cal, i)} = $expr;"
      end for
      r ++= ""

      // definitions: array instances
      for toki.ArrayDef(arr, elemTyp, len) <- cdfg.arrayDefs.valuesIterator do
        r ++= s"wire ${writeEnable(arr)};"
        r ++= s"wire[${len.width - 1}:0] ${index(arr)};"
        r ++= s"wire${typ2sv(elemTyp)} ${readData(arr)};"
        r ++= s"wire${typ2sv(elemTyp)} ${writeData(arr)};"
        r ++= s"arr_${lab2sv(arr)} arr_${lab2sv(arr)}(.*);"
      r ++= ""

      // (calculator input and array index) port selectors
      for (dst: (
        ConnPort.CalcIn | ConnPort.ArrWriteEnable | ConnPort.ArrIndex | ConnPort.ArrWriteValue
      ), paths) <- fsmd.datapath.map do
        r.indent(s"assign ${connDst2sv(dst)} =", ""):
          for (state -> source) <- paths do
            r ++= f"state == $stateBitLen'd${state.id} ? ${source2sv(source, dst)} :"
          r ++= s"'x;"
      r ++= ""

      r.indent("always @(posedge clk) begin", "end"):
        // initializations
        r.indent("if(r_enable) begin", ""):
          r ++= "state <= '0;"
          r ++= "linkreg <= '1;"
          r ++= "w_enable <= 1'd0;"
          for (p, i) <- cdfg.main.params.zipWithIndex do
            r ++= s"${reg2sv(Register(i))} <= init_${lab2sv(p)};"
        r.indent("end else begin", "end"):
          // states
          r.indent("case(state)", "endcase"):
            r.indent("'1: begin", "end"):
              r ++= "w_enable <= 1'd1;"
              r ++= "result <= reg0;"
            for (State(q1) -> next) <- fsmd.fsm do
              r ++= s"$stateBitLen'd$q1: state <= " + next.match
                case Transition.Always(State(q2)) =>
                  s"$stateBitLen'd$q2;"
                case Transition.Conditional(reg, State(q2), State(q3)) =>
                  s"${reg2sv(reg)} ? $stateBitLen'd$q2 : $stateBitLen'd$q3;"
                case Transition.LinkReg =>
                  "linkreg;"

          // register selectors
          val regDatapath = immutable.SortedMap.from:
            for (ConnPort.Reg(reg), paths) <- fsmd.datapath.map
            yield (reg, paths)
          for (reg, paths) <- regDatapath do
            r.indent("case(state)", "endcase"):
              for (state -> source) <- paths do
                r ++= s"$stateBitLen'd${s"${state.id}:"} ${reg2sv(reg)} <= " +
                      s"${source2sv(source, new ConnPort.Reg(reg))};"

    // array modules
    /**
      * Using the result of
      *   https://github.com/PyHDI/veriloggen/blob/1465c93fef/examples/thread_matmul/Makefile.
      * Original: Copyright 2015, Shinya Takamaeda-Yamazaki and Contributors
      *   (Apache License 2.0 https://github.com/PyHDI/veriloggen/blob/1465c93fef/LICENSE)).
      * Modified by akouryy.
      */
    for toki.ArrayDef(arr, elemTyp, len) <- cdfg.arrayDefs.valuesIterator do
      r ++= ""
      r ++= s"module arr_${lab2sv(arr)} ("
      r.indent:
        r ++= s"input wire clk, ${writeEnable(arr)},"
        r ++= s"input wire[${len.width - 1}:0] ${index(arr)},"
        r ++= s"output wire${typ2sv(elemTyp)} ${readData(arr)},"
        r ++= s"input wire${typ2sv(elemTyp)} ${writeData(arr)}"
      r.indent(");", "endmodule"):

        r ++= "integer i;"
        r.indent("initial begin", "end"):
          r.indent(s"for(i = 0; i < $len; i = i + 1)", ""):
            r ++= "mem[i] = i;"

        r ++= s"reg${typ2sv(elemTyp)} mem [0:${len - 1}];";
        r.indent("always @(posedge clk) begin", "end"):
          r.indent(s"if(${writeEnable(arr)}) begin", "end"):
            r ++= s"mem[${index(arr)}] <= ${writeData(arr)};"
        r ++= s"assign ${readData(arr)} = /*${writeEnable(arr)} ? 'x :*/ mem[${index(arr)}];"

    r ++= "`default_nettype wire"
    r.toString
  end emit
end Emitter
