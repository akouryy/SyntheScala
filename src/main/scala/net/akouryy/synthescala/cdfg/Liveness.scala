package net.akouryy.synthescala
package cdfg

import scala.collection.mutable

object Liveness:
  def insertInOuts(prog: CDFG): Unit =
    val jis = mutable.Stack.from:
      prog.main.jumps.iterator.filter(_._2.outBlocks.isEmpty).map(_._1)

    val visited = mutable.Set.empty[BlockIndex]
    val liveIns = mutable.Map.empty[BlockIndex, Set[Label]]

    while jis.nonEmpty do
      val ji = jis.pop()
      val jump = prog.main.jumps(ji)
      if jump.outBlocks.forall(visited)
        val liveOutBase = jump.outBlocks.toSet.flatMap(liveIns)
        for
          bi <- jump.inBlocks
          if !visited(bi)
        do
          val block = prog.main.blocks(bi)
          visited += bi
          jis += block.inJumpIndex
          val liveOut = (jump: @unchecked) match
            case Jump.Branch(_, cond, _, _, _) => liveOutBase + cond
            case Jump.Merge(_, ibs, ins, _, ons) => liveOutBase -- ons ++ ins(ibs.indexOf(bi))
            case Jump.Return(_, v, _) => liveOutBase + v
            case Jump.ForLoopTop(_, _, cond, _, _, _, _, _) => liveOutBase + cond
            case Jump.ForLoopBottom(_, _, _, names) => liveOutBase ++ names

          liveIns(bi) = liveOut ++ block.uses -- block.defs

          prog.main.blocks(bi) = block.copy(
            inputs = liveIns(bi).toSeq,
            outputs = liveOut.toSeq,
          )
  end insertInOuts

  def liveInsForState(graph: CDFGFun, sche: schedule.Schedule)
  : collection.Map[JNState, Set[Label]] =
    val ret = mutable.Map.empty[JNState, Set[Label]]

    for (bi -> b) <- graph.blocks do
      var live = Set.from(b.outputs)
      locally:
        val st = JNState(isJump = true, sche.jumpStates(b.outJump)(bi))
        graph(b.outJump) match
          case Jump.ForLoopTop(_, _, _, _, _, _, _, topNames) =>
            live ++= topNames // TODO: define topNames properly to remove this statement
          case _ =>
        ret(st) = live ++ ret.getOrElse(st, Nil)

      for (state -> nodes) <- b.stateToNodes(sche).sets.toSeq.reverseIterator do
        for node <- nodes do
          live ++= node.read
          live --= node.written
        ret(JNState(isJump = false, state)) = live

    ret
  end liveInsForState

end Liveness
