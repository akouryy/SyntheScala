// Forked from https://github.com/cpu2019-5/anscaml/blob/master/src/main/scala/net/akouryy/anscaml/arch/tig/optimize/EarlyReturn.scala (MIT License)

package net.akouryy.synthescala
package cdfg
package optimize

import scala.collection.mutable

class RemoveUnused(graph: CDFG):
  private val used = mutable.Set.empty[Label]

  def run(): Unit =
    used.clear()

    for b <- graph.main.blocks.valuesIterator do
      used ++= b.inputs
      used ++= b.outputs
      used ++= b.uses

    graph.main.blocks.mapValuesInPlace:
      (bi, b) =>
        b.copy(nodes = b.nodes.filter { (_, node) =>
          node.isMemoryRelated || node.isNop || node.written.forall(used)
        })
  end run

end RemoveUnused
