package net.akouryy.synthescala
package cdfg
package schedule

import scala.collection.mutable
import scala.math.Ordering.Implicits.infixOrderingOps

class GorgeousScheduler(graph: CDFG) extends Scheduler:
  private val jumpStates = mutable.Map.empty[JumpIndex, mutable.Map[BlockIndex, State]]
  private val nodeStates = mutable.Map.empty[NodeID, State]
  private val visited = mutable.Set.empty[BlockIndex]
  private val arrayAccessedAfter = mutable.Set.empty[(State, Label)]
  private var maxState: State = _

  override def schedule: Schedule =
    jumpStates.clear()
    nodeStates.clear()
    visited.clear()
    arrayAccessedAfter.clear()
    maxState = State(0)
    scheduleJump(graph.main, null, graph.main.jumps.firstKey) // TODO
    Schedule(jumpStates.toMap, nodeStates.toMap)

  private def scheduleBlock(fn: CDFGFun, bi: BlockIndex): Unit =
    val block = fn.blocks(bi)
    val nodes = block.nodes
    visited += bi

    def labelDefVisited(label: Label): Boolean =
      block.getWritingNode(label).forall(nodeStates.contains)

    def parents(nid: NodeID) =
      if nodes(nid).isMemoryRelated
        block.arrayDeps.goBackward(nid)
      else
        Set.empty

    def children(nid: NodeID) =
      if nodes(nid).isMemoryRelated
        block.arrayDeps.goForward(nid)
      else
        Set.empty

    def isNodeReady(nid: NodeID): Boolean =
      nodes(nid).read.forall(labelDefVisited) &&
      parents(nid).forall(nodeStates.contains)

    val q = mutable.Queue.from(nodes.keysIterator.filter(isNodeReady))

    while q.nonEmpty do
      val nid = q.dequeue()

      if !nodeStates.contains(nid)
        val node = nodes(nid)

        var state =
          (
            node.read.map(r =>
              if block.inputs.contains(r)
                maxState
              else
                nodeStates(block.writeMap(r))
            ).maxOption.toList ++
            parents(nid).map(par => nodeStates(par)).maxOption ++
            Some(maxState)
          ).max.succ
        node match
          case Node.GetReq(_, _, arr, _) =>
            while arrayAccessedAfter(state, arr) do
              state = state.succ
            arrayAccessedAfter += state -> arr
          case Node.GetAwa(_, reqID, _, _) =>
            state = nodeStates(reqID).succ
          case _ =>
        nodeStates(nid) = state

        for
          nid2 <- node.written.iterator.flatMap(block.getReadingNodes) ++ children(nid)
          if isNodeReady(nid2)
        do
          q.enqueue(nid2)
    end while

    maxState =
      import scala.language.implicitConversions
      // TODO: そもそもmapでなくflatMapなのはおかしい(生存解析失敗)
      block.nodes.keysIterator.flatMap(nid =>
        Option.when(!block.inputs.contains(nid))(nodeStates(nid))
      ).maxOption.fold(maxState)(maxState.max)

    scheduleJump(fn, bi, block.outJump)
  end scheduleBlock

  private def scheduleJump(fn: CDFGFun, ibi: BlockIndex, ji: JumpIndex): Unit =
    val jump = fn.jumps(ji)
    jumpStates.getOrElseUpdate(ji, mutable.Map.empty)
    if jump.occupiesState
      maxState = maxState.succ
    jumpStates(ji)(ibi) = maxState
    if jump.inBlocks.forall(visited)
      jump.outBlocks.foreach(scheduleBlock(fn, _))
  end scheduleJump
