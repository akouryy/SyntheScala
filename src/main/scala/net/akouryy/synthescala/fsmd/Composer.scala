package net.akouryy.synthescala
package fsmd

import cdfg.Jump
import scala.collection.mutable

class Composer(
  graph: cdfg.CDFG, sche: cdfg.schedule.Schedule,
  regs: cdfg.bind.RegisterAllocator.Allocations, bindings: cdfg.bind.Binder.Bindings,
):
  val fsm = mutable.Map.empty[State, Transition]

  def compose: FSMD =
    composeFSM()
    FSMD(fsm, DataPath())

  private def blockFirstState(bi: cdfg.BlockIndex) =
    graph.blocks(bi).stateToNodes(sche).keySet.head

  def composeFSM(): Unit =
    for
      b <- graph.blocks.valuesIterator
      Seq(q1, q2) <- b.stateToNodes(sche).keySet.toList.sliding(2)
    do
      fsm(q1) = Transition.Always(q2)

    for j <- graph.jumps.valuesIterator do
      j match
        case _: Jump.Return =>
          for q1 <- sche.jumpStates(j.i) do
            fsm(q1) = Transition.LinkReg
        case _: (Jump.StartFun | Jump.Merge) =>
          val Seq(bi) = j.outBlocks: @unchecked
          for q1 <- sche.jumpStates(j.i) do
            fsm(q1) = Transition.Always(blockFirstState(bi))
        case Jump.Branch(ji, cond, _, tbi, fbi) =>
          val Seq(q1) = sche.jumpStates(ji).toSeq: @unchecked
          fsm(q1) = Transition.Conditional(
            regs(cond), blockFirstState(tbi), blockFirstState(fbi),
          )
