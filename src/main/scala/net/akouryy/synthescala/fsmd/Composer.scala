package net.akouryy.synthescala
package fsmd

import cdfg.{bind, CDFG, CDFGFun, Jump}
import scala.collection.mutable

class Composer(
  graph: CDFG, sche: cdfg.schedule.Schedule,
  regs: bind.RegisterAllocator.Allocations, bindings: bind.Binder.Bindings,
):
  val fsm = mutable.Map.empty[State, Transition]
  val datapath = mutable.Map.empty[ConnPort.Dst, mutable.SortedMap[State, Source]]

  def compose: FSMD =
    composeFSM(graph.main)
    composeDatapath(graph.main)
    FSMD(fsm, Datapath(datapath))

  private def blockFirstState(fn: CDFGFun, bi: cdfg.BlockIndex) =
    fn.blocks(bi).stateToNodes(sche).keySet.head

  private def composeFSM(fn: CDFGFun): Unit =
    for
      b <- fn.blocks.valuesIterator
      Seq(q1, q2) <- b.stateToNodes(sche).keySet.toList.sliding(2)
    do
      fsm(q1) = Transition.Always(q2)

    for j <- fn.jumps.valuesIterator do
      j match
        case _: Jump.Return =>
          for q1 <- sche.jumpStates(j.i) do
            fsm(q1) = Transition.LinkReg
        case _: Jump.TailCall =>
          for q1 <- sche.jumpStates(j.i) do
            fsm(q1) = Transition.Always(State(0))
        case _: (Jump.StartFun | Jump.Merge) =>
          val bi = j.outBlocks.soleElement
          for q1 <- sche.jumpStates(j.i) do
            fsm(q1) = Transition.Always(blockFirstState(fn, bi))
        case Jump.Branch(ji, cond, _, tbi, fbi) =>
          val q1 = sche.jumpStates(ji).soleElement
          fsm(q1) = Transition.Conditional(
            regs(cond), blockFirstState(fn, tbi), blockFirstState(fn, fbi),
          )
  end composeFSM

  private def mergeDatapath(pin: ConnPort.Dst, q: State, src: Source): Unit =
    import ConnPort._
    import Source._

    val newSrc =
      (datapath.getOrElseUpdate(pin, mutable.SortedMap.empty).get(q), src) match
        case (None, _) => src
        case (Some(Conditional(reg, tru, Inherit)), Conditional(reg1, Inherit, fls))
        if reg == reg1 =>
          Conditional(reg, tru, fls)
        case (Some(Conditional(reg, Inherit, fls)), Conditional(reg1, tru, Inherit))
        if reg == reg1 =>
          Conditional(reg, tru, fls)
        case _ => ???

    datapath(pin)(q) = newSrc

  private def composeDatapath(fn: CDFGFun): Unit =
    for b <- fn.blocks.valuesIterator
        node <- b.nodes
    do
      import cdfg.Node._
      lazy val q = sche.nodeStates(b.i, node)

      node match
        case _: (Input | Output) => // nothing to do
        case Const(num, ident) =>
          import Jump._
          fn.jumps(b.inJumpIndex) match
            case Branch(ji1, cond, _, tru, fls) =>
              mergeDatapath(
                new ConnPort.Reg(regs(ident)),
                sche.jumpStates(ji1).soleElement,
                Source.Conditional(
                  regs(cond),
                  if tru == b.i then new ConnPort.Const(num) else ConnPort.Inherit,
                  if fls == b.i then new ConnPort.Const(num) else ConnPort.Inherit,
                )
              )
            case j1 =>
              for q <- sche.jumpStates(j1.i) do
                mergeDatapath(
                  new ConnPort.Reg(regs(ident)),
                  q,
                  Source.Always(new ConnPort.Const(num))
                )
        case BinOp(_, l, r, a) =>
          val calc = bindings(b.i, node)
          mergeDatapath(
            new ConnPort.CalcIn(calc.id, 0), q,
            Source.Always(new ConnPort.Reg(regs(l))),
          )
          mergeDatapath(
            new ConnPort.CalcIn(calc.id, 1), q,
            Source.Always(new ConnPort.Reg(regs(r))),
          )
          mergeDatapath(
            new ConnPort.Reg(regs(a)), q,
            Source.Always(new ConnPort.CalcOut(calc.id, 0)),
          )
        case Get(arr, index, res) =>
          mergeDatapath(
            new ConnPort.ArrReadIndex(arr), q,
            Source.Always(new ConnPort.Reg(regs(index))),
          )
          mergeDatapath(
            new ConnPort.Reg(regs(res)), q,
            Source.Always(new ConnPort.ArrReadValue(arr)),
          )
        case _: Call => !!!(node)
    end for

    for j <- fn.jumps.valuesIterator do
      j match
        case Jump.TailCall(ji, _, params, _) =>
          for (param, i) <- params.zipWithIndex do
            mergeDatapath(
              new ConnPort.Reg(Register(i)),
              sche.jumpStates(ji).soleElement,
              Source.Always(new ConnPort.Reg(regs(param))),
            )
        case Jump.Return(ji, ident, _) =>
          mergeDatapath(
            new ConnPort.Reg(Register(0)),
            sche.jumpStates(ji).soleElement,
            Source.Always(new ConnPort.Reg(regs(ident))),
          )
        case _ =>
  end composeDatapath
end Composer
