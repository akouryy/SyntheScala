// Forked from https://github.com/cpu2019-5/anscaml/blob/master/src/main/scala/net/akouryy/anscaml/arch/tig/optimize/EarlyReturn.scala (MIT License)

package net.akouryy.synthescala
package cdfg
package optimize

import scala.collection.mutable

class ConstFold(graph: CDFG, typEnv: toki.TypeEnv):
  private val consts = mutable.Map.empty[Label, Long]

  def run(): Unit =
    consts.clear()

    for
      b <- graph.main.blocks.valuesIterator
      Node.Const(_, c, v) <- b.nodes.valuesIterator
    do
      consts(v) = c

    graph.main.blocks.mapValuesInPlace:
      (bi, b) =>
        b.copy(nodes = b.nodes.view.mapValues {
          case (node @ Node.BinOp(_, _, left, right, _)) =>
            node.copy(
              left = left match
                case VC.V(v) => consts.get(v).fold(left)(VC.C(_, typEnv(v)))
                case c => c,
              right = right match
                case VC.V(v) => consts.get(v).fold(right)(VC.C(_, typEnv(v)))
                case c => c,
            )
          case node => node
        }.toMap)
  end run

end ConstFold
