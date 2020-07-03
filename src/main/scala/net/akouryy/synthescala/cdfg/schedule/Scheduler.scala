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
  jumpStates: collection.Map[JumpIndex, collection.Set[State]],
  nodeStates: collection.Map[(BlockIndex, Node), State],
):
  def stateOf(graph: CDFG, bi: BlockIndex, nd: Node): State | collection.Set[State] =
    getStateOf(graph, bi, nd).get

  def getStateOf(graph: CDFG, bi: BlockIndex, nd: Node): Option[State | collection.Set[State]] =
    if nd.isInput
      jumpStates.get(graph.blocks(bi).inJumpIndex)
    else
      nodeStates.get(bi, nd)
