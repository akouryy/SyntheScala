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
)
