package net.akouryy.synthescala
package cdfg

import scala.collection.mutable

object Liveness:
  def insertInOuts(graph: CDFG): Unit =
    val jis = graph.jumps.filter(_._2.isInstanceOf[Jump.Return]).keysIterator.to:
      import scala.language.implicitConversions
      mutable.Stack

    val visited = mutable.Set.empty[BlockIndex]
    val liveIns = mutable.Map.empty[BlockIndex, Set[String]]

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

  def liveInsForState(graph: CDFG, sche: schedule.Scheduler.Schedule)
  : collection.Map[State, Set[String]] =

    val ret = mutable.Map.empty[State, Set[String]]

    for (ji -> j) <- graph.jumps do
      j match
        case Jump.Branch(_, cond, _, _, obi) =>
          val sc = sche(ji)
          ret(sc) = graph.blocks(obi).nodes.flatMap:
            case Node.Input(id) => Some(id)
            case _ => None
          ret(sc) += cond
        case _ => // TODO: Merge

    for (bi -> b) <- graph.blocks do
      val stateToNodes = b.stateToNodes(sche)
      val live = mutable.Set.empty[String]
      for node <- b.nodes do
        node match
          case Node.Output(id) => live += id
          case _ =>

      for (state -> nodes) <- stateToNodes.sets.toSeq.reverseIterator do
        for node <- nodes do
          live ++= node.read
          live --= node.written
        ret(state) = live.toSet

    ret
  end liveInsForState

end Liveness
