package net.akouryy.synthescala
package cdfg
package schedule

import scala.collection.mutable
import scala.math.Ordering.Implicits.infixOrderingOps

class GorgeousScheduler(graph: CDFG) extends Scheduler:
  private val jumpStates = mutable.MultiDict.empty[JumpIndex, State]
  private val nodeStates = mutable.Map.empty[(BlockIndex, NodeID), State]
  private val visited = mutable.Set.empty[BlockIndex]
  private val arrayAccessedAfter = mutable.Set.empty[(State, Label)]
  private var maxState: State = _

  override def schedule: Schedule =
    jumpStates.clear()
    nodeStates.clear()
    visited.clear()
    arrayAccessedAfter.clear()
    maxState = State(0)
    scheduleJump(graph.main, graph.main.jumps.firstKey)
    Schedule(jumpStates.sets, nodeStates.toMap)

  private def stateOf(bi: BlockIndex, nid: NodeID): Option[State] =
    graph.node(bi, nid) match
      case _: (Node.Input | Node.Const) => Some(maxState)
      case _ => nodeStates.get(bi, nid)

  private def scheduleBlock(fn: CDFGFun, bi: BlockIndex): Unit =
    val block = fn.blocks(bi)
    val nodes = block.nodes
    visited += bi

    def nodeVisited(nid: NodeID): Boolean = stateOf(bi, nid).nonEmpty

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

    val q = mutable.Queue.from:
      import scala.language.implicitConversions
      nodes.keysIterator.filter(nodes(_).isInput)

    while q.nonEmpty do
      val nid = q.dequeue()

      if !nodeStates.contains(bi, nid)
        val node = nodes(nid)
        if !node.isInput
          var state =
            (
              node.read.map(r => stateOf(bi, block.writeMap(r)).get).maxOption.toList ++
              parents(nid).map(par => stateOf(bi, par).get).maxOption
            ).max.succ
          node match
            case Node.GetReq(_, _, arr, _) =>
              while arrayAccessedAfter(state, arr) do
                state = state.succ
              arrayAccessedAfter += state -> arr
            case Node.GetAwa(_, reqID, _, _) =>
              state = nodeStates(bi, reqID).succ
            case _ =>
          nodeStates((bi, nid)) = state

        for
          nid2 <- node.written.iterator.flatMap(block.readMap) ++ children(nid)
          if nodes(nid2).read.forall(r => nodeVisited(block.writeMap(r))) &&
             parents(nid2).forall(nodeVisited)
        do
          q.enqueue(nid2)
    end while

    maxState =
      import scala.language.implicitConversions
      // TODO: そもそもmapでなくflatMapなのはおかしい(生存解析失敗)
      block.nodes.keysIterator.flatMap(nid => stateOf(bi, nid))
        .maxOption.fold(maxState)(maxState.max)

    // align Outputs
    for (nid, node: Node.Output) <- block.nodes do
      nodeStates((bi, nid)) = maxState

    scheduleJump(fn, block.outJump)
  end scheduleBlock

  private def scheduleJump(fn: CDFGFun, ji: JumpIndex): Unit =
    val jump = fn.jumps(ji)
    jumpStates += ji -> maxState
    if jump.inBlocks.forall(visited)
      jump match
        case _: Jump.Branch =>
          maxState = maxState.succ
        case _ =>
      jump.outBlocks.foreach(scheduleBlock(fn, _))
  end scheduleJump
