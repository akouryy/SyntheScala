// Forked from https://github.com/cpu2019-5/anscaml/blob/master/src/main/scala/net/akouryy/anscaml/arch/tig/optimize/EarlyReturn.scala (MIT License)

package net.akouryy.synthescala
package cdfg
package optimize

object EarlyReturn:
  def apply(graph: CDFG): Unit =
    traverse(graph.main, graph.main.jumps.lastKey)

  private def traverse(fn: CDFGFun, ji0: JumpIndex): Unit =
    fn.jumps(ji0) match
      case Jump.Return(_, retID, bi1) =>
        val b1 @ Block(_, nodes, ji2, _) = fn.blocks(bi1)

        if nodes.forall {
          case Node.Input(id) => id == retID
          case Node.Output(id) => id == retID
          case _ => false
        } then
          fn.jumps(ji2) match
            case Jump.Merge(_, ibs, inIDss, _, ons) if ons.contains(retID) =>
              val retIdx = ons.indexOf(retID)
              fn.jumps --= Seq(ji0, ji2)
              fn.blocks -= bi1

              for
                (bi3, inIDs) <- ibs zip inIDss
                inID = inIDs(retIdx)
              do
                val ji2p = JumpIndex.generate(ji2)
                fn.jumps(ji2p) = Jump.Return(ji2p, inID, bi3)
                fn.blocks(bi3) = fn.blocks(bi3).copy(outJump = ji2p)
                traverse(fn, ji2p)
              return
            case _ =>
        end if

        b1.nodes.filter(_.isInstanceOf[Node.Output]).toSeq match
          case Seq(outputNode: Node.Output) => // one element
            b1.writeMap.get(outputNode.name) match
              case Some(node @ Node.Call(callee, args, _)) =>
                fn.jumps -= ji0
                val ji0p = JumpIndex.generate(ji0)
                fn.jumps(ji0p) = Jump.TailCall(ji0p, callee, args, bi1)
                fn.blocks(bi1) = b1.copy(
                  nodes = b1.nodes - node - outputNode ++ args.map(Node.Output(_)),
                  outJump = ji0p
                )
                return
              case _ =>
          case _ =>
      case _ => ???
  end traverse
end EarlyReturn
