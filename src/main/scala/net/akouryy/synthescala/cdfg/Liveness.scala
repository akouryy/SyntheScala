package net.akouryy.synthescala
package cdfg

import scala.collection.mutable

object Liveness:
  def insertInOuts(graph: CDFG): Unit =
    val jis = graph.jumps.filter(_._2.isInstanceOf[Jump.Return]).keysIterator.to:
      import scala.language.implicitConversions
      mutable.Stack

    val visited = mutable.Set.empty[BlockIndex]
    val liveIns = mutable.Map.empty[BlockIndex, Set[Label]]

    while jis.nonEmpty do
      val ji = jis.pop()
      val jump = graph.jumps(ji)
      if jump.outBlocks.forall(visited)
        val liveOutBase = jump.outBlocks.toSet.flatMap(liveIns)
        for bi <- jump.inBlocks do
          val block = graph.blocks(bi)
          visited += bi
          jis += block.inJumpIndex
          val liveOut = jump match
            case Jump.Branch(_, cond, _, _, _) => liveOutBase + cond
            case Jump.Merge(_, ibs, ins, _, ons) => liveOutBase -- ons ++ ins(ibs.indexOf(bi))
            case Jump.Return(_, v, _) => liveOutBase + v
            case _ => assert(false)

          liveIns(bi) = liveOut ++ block.uses -- block.defs

          graph.blocks(bi) = block.copy(
            nodes = block.nodes ++ liveIns(bi).map(Node.Input(_)) ++ liveOut.map(Node.Output(_))
          )
  end insertInOuts

  def liveInsForState(graph: CDFG, sche: schedule.Schedule)
  : collection.Map[State, Set[Label]] =

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
      val live = mutable.Set.empty[Label]
      for node <- b.nodes do
        node match
          case Node.Output(id) => live += id
          case _ =>

      for (state -> nodes) <- b.stateToNodes(sche).sets.toSeq.reverseIterator do
        for node <- nodes do
          live ++= node.read
          live --= node.written
        ret(state) = live.toSet

      // ignore input nodes

    ret
  end liveInsForState

end Liveness
