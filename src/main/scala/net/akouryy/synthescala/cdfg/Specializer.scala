// Forked from https://github.com/cpu2019-5/anscaml/blob/master/src/main/scala/net/akouryy/anscaml/arch/tig/Specializer.scala (MIT License)

package net.akouryy.synthescala
package cdfg

import scala.collection.mutable

class Specializer:
  private var currentGraph: CDFGFun = _
  private var currentBlockIndex: BlockIndex = _
  private var currentInputJumpIndex: JumpIndex = _
  private val currentNodes = mutable.IndexedBuffer.empty[Node]
  private var currentArrayDeps: UnweightedGraph[Node] = _
  private var currentArrayLastGet = mutable.Map.empty[Label, List[Node]]
  private var currentArrayLastPut = mutable.Map.empty[Label, Option[Node]]
  private val writtenLabel = mutable.Set.empty[Label]
  private var aliasMap = mutable.Map.empty[Label, Label]

  private def normalize(x: Label, checkWritten: Boolean = true): Label =
    if !checkWritten || writtenLabel(x)
      aliasMap.getOrElse(x, x)
    else
      writtenLabel += x
      x

  private def addNode(node: Node): Node =
    currentArrayDeps.addVertex(node)
    currentNodes += node
    node
  private def arrayLastGet(lab: Label): List[Node] =
    currentArrayLastGet.getOrElseUpdate(lab, Nil)
  private def arrayLastPut(lab: Label): Option[Node] =
    currentArrayLastPut.getOrElseUpdate(lab, None)

  private def specializeExpr(dest: Label, expr: toki.Expr): Unit =
    import toki.Expr._
    expr match
      case Num(i, _) =>
        writtenLabel += dest
        addNode(Node.Const(i, dest))
      case Ref(n) =>
        writtenLabel += dest
        aliasMap(dest) = normalize(n)
      case Bin(op, Ref(l), Ref(r)) =>
        writtenLabel += dest
        addNode(Node.BinOp(op, normalize(l), normalize(r), dest))
      case Call(fn, args) =>
        writtenLabel += dest
        addNode(Node.Call(fn, args.map(a => normalize(a.asInstanceOf[Ref].name)), dest))

      case Get(arr, Ref(index)) =>
        writtenLabel += dest
        val node = addNode(Node.Get(arr, normalize(index), dest))
        for parent <- arrayLastPut(arr) do
          currentArrayDeps.addEdge(parent -> node)
        currentArrayLastGet(arr) = node :: arrayLastGet(arr)
      case Put(arr, Ref(index), Ref(value), kont) =>
        val node = addNode(Node.Put(arr, normalize(index), normalize(value)))
        for parent <- arrayLastGet(arr).iterator ++ arrayLastPut(arr) do
          currentArrayDeps.addEdge(parent -> node)
        currentArrayLastGet(arr) = Nil
        currentArrayLastPut(arr) = Some(node)
        specializeExpr(dest, kont)

      case Let(toki.Entry(n, t), x, b) =>
        specializeExpr(n, x)
        specializeExpr(dest, b)
      case If(Ref(c), truExpr, flsExpr) =>
        val beginBI = currentBlockIndex
        val branchJI, mergeJI = JumpIndex.generate()
        val truBI, flsBI, kontBI = BlockIndex.generate()
        val truDest, flsDest = Label.temp()

        // ifの前のブロックを登録
        currentGraph.blocks(beginBI) =
          Block(beginBI, currentNodes.toSet, currentArrayDeps, currentInputJumpIndex, branchJI)
        currentNodes.clear()
        currentArrayDeps = UnweightedGraph()
        currentArrayLastGet.clear()
        currentArrayLastPut.clear()
        writtenLabel.clear()
        // if分岐を登録
        currentGraph.jumps(branchJI) = Jump.Branch(branchJI, c, beginBI, truBI, flsBI)

        // 真分岐
        currentBlockIndex = truBI
        currentInputJumpIndex = branchJI
        specializeExpr(truDest, truExpr)
        // 真分岐の最後のブロックを登録
        val truLastBI = currentBlockIndex
        currentGraph.blocks(truLastBI) =
          Block(truLastBI, currentNodes.toSet, currentArrayDeps, currentInputJumpIndex, mergeJI)
        currentNodes.clear()
        currentArrayDeps = UnweightedGraph()
        currentArrayLastGet.clear()
        currentArrayLastPut.clear()
        writtenLabel.clear()

        // 偽分岐
        currentBlockIndex = flsBI
        currentInputJumpIndex = branchJI
        specializeExpr(flsDest, flsExpr)
        // 偽分岐の最後のブロックを登録
        val flsLastBI = currentBlockIndex
        currentGraph.blocks(flsLastBI) =
          Block(flsLastBI, currentNodes.toSet, currentArrayDeps, currentInputJumpIndex, mergeJI)
        currentNodes.clear()
        currentArrayDeps = UnweightedGraph()
        currentArrayLastGet.clear()
        currentArrayLastPut.clear()
        writtenLabel.clear()

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
        currentArrayDeps = UnweightedGraph()
        currentArrayLastGet.clear()
        currentArrayLastPut.clear()
        writtenLabel.clear()
        writtenLabel += dest
      case _ => assert(false, expr)
  end specializeExpr

  def apply(prog: toki.Program): CDFG = // synchronized:
    currentGraph = CDFGFun(prog.main.name, prog.main.params.map(_.name))
    val dest = Label.temp()
    currentInputJumpIndex = JumpIndex.generate()
    currentBlockIndex = BlockIndex.generate()
    currentArrayDeps = UnweightedGraph()
    currentGraph.jumps(currentInputJumpIndex) =
      Jump.StartFun(currentInputJumpIndex, currentBlockIndex)

    specializeExpr(dest, prog.main.body)

    val normalizedDest = normalize(dest, checkWritten=false)
    val retJI = JumpIndex.generate()
    currentGraph.blocks(currentBlockIndex) =
      Block(currentBlockIndex, currentNodes.toSet, currentArrayDeps, currentInputJumpIndex, retJI)
    currentNodes.clear()
    currentArrayDeps = UnweightedGraph()
    currentArrayLastGet.clear()
    currentArrayLastPut.clear()
    writtenLabel.clear()
    currentGraph.jumps(retJI) =
      Jump.Return(retJI, normalizedDest, currentBlockIndex)

    CDFG(prog.arrayDefs, currentGraph)
  end apply

end Specializer
