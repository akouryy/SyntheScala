package net.akouryy.synthescala
package cdfg
package schedule

import scala.collection.mutable
import scala.math.Ordering.Implicits.infixOrderingOps

class GorgeousScheduler(graph: CDFG) extends Scheduler:
  private val jumpStates = mutable.MultiDict.empty[JumpIndex, State]
  private val nodeStates = mutable.Map.empty[(BlockIndex, Node), State]
  private val visited = mutable.Set.empty[BlockIndex]
  private val arrayAccessed = mutable.Set.empty[(State, Label)]
  private var maxState: State = _

  override def schedule: Schedule =
    jumpStates.clear()
    nodeStates.clear()
    visited.clear()
    arrayAccessed.clear()
    maxState = State(0)
    scheduleJump(graph.main, graph.main.jumps.firstKey)
    Schedule(jumpStates.sets, nodeStates.toMap)

  private def stateOf(bi: BlockIndex, node: Node): Option[State] =
    node match
      case _: (Node.Input | Node.Const) => Some(maxState)
      case _ => nodeStates.get(bi, node)

  private def scheduleBlock(fn: CDFGFun, bi: BlockIndex): Unit =
    val block = fn.blocks(bi)
    visited += bi

    def nodeVisited(node: Node): Boolean = stateOf(bi, node).nonEmpty

    def parents(node: Node) =
      node match
        case _: (Node.Get | Node.Put) => block.arrayDeps.goBackward(node)
        case _ => Set.empty

    def children(node: Node) =
      node match
        case _: (Node.Get | Node.Put) => block.arrayDeps.goForward(node)
        case _ => Set.empty

    val q =
      import scala.language.implicitConversions
      block.nodes.filter(_.isInstanceOf[Node.Input | Node.Const]).to(mutable.Queue)

    while q.nonEmpty do
      val nd = q.dequeue()
      if !nodeStates.contains(bi, nd)
        if !nd.isInput
          var state =
            (
              nd.read.map(r => stateOf(bi, block.writeMap(r)).get).maxOption.toList ++
              parents(nd).map(par => stateOf(bi, par).get).maxOption
            ).max.succ
          nd match
            case Node.Get(arr, _, _) =>
              while arrayAccessed(state, arr) do
                state = state.succ
              arrayAccessed += state -> arr
            case _ =>
          nodeStates((bi, nd)) = state

        for
          nd2 <- nd.written.iterator.flatMap(block.readMap) ++ children(nd)
          if nd2.read.forall(r => nodeVisited(block.writeMap(r))) &&
             parents(nd2).forall(nodeVisited)
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
