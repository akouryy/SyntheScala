// Forked from https://github.com/cpu2019-5/anscaml/blob/master/src/main/scala/net/akouryy/anscaml/arch/tig/Specializer.scala (MIT License)

package net.akouryy.synthescala
package cdfg

import scala.collection.mutable

class Specializer:
  private var currentGraph: CDFG = _
  private var currentBlockIndex: BlockIndex = _
  private var currentInputJumpIndex: JumpIndex = _
  private val currentNodes = mutable.IndexedBuffer.empty[Node]
  private val writtenID = mutable.Set.empty[String]
  private var normalizationMap = mutable.Map.empty[String, String]

  private def normalize(x: String, checkWritten: Boolean = true) =
    if !checkWritten || writtenID(x)
      normalizationMap.getOrElse(x, x)
    else
      writtenID += x
      x

  private def specializeExpr(dest: String, expr: toki.Expr): Unit =
    import toki.Expr._
    expr match
      case Num(i) =>
        writtenID += dest
        currentNodes += Node.Const(i, dest)
      case Ref(n) =>
        writtenID += dest
        normalizationMap(dest) = normalize(n)
      case Bin(op, Ref(l), Ref(r)) =>
        writtenID += dest
        currentNodes += Node.BinOp(op, normalize(l), normalize(r), dest)
      case Call(fn, args) =>
        writtenID += dest
        currentNodes += Node.Call(fn, args.map(a => normalize(a.asInstanceOf[Ref].name)), dest)
      case Let(toki.Entry(n, t), x, b) =>
        specializeExpr(n, x)
        specializeExpr(dest, b)
      case If(Ref(c), truExpr, flsExpr) =>
        val beginBI = currentBlockIndex
        val branchJI, mergeJI = JumpIndex.generate()
        val truBI, flsBI, kontBI = BlockIndex.generate()
        val truDest, flsDest = ID.temp()

        // ifの前のブロックを登録
        currentGraph.blocks(beginBI) =
          Block(beginBI, currentNodes.toSet, currentInputJumpIndex, branchJI)
        currentNodes.clear()
        writtenID.clear()
        // if分岐を登録
        currentGraph.jumps(branchJI) = Jump.Branch(branchJI, c, beginBI, truBI, flsBI)

        // 真分岐
        currentBlockIndex = truBI
        currentInputJumpIndex = branchJI
        specializeExpr(truDest, truExpr)
        // 真分岐の最後のブロックを登録
        val truLastBI = currentBlockIndex
        currentGraph.blocks(truLastBI) =
          Block(truLastBI, currentNodes.toSet, currentInputJumpIndex, mergeJI)
        currentNodes.clear()
        writtenID.clear()

        // 偽分岐
        currentBlockIndex = flsBI
        currentInputJumpIndex = branchJI
        specializeExpr(flsDest, flsExpr)
        // 偽分岐の最後のブロックを登録
        val flsLastBI = currentBlockIndex
        currentGraph.blocks(flsLastBI) =
          Block(flsLastBI, currentNodes.toSet, currentInputJumpIndex, mergeJI)
        currentNodes.clear()
        writtenID.clear()

        // 併合を登録
        currentGraph.jumps(mergeJI) = Jump.Merge(
          mergeJI, IndexedSeq(truLastBI, flsLastBI),
          IndexedSeq(truDest, flsDest).map(d => IndexedSeq(normalize(d, checkWritten=false))),
          kontBI, IndexedSeq(dest),
        )
        // 継続
        currentBlockIndex = kontBI
        currentInputJumpIndex = mergeJI
        currentNodes.clear()
        writtenID.clear()
        writtenID += dest
      case _ => assert(false, expr)
  end specializeExpr

  def apply(f: toki.Fun): CDFG = // synchronized:
    currentGraph = CDFG(f.name, f.params.map(_.name))
    val dest = ID.temp()
    currentInputJumpIndex = JumpIndex.generate()
    currentBlockIndex = BlockIndex.generate()
    currentGraph.jumps(currentInputJumpIndex) =
      Jump.StartFun(currentInputJumpIndex, currentBlockIndex)

    specializeExpr(dest, f.body)

    val normalizedDest = normalize(dest, checkWritten=false)
    val retJI = JumpIndex.generate()
    currentGraph.blocks(currentBlockIndex) =
      Block(currentBlockIndex, currentNodes.toSet, currentInputJumpIndex, retJI)
    currentNodes.clear()
    writtenID.clear()
    currentGraph.jumps(retJI) =
      Jump.Return(retJI, normalizedDest, currentBlockIndex)
    currentGraph
  end apply

end Specializer
