package net.akouryy.synthescala
package cdfg
package schedule

import scala.collection.mutable
import scala.math.Ordering.Implicits.infixOrderingOps

class GorgeousScheduler(graph: CDFG) extends Scheduler:
  private val states = mutable.Map.empty[(BlockIndex, Node) | JumpIndex, State]
  private val visited = mutable.Set.empty[BlockIndex]
  private var maxState: State = _

  override def schedule: Scheduler.Schedule =
    states.clear()
    visited.clear()
    maxState = State(0)
    scheduleBlock(graph.blocks.firstKey)
    states.toMap

  private def scheduleBlock(bi: BlockIndex): Unit =
    val block = graph.blocks(bi)
    visited += bi
    val q =
      import scala.language.implicitConversions
      block.nodes.filter(_.isInstanceOf[Node.Input | Node.Const]).to(mutable.Queue)

    while q.nonEmpty do
      val nd = q.dequeue()
      println(nd)
      if !states.contains(bi, nd)
        nd match
          case _: (Node.Input | Node.Const) =>
            states((bi, nd)) = maxState
          case _ =>
            states((bi, nd)) =
              nd.read.map(r => states(bi, block.writeMap(r))).max.succ

        for
          w <- nd.written
          nd2 <- block.readMap(w)
          () = println((nd2, nd2.read, nd2.read.map(block.writeMap),
            nd2.read.map(r => states.get(bi, block.writeMap(r)))))
          if nd2.read.forall(r => states.contains(bi, block.writeMap(r)))
        do
          q.enqueue(nd2)
    end while

    maxState =
      import scala.language.implicitConversions
      // TODO: そもそもgetが必要なのはおかしい(生存解析失敗)
      block.nodes.flatMap(states.get(bi, _)).maxOption.fold(maxState)(maxState.max)

    scheduleJump(block.outJump)
  end scheduleBlock

  private def scheduleJump(ji: JumpIndex): Unit =
    val jump = graph.jumps(ji)
    if jump.inBlocks.forall(visited)
      jump match
        case _: Jump.Branch =>
          states(ji) = maxState.succ
          maxState = maxState.succ
        case _ =>

      jump.outBlocks.foreach(scheduleBlock)
  end scheduleJump
