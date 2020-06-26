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
          jis += block.inJump
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
end Liveness
