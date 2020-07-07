package net.akouryy.synthescala
package emit

import cdfg.CDFG
import cdfg.bind.Binder.Bindings
import cdfg.bind.Calculator
import cdfg.bind.RegisterAllocator.Allocations
import fsmd._
import toki.Type
import scala.collection.{immutable, mutable}

class Emitter(cdfg: CDFG, regs: Allocations, bindings: Bindings, fsmd: FSMD):
  private val calculators = immutable.SortedMap.from:
    bindings.valuesIterator.toSeq.distinct.map(c => c.id -> c)

  private val varTyps = mutable.Map.empty[String, Type]

  private def lab2sv(lab: Label): String =
    lab.str.replace("_", "__").replace("@", "_a_")

  private def in(cal: Calculator, i: Int): String = s"in${i}_${cal.shortString}"
  private def out(cal: Calculator, i: Int): String = s"out${i}_${cal.shortString}"

  private def writeEnable(arr: Label): String = s"arrWEnable_${lab2sv(arr)}"
  private def index(arr: Label): String = s"arrAddr_${lab2sv(arr)}"
  private def readData(arr: Label): String = s"arrRData_${lab2sv(arr)}"
  private def writeData(arr: Label): String = s"arrWData_${lab2sv(arr)}"
  private def ctrl(s: String) = s"control${s.capitalize}"

  private def typ2sv(typ: Type): String = typ match
    case Type.U(1) => ""
    case Type.U(w) => s"""[${w - 1}:0]"""
    case Type.S(w) => s""" signed[${w - 1}:0]"""

  private def reg2sv(reg: Register): String = reg match
    case Register.STATE => s"stateR"
    case _ => s"reg${reg.id}"

  private def convBitWidth(typs: (Type, Type), v: String): String =
    import Type._
    typs match
      case f -> t if f == t => v
      case U(fw) -> U(tw) if fw < tw => s"""{${tw - fw}'d0, $v}"""
      case U(fw) -> U(tw) if fw > tw => s"""$v[${tw - 1}:0]"""
      case S(fw) -> S(tw) if fw < tw => s"""{{${tw - fw}{$v[${fw - 1}]}}, $v}"""
      case S(fw) -> S(tw) if fw > tw => s"""{$v[${fw - 1}], $v[${tw - 1}:0]}"""
      case U(fw) -> S(tw) => // read from register
        convBitWidth(S(fw) -> S(tw), v)
      case S(fw) -> U(tw) => // write to register
        convBitWidth(S(fw) -> S(tw), v)
      case _ => !!!(typs)
  end convBitWidth

  private def (v: String) :> (toTyp: Option[Type]): String = toTyp match
    case Some(toTyp) => convBitWidth(varTyps(v) -> toTyp, v)
    case None => v

  private def connSrc2sv(src: ConnPort.Src, dst: ConnPort.Dst, reqTyp: Option[Type]): String =
    src match
      case ConnPort.CalcOut(cid, port) => out(calculators(cid), port) :> reqTyp
      case ConnPort.ArrReadValue(arr) => readData(arr) :> reqTyp
      case ConnPort.Reg(reg) => reg2sv(reg) :> reqTyp
      case ConnPort.Const(num) =>
        def base(w: Int) = if num >= 0 then s"$w'd$num" else s"(-$$signed($w'd${num.abs}))"
        reqTyp.get match
        case Type.U(w) => if num >= 0 then base(w) else s"$$unsigned${base(w)}"
        case Type.S(w) => if num >= 0 then s"$$signed(${base(w)})" else base(w)
      case ConnPort.Inherit => connDst2sv(dst, reqTyp)

  private def connDst2sv(dst: ConnPort.Dst, reqTyp: Option[Type]): String = dst match
    case ConnPort.CalcIn(cid, port) => in(calculators(cid), port) :> reqTyp
    case ConnPort.ArrWriteEnable(arr) => writeEnable(arr) :> reqTyp
    case ConnPort.ArrIndex(arr) => index(arr)
    case ConnPort.ArrWriteValue(arr) => writeData(arr)
    case dest: ConnPort.Reg => connSrc2sv(dest, dest, reqTyp)

  private def source2sv(source: Source, dst: ConnPort.Dst, reqTyp: Option[Type]): String =
    source match
      case Source.Always(src) => connSrc2sv(src, dst, reqTyp)
      case Source.Conditional(reg, tru, fls) =>
        s"${reg2sv(reg)} ? ${connSrc2sv(tru, dst, reqTyp)} : ${connSrc2sv(fls, dst, reqTyp)}"

  private def defReg(v: String): String = s"reg${typ2sv(varTyps(v))} $v;"
  private def defWire(v: String): String = s"wire${typ2sv(varTyps(v))} $v;"
  private def defWire(v: String, bound: String): String = s"wire${typ2sv(varTyps(v))} $v = $bound;"

  def emit: String =
    val r = util.IndentedStringBuilder()
    val stateBitLen = (fsmd.fsm.keys.map(_.id).max + 1).width
    val regSet = immutable.SortedSet.from(regs.valuesIterator)

    // header
    r ++= "`default_nettype none"
    r ++= s"module main ("
    r.indent:
      r ++= "input wire clk, r_enable, controlArr,"
      for toki.Entry(param, paramTyp) <- cdfg.main.params do
        r ++= s"input wire${typ2sv(paramTyp)} init_${lab2sv(param)},"
      for toki.ArrayDef(arr, elemTyp, len) <- cdfg.arrayDefs.valuesIterator do
        r ++= s"input wire ${ctrl(writeEnable(arr))},"
        r ++= s"input wire[${len.width - 1}:0] ${ctrl(index(arr))},"
        r ++= s"output wire${typ2sv(elemTyp)} ${ctrl(readData(arr))},"
        r ++= s"input wire${typ2sv(elemTyp)} ${ctrl(writeData(arr))},"
      r ++= "output reg w_enable,"
      r ++= s"output reg${typ2sv(cdfg.main.retTyp)} result"
    r.indent(");", "endmodule // main"):

      // definitions
      r ++= s"reg[${stateBitLen - 1}:0] ${reg2sv(Register.STATE)};"
      r ++= s"reg[${stateBitLen - 1}:0] linkreg;"
      for reg <- regSet do
        val name = reg2sv(reg)
        varTyps(name) = Type.U(64)
        r ++= defReg(name)
      r ++= ""

      // definitions: calculator ports
      for cal <- calculators.valuesIterator do
        val inputTyps =
          import Calculator._
          cal match
            case Bin(_, _, lt, rt) => Seq(lt, rt)
        val outputs =
          import Calculator._
          (cal: @unchecked) match
            case cal @ Bin(_, op, _, _) =>
              Seq(
                Bin.retTyp(cal) ->
                s"${in(cal, 0)} ${op.operatorString} ${in(cal, 1)}"
              )
        for (t, i) <- inputTyps.zipWithIndex do
          val name = in(cal, i)
          varTyps(name) = t
          r ++= defWire(name)
        for ((t, expr), i) <- outputs.zipWithIndex do
          val name = out(cal, i)
          varTyps(name) = t
          r ++= defWire(name, expr)
      end for
      r ++= ""

      // definitions: array instances
      for toki.ArrayDef(arr, elemTyp, len) <- cdfg.arrayDefs.valuesIterator do
        varTyps(writeEnable(arr)) = Type.U(1)
        varTyps(index(arr)) = Type.U(len.width)
        varTyps(readData(arr)) = elemTyp
        varTyps(writeData(arr)) = elemTyp
        r ++= defWire(writeEnable(arr))
        r ++= defWire(index(arr))
        r ++= defWire(readData(arr))
        r ++= defWire(writeData(arr))
        r ++= s"arr_${lab2sv(arr)} arr_${lab2sv(arr)}(.*);"
      r ++= ""

      // calculator input port selectors
      for (dst: ConnPort.CalcIn, paths) <- fsmd.datapath.map do
        val Calculator.Bin(_, _, lt, rt) = calculators(dst.id)
        val name = connDst2sv(dst, None)
        val typ = Some(varTyps(name))
        r.indent(s"assign $name =", ""):
          for (state -> source) <- paths do
            r ++= s"${reg2sv(Register.STATE)} == $stateBitLen'd${state.id} ? "
                + s"${source2sv(source, dst, typ)} :"
          r ++= s"'x;"
      r ++= ""

      // array port selectors
      for toki.ArrayDef(arr, elemTyp, len) <- cdfg.arrayDefs.valuesIterator do
        import ConnPort._
        for dst <- Seq(new ArrWriteEnable(arr), new ArrIndex(arr), new ArrWriteValue(arr)) do
          val name = connDst2sv(dst, None)
          val typ = Some(varTyps(name))
          r.indent(s"assign $name =", ""):
            r ++= s"controlArr ? ${ctrl(name)} :"
            for
              paths <- fsmd.datapath.map.get(dst)
              (state -> source) <- paths
            do
              r ++= s"${reg2sv(Register.STATE)} == $stateBitLen'd${state.id} ? " +
                    s"${source2sv(source, dst, typ)} :"
            dst match
              case _: ArrWriteEnable => r ++= s"1'd0;"
              case _ => r ++= s"'x;"
        val rd = readData(arr)
        r ++= s"assign ${ctrl(rd)} = controlArr ? ${rd} : 'x;"
      r ++= ""

      r.indent("always @(posedge clk) begin", "end"):
        // initializations
        r.indent("if(r_enable) begin", ""):
          r ++= s"${reg2sv(Register.STATE)} <= '0;"
          r ++= "linkreg <= '1;"
          r ++= "w_enable <= 1'd0;"
          for (toki.Entry(p, pt), i) <- cdfg.main.params.zipWithIndex do
            r ++= s"${reg2sv(Register(i))} <= " +
                  s"${convBitWidth(pt -> Type.U(64), s"init_${lab2sv(p)}")};"
        r.indent("end else begin", "end"):
          // states
          r.indent(s"case(${reg2sv(Register.STATE)})", "endcase"):
            r.indent("'1: begin", "end"):
              r ++= "w_enable <= 1'd1;"
              r ++= s"result <= ${convBitWidth(Type.U(64) -> cdfg.main.retTyp, "reg0")};"
            for (State(q1) -> next) <- fsmd.fsm do
              r ++= s"$stateBitLen'd$q1: ${reg2sv(Register.STATE)} <= " + next.match
                case Transition.Always(State(q2)) =>
                  s"$stateBitLen'd$q2;"
                case Transition.Conditional(cond, condReg, State(q2), State(q3)) =>
                  s"(${source2sv(cond, condReg, None)}) ? " +
                  s"$stateBitLen'd$q2 : $stateBitLen'd$q3;"
                case Transition.LinkReg =>
                  "linkreg;"

          // register selectors
          val regDatapath = immutable.SortedMap.from:
            for (ConnPort.Reg(reg), paths) <- fsmd.datapath.map
            yield (reg, paths)
          for (reg, paths) <- regDatapath do
            val name = reg2sv(reg)
            val typ = Some(varTyps(name))
            r.indent(s"case(${reg2sv(Register.STATE)})", "endcase"):
              for (state -> source) <- paths do
                r ++= s"$stateBitLen'd${s"${state.id}:"} $name <= " +
                      s"${source2sv(source, new ConnPort.Reg(reg), typ)};"

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

        r ++= s"reg[${len.width - 1}:0] delayedRAddr;";
        r ++= s"reg${typ2sv(elemTyp)} mem [0:${len - 1}];";
        r.indent("always @(posedge clk) begin", "end"):
          r.indent(s"if(${writeEnable(arr)}) begin", "end"):
            r ++= s"mem[${index(arr)}] <= ${writeData(arr)};"
          r ++= s"delayedRAddr <= ${writeEnable(arr)} ? 'x : ${index(arr)};"
        r ++= s"assign ${readData(arr)} = mem[delayedRAddr];"

    r ++= "`default_nettype wire"
    r.toString
  end emit
end Emitter
