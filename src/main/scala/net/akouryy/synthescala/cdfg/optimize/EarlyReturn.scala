// Forked from https://github.com/cpu2019-5/anscaml/blob/master/src/main/scala/net/akouryy/anscaml/arch/tig/optimize/EarlyReturn.scala (MIT License)

package net.akouryy.synthescala
package cdfg
package optimize

object EarlyReturn:
  def apply(graph: CDFG): Unit =
    traverse(graph, graph.jumps.lastKey)

  private def traverse(graph: CDFG, ji0: JumpIndex): Unit =
    graph.jumps(ji0) match
      case Jump.Return(_, retID, bi1) =>
        graph.blocks(bi1) match
          case Block(_, nodes, ji2, _) if nodes.forall {
            case Node.Input(id) => id == retID
            case Node.Output(id) => id == retID
            case _ => false
          } =>
            graph.jumps(ji2) match
              case Jump.Merge(_, ibs, inIDss, _, ons) if ons.contains(retID) =>
                val retIdx = ons.indexOf(retID)
                graph.jumps --= Seq(ji0, ji2)
                graph.blocks -= bi1

                for
                  (bi3, inIDs) <- ibs zip inIDss
                  inID = inIDs(retIdx)
                do
                  val ji4 = JumpIndex.generate()
                  graph.jumps(ji4) = Jump.Return(ji4, inID, bi3)
                  graph.blocks(bi3) = graph.blocks(bi3).copy(outJump = ji4)
                  traverse(graph, ji4)
              case _ =>
          case _ =>
      case _ => ???
  end traverse
end EarlyReturn
