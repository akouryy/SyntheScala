// Forked from https://github.com/cpu2019-5/anscaml/blob/master/src/main/scala/net/akouryy/anscaml/arch/tig/Specializer.scala (MIT License)

package net.akouryy.synthescala
package cdfg

import scala.collection.mutable

class Specializer:
  private var currentGraph: CDFGFun = _
  private var currentBlockIndex: BlockIndex = _
  private var currentInputJumpIndex: JumpIndex = _
  private val currentNodes = mutable.IndexedBuffer.empty[Node]
  private var currentArrayDeps: UnweightedGraph[NodeID] = _
  private var currentArrayLastGet = mutable.Map.empty[Label, List[NodeID]]
  private var currentArrayLastPut = mutable.Map.empty[Label, Option[NodeID]]
  private val writtenLabel = mutable.Set.empty[Label]
  private var aliasMap = mutable.Map.empty[Label, Label]

  private def normalize(x: Label, checkWritten: Boolean = true): Label =
    if !checkWritten || writtenLabel(x)
      aliasMap.getOrElse(x, x)
    else
      writtenLabel += x
      x

  private def addNode(node: Node): Node =
    currentArrayDeps.addVertex(node.id)
    currentNodes += node
    node
  private def arrayLastGet(lab: Label): List[NodeID] =
    currentArrayLastGet.getOrElseUpdate(lab, Nil)
  private def arrayLastPut(lab: Label): Option[NodeID] =
    currentArrayLastPut.getOrElseUpdate(lab, None)

  private def extractNodes(): Map[NodeID, Node] =
    val ret = currentNodes.map(n => n.id -> n).toMap
    currentNodes.clear()
    ret

  private def specializeExpr(dest: Label, expr: toki.Expr): Unit =
    import toki.Expr._
    expr match
      case Num(i, _) =>
        writtenLabel += dest
        addNode(Node.Const(NodeID.generate(), i, dest))
      case Ref(n) =>
        writtenLabel += dest
        aliasMap(dest) = normalize(n)
      case Bin(op, Ref(l), Ref(r)) =>
        writtenLabel += dest
        addNode(Node.BinOp(NodeID.generate(), op, normalize(l), normalize(r), dest))
      case Call(fn, args) =>
        writtenLabel += dest
        addNode(Node.Call(
          NodeID.generate(), fn, args.map(a => normalize(a.asInstanceOf[Ref].name)), dest,
        ))

      case Get(arr, Ref(index)) =>
        writtenLabel += dest
        val reqID = NodeID.generate()
        val awa = addNode(Node.GetAwa(NodeID.generate(), reqID, arr, dest))
        val req = addNode(Node.GetReq(reqID, awa.id, arr, normalize(index)))
        for parent <- arrayLastPut(arr) do
          currentArrayDeps.addEdge(parent -> reqID)
        currentArrayDeps.addEdge(reqID -> awa.id)
        currentArrayLastGet(arr) = awa.id :: arrayLastGet(arr)
      case Put(arr, Ref(index), Ref(value), kont) =>
        val node = addNode(Node.Put(NodeID.generate(), arr, normalize(index), normalize(value)))
        for parent <- arrayLastGet(arr).iterator ++ arrayLastPut(arr) do
          currentArrayDeps.addEdge(parent -> node.id)
        currentArrayLastGet(arr) = Nil
        currentArrayLastPut(arr) = Some(node.id)
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
        currentGraph.blocks(beginBI) = Block(
          beginBI, Nil, Nil, extractNodes(), currentArrayDeps, currentInputJumpIndex, branchJI,
        )
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
        currentGraph.blocks(truLastBI) = Block(
          truLastBI, Nil, Nil, extractNodes(), currentArrayDeps, currentInputJumpIndex, mergeJI,
        )
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
        currentGraph.blocks(flsLastBI) = Block(
          flsLastBI, Nil, Nil, extractNodes(), currentArrayDeps, currentInputJumpIndex, mergeJI,
        )
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
    currentGraph = CDFGFun(prog.main.name, prog.main.ret, prog.main.params)
    val dest = Label.temp()
    currentInputJumpIndex = JumpIndex.generate()
    currentBlockIndex = BlockIndex.generate()
    currentArrayDeps = UnweightedGraph()
    currentGraph.jumps(currentInputJumpIndex) =
      Jump.StartFun(currentInputJumpIndex, currentBlockIndex)

    specializeExpr(dest, prog.main.body)

    val normalizedDest = normalize(dest, checkWritten=false)
    val retJI = JumpIndex.generate()
    currentGraph.blocks(currentBlockIndex) = Block(
      currentBlockIndex, Nil, Nil, extractNodes(), currentArrayDeps, currentInputJumpIndex, retJI,
    )
    currentArrayDeps = UnweightedGraph()
    currentArrayLastGet.clear()
    currentArrayLastPut.clear()
    writtenLabel.clear()
    currentGraph.jumps(retJI) =
      Jump.Return(retJI, normalizedDest, currentBlockIndex)

    CDFG(prog.arrayDefs, currentGraph)
  end apply

end Specializer
