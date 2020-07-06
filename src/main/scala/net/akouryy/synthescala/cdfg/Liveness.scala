package net.akouryy.synthescala
package cdfg

import scala.collection.mutable

object Liveness:
  def insertInOuts(prog: CDFG): Unit =
    val jis = prog.main.jumps.filter(_._2.isInstanceOf[Jump.Return]).keysIterator.to:
      import scala.language.implicitConversions
      mutable.Stack

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

          liveIns(bi) = liveOut ++ block.uses -- block.defs

          prog.main.blocks(bi) = block.copy(
            nodes = block.nodes ++
                    liveIns(bi).map(Node.Input(NodeID.generate(), _).withID) ++
                    liveOut.map(Node.Output(NodeID.generate(), _).withID)
          )
  end insertInOuts

  def liveInsForState(graph: CDFGFun, sche: schedule.Schedule): collection.Map[State, Set[Label]] =
    val ret = mutable.Map.empty[State, Set[Label]]

    /*for (ji -> j) <- graph.jumps do
      j match
        case Jump.Branch(_, cond, _, _, obi) =>
          val sc = sche.jumpStates(ji)
          ret(sc) = graph.blocks(obi).nodes.flatMap:
            case Node.Input(id) => Some(id)
            case _ => None
          ret(sc) += cond
        case _ => // TODO: Merge*/

    for (bi -> b) <- graph.blocks do
      val live = mutable.Set.from:
        for Node.Output(_, lab) <- b.nodes.valuesIterator
        yield lab

      for (state -> nodes) <- b.stateToNodes(sche).sets.toSeq.reverseIterator do
        for node <- nodes do
          live ++= node.read
          live --= node.written
        ret(state) = live.toSet

      // ignore input nodes

    ret
  end liveInsForState

end Liveness
