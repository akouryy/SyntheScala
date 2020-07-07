package net.akouryy.synthescala
package fsmd

import cdfg.{bind, CDFG, CDFGFun, Jump}
import scala.collection.mutable

class Composer(
  graph: CDFG, sche: cdfg.schedule.Schedule,
  regs: bind.RegisterAllocator.Allocations, bindings: bind.Binder.Bindings,
):
  val fsm = mutable.SortedMap.empty[State, Transition]
  val datapath = mutable.Map.empty[ConnPort.Dst, mutable.SortedMap[State, Source]]

  def compose: FSMD =
    composeDatapath(graph.main)
    composeFSM(graph.main)
    FSMD(fsm, Datapath(datapath))

  private def blockFirstState(fn: CDFGFun, bi: cdfg.BlockIndex): State =
    val b = fn.blocks(bi)
    b.stateToNodes(sche).keySet.headOption
     .getOrElse(sche.jumpStates(b.outJump)(bi))

  private def composeFSM(fn: CDFGFun): Unit =
    for b <- fn.blocks.valuesIterator do
      for Seq(q1, q2) <- b.stateToNodes(sche).keySet.toList.sliding(2) do
        fsm(q1) = Transition.Always(q2)
      for q1 <- b.stateToNodes(sche).keySet.maxOption do
        fsm(q1) = Transition.Always(sche.jumpStates(b.outJump)(b.i))

    for j <- fn.jumps.valuesIterator do
      j match
        case _: Jump.Return =>
          for q1 <- sche.jumpStates(j.i).valuesIterator do
            fsm(q1) = Transition.LinkReg
        case _: Jump.TailCall =>
          for q1 <- sche.jumpStates(j.i).valuesIterator do
            fsm(q1) = Transition.Always(State(0))
        case _: (Jump.StartFun | Jump.Merge) =>
          val bi = j.outBlocks.soleElement
          for q1 <- sche.jumpStates(j.i).valuesIterator do
            fsm(q1) = Transition.Always(blockFirstState(fn, bi))
        case Jump.Branch(ji, cond, ibi, tbi, fbi) =>
          val q1 = sche.jumpStates(ji)(ibi)
          val port = new ConnPort.Reg(regs(cond))
          fsm(q1) = Transition.Conditional(
            datapath(port).getOrElse(q1, Source.Always(ConnPort.Inherit)), port,
            blockFirstState(fn, tbi), blockFirstState(fn, fbi),
          )
  end composeFSM

  private def mergeDatapath(pin: ConnPort.Dst, q: State, src: Source): Unit =
    import ConnPort._
    import Source._

    val newSrc =
      (datapath.getOrElseUpdate(pin, mutable.SortedMap.empty).get(q), src): @unchecked match
        case (None, _) => src
        case (Some(Conditional(reg, tru, Inherit)), Conditional(reg1, Inherit, fls))
        if reg == reg1 =>
          Conditional(reg, tru, fls)
        case (Some(Conditional(reg, Inherit, fls)), Conditional(reg1, tru, Inherit))
        if reg == reg1 =>
          Conditional(reg, tru, fls)

    datapath(pin)(q) = newSrc
  end mergeDatapath

  private def vc2connSrc(vc: VC): ConnPort.Src = vc match
    case VC.V(v) => new ConnPort.Reg(regs(v))
    case VC.C(c, _) => new ConnPort.Const(c)

  private def composeDatapath(fn: CDFGFun): Unit =
    for b <- fn.blocks.valuesIterator
        (nid -> node) <- b.nodes
    do
      import cdfg.Node._
      lazy val q = sche.nodeStates(b.i, nid)

      node match
        case Const(_, num, ident) =>
          import Jump._
          mergeDatapath(
            new ConnPort.Reg(regs(ident)),
            q,
            Source.Always(new ConnPort.Const(num))
          )
        case BinOp(_, _, l, r, a) =>
          val calc = bindings(b.i, nid)
          mergeDatapath(
            new ConnPort.CalcIn(calc.id, 0), q,
            Source.Always(vc2connSrc(l)),
          )
          mergeDatapath(
            new ConnPort.CalcIn(calc.id, 1), q,
            Source.Always(vc2connSrc(r)),
          )
          mergeDatapath(
            new ConnPort.Reg(regs(a)), q,
            Source.Always(new ConnPort.CalcOut(calc.id, 0)),
          )
        case GetReq(_, _, arr, index) =>
          mergeDatapath(
            new ConnPort.ArrWriteEnable(arr), q,
            Source.Always(new ConnPort.Const(0)),
          )
          mergeDatapath(
            new ConnPort.ArrIndex(arr), q,
            Source.Always(new ConnPort.Reg(regs(index))),
          )
        case GetAwa(_, _, arr, res) =>
          mergeDatapath(
            new ConnPort.Reg(regs(res)), q,
            Source.Always(new ConnPort.ArrReadValue(arr)),
          )
        case Put(_, arr, index, value) =>
          mergeDatapath(
            new ConnPort.ArrWriteEnable(arr), q,
            Source.Always(new ConnPort.Const(1)),
          )
          mergeDatapath(
            new ConnPort.ArrIndex(arr), q,
            Source.Always(new ConnPort.Reg(regs(index))),
          )
          mergeDatapath(
            new ConnPort.ArrWriteValue(arr), q,
            Source.Always(new ConnPort.Reg(regs(value))),
          )
        case _: Call => assert(false, "non-tail recursion")
    end for

    for j <- fn.jumps.valuesIterator do
      j match
        case Jump.Merge(ji, ibis, inLabss, _, outLabs) =>
          for
            (ibi, inLabs) <- ibis.zipStrict(inLabss)
            (inLab, outLab) <- inLabs.zipStrict(outLabs)
          do
            mergeDatapath(
              new ConnPort.Reg(regs(outLab)),
              sche.jumpStates(ji)(ibi),
              Source.Always(new ConnPort.Reg(regs(inLab))),
            )
        case Jump.TailCall(ji, _, params, ibi) =>
          for (param, i) <- params.zipWithIndex do
            mergeDatapath(
              new ConnPort.Reg(Register(i)),
              sche.jumpStates(ji)(ibi),
              Source.Always(new ConnPort.Reg(regs(param))),
            )
        case Jump.Return(ji, ident, ibi) =>
          mergeDatapath(
            new ConnPort.Reg(Register(0)),
            sche.jumpStates(ji)(ibi),
            Source.Always(new ConnPort.Reg(regs(ident))),
          )
        case _ =>
  end composeDatapath
end Composer
