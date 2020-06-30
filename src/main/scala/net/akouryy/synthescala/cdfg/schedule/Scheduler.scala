package net.akouryy.synthescala
package cdfg
package schedule

trait Scheduler:
  def schedule: Scheduler.Schedule

object Scheduler:
  type Schedule = Map[(BlockIndex, Node) | JumpIndex, State]
