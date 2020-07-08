package net.akouryy.synthescala
package cdfg
package optimize

object ScheduledOptimizer:
  def apply(graph: CDFG, typEnv: toki.TypeEnv, sche: schedule.Schedule)
  : (CDFG, toki.TypeEnv, schedule.Schedule) =
    SimpleRecParallelism(graph, typEnv)(using sche).run()
