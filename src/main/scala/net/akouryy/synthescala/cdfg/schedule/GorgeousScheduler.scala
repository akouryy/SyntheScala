package net.akouryy.synthescala
package cdfg
package schedule

import scala.collection.mutable
import scala.math.Ordering.Implicits.infixOrderingOps

class GorgeousScheduler(graph: CDFG) extends Scheduler:
  private val jumpStates = mutable.MultiDict.empty[JumpIndex, State]
  private val nodeStates = mutable.Map.empty[(BlockIndex, Node), State]
  private val visited = mutable.Set.empty[BlockIndex]
  private var maxState: State = _

  override def schedule: Schedule =
    jumpStates.clear()
    nodeStates.clear()
    visited.clear()
    maxState = State(0)
    scheduleJump(graph.jumps.firstKey)
    Schedule(jumpStates.sets, nodeStates.toMap)

  private def stateOf(bi: BlockIndex, node: Node): Option[State] =
    node match
      case _: (Node.Input | Node.Const) => Some(maxState)
      case _ => nodeStates.get(bi, node)

  private def scheduleBlock(bi: BlockIndex): Unit =
    val block = graph.blocks(bi)
    visited += bi

    val q =
      import scala.language.implicitConversions
      block.nodes.filter(_.isInstanceOf[Node.Input | Node.Const]).to(mutable.Queue)

    while q.nonEmpty do
      val nd = q.dequeue()
      if !nodeStates.contains(bi, nd)
        if !nd.isInput
          nodeStates((bi, nd)) =
            nd.read.map(r => stateOf(bi, block.writeMap(r)).get).max.succ

        for
          w <- nd.written
          nd2 <- block.readMap(w)
          if nd2.read.forall(r => stateOf(bi, block.writeMap(r)).nonEmpty)
        do
          q.enqueue(nd2)
    end while

    maxState =
      import scala.language.implicitConversions
      // TODO: そもそもmapでなくflatMapなのはおかしい(生存解析失敗)
      block.nodes.flatMap(nd => stateOf(bi, nd)).maxOption.fold(maxState)(maxState.max)

    // align Outputs
    for util.Identity(node: Node.Output) <- block.nodes do
      nodeStates((bi, node)) = maxState

    scheduleJump(block.outJump)
  end scheduleBlock

  private def scheduleJump(ji: JumpIndex): Unit =
    val jump = graph.jumps(ji)
    jumpStates += ji -> maxState
    if jump.inBlocks.forall(visited)
      jump match
        case _: Jump.Branch =>
          maxState = maxState.succ
        case _ =>
      jump.outBlocks.foreach(scheduleBlock)
  end scheduleJump
