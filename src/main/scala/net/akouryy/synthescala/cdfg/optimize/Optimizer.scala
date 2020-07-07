package net.akouryy.synthescala
package cdfg
package optimize

object Optimizer:
  def apply(graph: CDFG, typEnv: toki.TypeEnv): Unit =
    EarlyReturn(graph)
    ConstFold(graph, typEnv).run()
    RemoveUnused(graph).run()
