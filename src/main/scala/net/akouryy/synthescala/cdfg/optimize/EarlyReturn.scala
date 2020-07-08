// Forked from https://github.com/cpu2019-5/anscaml/blob/master/src/main/scala/net/akouryy/anscaml/arch/tig/optimize/EarlyReturn.scala (MIT License)

package net.akouryy.synthescala
package cdfg
package optimize

object EarlyReturn:
  def apply(graph: CDFG): Unit =
    traverse(graph.main, graph.main.jumps.lastKey)

  private def traverse(fn: CDFGFun, ji0: JumpIndex): Unit =
    fn.jumps(ji0) match
      case Jump.Return(_, retLab, bi1) =>
        val b1 @ Block(_, _, _, rawNodes, _, ji2, _) = fn.blocks(bi1)
        val nodes = rawNodes.filter(!_._2.isNop)

        if nodes.isEmpty
          fn.jumps(ji2) match
            case Jump.Merge(_, ibs, inIDss, _, ons) if ons.contains(retLab) =>
              val retIdx = ons.indexOf(retLab)
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

        b1.outputs match
          case Seq(lab) => // one element
            b1.writeMap.get(lab).map(b1.nodes) match
              case Some(node @ Node.Call(nid, callee, args, _)) =>
                fn.jumps -= ji0
                val ji0p = JumpIndex.generate(ji0)
                fn.jumps(ji0p) = Jump.TailCall(ji0p, callee, args, bi1)
                var newNodes = b1.nodes - nid
                fn.blocks(bi1) = b1.copy(
                  outputs = args,
                  nodes = Node.compensateNop(newNodes)(_ => ()),
                  outJump = ji0p
                )
                return
              case _ =>
          case _ =>
      case _ => ???
  end traverse
end EarlyReturn
