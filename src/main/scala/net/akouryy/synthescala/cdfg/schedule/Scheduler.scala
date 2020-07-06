package net.akouryy.synthescala
package cdfg
package schedule

trait Scheduler:
  def schedule: Schedule

/**
  @param jumpStates ジャンプ実行時の状態としてありえるもの
  @param nodeStates Input,Const以外のノードの実行時の状態
*/
case class Schedule(
  jumpStates: collection.Map[JumpIndex, collection.Map[BlockIndex, State]],
  nodeStates: collection.Map[(BlockIndex, NodeID), State],
):
  def stateOf(graph: CDFG, bi: BlockIndex, nd: NodeID): State | collection.Set[State] =
    getStateOf(graph, bi, nd).get

  def getStateOf(graph: CDFG, bi: BlockIndex, nd: NodeID): Option[State | collection.Set[State]] =
    if graph.node(bi, nd).isInput
      jumpStates.get(graph.main.blocks(bi).inJumpIndex).map(_.values.toSet)
    else
      nodeStates.get(bi, nd)
