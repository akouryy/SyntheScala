package net.akouryy.synthescala
package cdfg
package optimize

import toki.Type
import scala.collection.{immutable, mutable}

final class SimpleRecParallelism(graph: CDFG, typEnv: toki.TypeEnv)(using sche: schedule.Schedule):
  import SimpleRecParallelism._

  private var stateCnt = 0
  private val newTypEnv = mutable.Map.from(typEnv)
  private var oldStateToNodes: Map[State, Seq[(BlockIndex, Node)]] = _
  private val newJumpStates = mutable.Map.empty[JumpIndex, mutable.Map[BlockIndex, State]]
  private val newNodeStates = mutable.Map.empty[NodeID, State]
  private val oldReqIDToNewReqID = mutable.Map.empty[NodeID, NodeID]
  private val oldReqIDToNewAwaID = mutable.Map.empty[NodeID, NodeID]
  private var branchIndices: IndexedSeq[JumpIndex] = _
  private var secoIndices: IndexedSeq[BlockIndex] = _

  def run(): (CDFG, toki.TypeEnv, schedule.Schedule) =
    try
      val newMain = traverseFn(graph.main)
      (
        CDFG(graph.arrayDefs, newMain),
        newTypEnv.toMap,
        schedule.Schedule(newJumpStates, newNodeStates),
      )
    catch case CancelOptimizationException(msg) =>
      print(s"${fansi.Color.LightMagenta(s"SRP.cancel:$msg")}; ")
      (graph, typEnv, sche)

  private def traverseFn(oldFn: CDFGFun): CDFGFun =
    val oldNodeIDToBlockIndex = oldFn.nodeIDToBlockIndex
    oldStateToNodes = locally:
      val ret = mutable.Map.empty[State, List[(BlockIndex, Node)]]
      for (nid -> st) <- sche.nodeStates do
        val b = oldFn(oldNodeIDToBlockIndex(nid))
        ret(st) = b.i -> b.nodes(nid) :: ret.getOrElse(st, Nil)
      ret.toMap

    given pack as BlockPack = detectProperPath(oldFn).getOrElse:
      throw CancelOptimizationException("detect")

    if pack.prim.nodes.isEmpty then throw CancelOptimizationException("empty_prim")
    if pack.seco.nodes.isEmpty then throw CancelOptimizationException("empty_seco")

    val interval = intervalFromBlocks(oldFn)
    val (primPSS, rounds) = parallelizedStates(
      interval, pack.prim.states, pack.seco.states,
      oldFn,
    )
    println(interval)
    PP.pprintln(primPSS)
    PP.pprintln(rounds)

    given newLabs as NewLabs = locally:
      val newLabs = mutable.Map.empty[(Label, Int), Label]
      for
        i <- 0 to rounds.length
        params = oldFn.params.map(_.name)
        lab <- params ++ pack.prim.defs ++ pack.seco.defs
      do
        newLabs(lab -> i) =
          params.getIndexOf(lab) match
            case Some(idx) if i > 0 =>
              newLabs(pack.rec.args(idx) -> (i - 1))
            case _ =>
              Label.generate(lab)
      newLabs.toMap

    val newFn = CDFGFun(
      graph.main.fnName, graph.main.retTyp,
      graph.main.params.map(e => e.copy(name = newLabs(e.name -> 0))),
    )

    rewriteTyps()
    buildNewGraph(oldFn, newFn, primPSS, rounds)

    newFn
  end traverseFn

  /**
    末尾再帰に至るパスが1つしかなく、さらにそのパス上に分岐が1つしかない場合、そのパスの情報を返す。
  **/
  private def detectProperPath(oldFn: CDFGFun): Option[BlockPack] =
    val recs = oldFn.jumps.values.flatMap:
      case j: Jump.TailCall => Some(j)
      case _ => None

    if recs.sizeIs != 1
      None
    else
      val rec = recs.soleElement
      val pb = oldFn(oldFn.blocks.firstKey)
      val sbi = rec.inBlock
      for
        (branch, xbi, isSecoTruBody) <- oldFn(pb.outJump) match
          case j @ Jump.Branch(_, _, _, `sbi`, xbi) => Some((j, xbi, true))
          case j @ Jump.Branch(_, _, _, xbi, `sbi`) => Some((j, xbi, false))
          case _ => None
      yield
        BlockPack(
          start = oldFn(pb.inJumpIndex).asInstanceOf[Jump.StartFun],
          prim = pb, seco = oldFn(sbi), rec = rec,
          branch = branch, isSecoTruBody = isSecoTruBody, exit = oldFn(xbi),
        )
  end detectProperPath

  private def intervalFromBlocks(oldFn: CDFGFun)(using pack: BlockPack): Int =
    val recArgs = pack.rec.args
    val states = pack.prim.states ++ pack.seco.states

    ((
      for
        (toki.Entry(param, _), arg) <- oldFn.params.zipStrict(recArgs)
        if !oldFn.params.exists(_.name == arg)
      yield
        val defState = sche.nodeStates(
          pack.prim.writeMap.getOrElse(arg, pack.seco.writeMap(arg))
        )
        val firstUseState = (
          (pack.prim.readMap.getOrElse(param, Nil) ++ pack.seco.readMap.getOrElse(param, Nil))
            .map(sche.nodeStates)
          ++ Option.when(param == pack.branch.cond)(sche.jumpStates(pack.branch.i)(pack.prim.i))
        ).min
        states.getIndexOf(defState).get - states.getIndexOf(firstUseState).get
    ).iterator ++ {
      val arrAccesses = immutable.MultiDict.from:
        for
          node <- pack.prim.nodes.valuesIterator ++ pack.seco.nodes.valuesIterator
          arr <- node match
            case Node.GetReq(_, _, arr, _) => Some(arr)
            case Node.Put(_, arr, _, _) => Some(arr)
            case _ => None
        yield
          arr -> node.id
      for
        arr <- graph.arrayDefs.keysIterator
        accessStates = arrAccesses.get(arr).map(sche.nodeStates)
        if accessStates.nonEmpty
      yield
        states.getIndexOf(accessStates.last).get - states.getIndexOf(accessStates.head).get + 1
    }).max.clamp(pack.prim.states.size + 1, Int.MaxValue)

  /**
    並列化後の `(
      最初のブロックの並列状態列,
      ラウンドのList[(そのラウンドの並列状態列, そのラウンドの兄弟の末尾再帰に至らない方の並列状態列)]
    )` を返す。並列状態とは、(並列化添字を表すInt, 並列化前の状態を表すState)の組である。
    末尾再帰に至らない状態列は元のexitブロックに続く形で終わる。並列化添字は被らない。
    @param interval パイプラインに流す間隔 (`>= primStates.size`)
    @param primStates 分岐前の状態列
    @param secoStates 分岐後末尾再帰に至る状態列
  **/
  private def parallelizedStates(
    interval: Int, primStates: IndexedSeq[State], secoStates: Seq[State],
    oldFn: CDFGFun
  ): (Seq[ParallelState], Seq[Round]) =
    (
      primStates.map(q => ParallelState(Seq(0 -> q))),
      locally:
        val nRounds = 1 + (secoStates.length - 1) / interval
        for round <- 0 until nRounds yield Round(
          for j <- 0 until interval yield
            ParallelState(
              0.to(round).flatMap(i => secoStates.lift(j + interval * (round - i)).map(i -> _)) ++
              primStates.lift(j - interval + primStates.length).map(round + 1 -> _)
            ),
          0.until(secoStates.length - interval).map(j => ParallelState(
            for
              i <- 0 until round
              q <- secoStates.lift(j + interval * (round - i))
            yield i -> q
          )).filter(_.states.nonEmpty),
        ),
    )
  end parallelizedStates

  private def rewriteTyps()(using newLabs: NewLabs): Unit =
    for ((oldLab, _), newLab) <- newLabs do
      newTypEnv(newLab) = typEnv(oldLab)

  private def buildNewGraph(
    oldFn: CDFGFun, newFn: CDFGFun, primPSs: Seq[ParallelState], rounds: Seq[Round],
  )(using pack: BlockPack, newLabs: NewLabs): Unit =
    branchIndices = 0.to(rounds.size).map(_ => JumpIndex.generate(pack.branch.i))
    secoIndices = rounds.indices.map(_ => BlockIndex.generate(pack.prim.i))
    buildNewPrim(oldFn, newFn, primPSs)
    for (round, i) <- rounds.zipWithIndex do
      buildNewSeco(oldFn, newFn, round, i)
      if i == rounds.size - 1 then
        buildNewLastJump(oldFn, newFn, round, i)
      else
        buildNewMiddleJump(oldFn, newFn, round, i)

  private def buildNewPrim(oldFn: CDFGFun, newFn: CDFGFun, primPSs: Seq[ParallelState])
  (using pack: BlockPack, newLabs: NewLabs): Unit =
    addJumpState(pack.start.i, null, generateState())
    newFn.jumps(pack.start.i) = pack.start

    val newNodes = mutable.Map.empty[NodeID, Node]
    val newSortedNodess = mutable.ListBuffer.empty[Seq[Node]]

    for ps <- primPSs do
      val nodes = registerFromParallelState(ps)
      newNodes ++= nodes.map(_.pairWithID)
      newSortedNodess += nodes

    newFn.blocks(pack.prim.i) = Block(
      pack.prim.i,
      Nil, Nil,
      newNodes.toMap, buildArrayDeps(newSortedNodess), pack.start.i, branchIndices(0),
    )
  end buildNewPrim

  private def buildNewSeco(oldFn: CDFGFun, newFn: CDFGFun, round: Round, ri: Int)
  (using pack: BlockPack, newLabs: NewLabs): Unit =
    val newNodes = mutable.Map.empty[NodeID, Node]
    val newSortedNodess = mutable.ListBuffer.empty[Seq[Node]]

    for ps <- round.secoPSs do
      val nodes = registerFromParallelState(ps)
      newNodes ++= nodes.map(_.pairWithID)
      newSortedNodess += nodes

    newFn.blocks(secoIndices(ri)) = Block(
      secoIndices(ri), Nil, Nil,
      newNodes.toMap, buildArrayDeps(newSortedNodess), branchIndices(ri), branchIndices(ri + 1),
    )

  private def buildNewMiddleJump(oldFn: CDFGFun, newFn: CDFGFun, round: Round, ri: Int)
  (using pack: BlockPack, newLabs: NewLabs): Unit =

    val inputBI = if ri == 0 then pack.prim.i else secoIndices(ri - 1)
    val exitBI = BlockIndex.generate(pack.exit.i)

    newFn.jumps(branchIndices(ri)) = Jump.Branch(
      branchIndices(ri), newLabs(pack.branch.cond, ri), inputBI,
      if pack.isSecoTruBody then secoIndices(ri) else exitBI,
      if pack.isSecoTruBody then exitBI else secoIndices(ri),
    )
    addJumpState(branchIndices(ri), inputBI, newNodeStates(newFn(inputBI).nodes.last._1))

    buildNewExit(oldFn, newFn, round, ri, exitBI)
  end buildNewMiddleJump

  private def buildNewLastJump(oldFn: CDFGFun, newFn: CDFGFun, round: Round, ri: Int)
  (using pack: BlockPack, newLabs: NewLabs): Unit =

    val inputBI = if ri == 0 then pack.prim.i else secoIndices(ri - 1)
    val secoBI = secoIndices(ri)
    val exitBI = BlockIndex.generate(pack.exit.i)

    val secoDefs = newFn(secoBI).defs.toSet
    val names = (
      for
        (origLab -> rj, topLab) <- newLabs.toIndexedSeq
        if !secoDefs(topLab)
        bottomLab <- newLabs.get(origLab -> (rj + 1))
      yield
        topLab -> bottomLab
    ).distinct

    newFn.jumps(branchIndices(ri)) = Jump.ForLoopTop(
      branchIndices(ri), branchIndices(ri + 1), newLabs(pack.branch.cond, ri),
      pack.isSecoTruBody, inputBI,
      secoBI, exitBI,
      names.map(_._1), // FIXME: 未使用変数は？
    )
    addJumpState(branchIndices(ri), inputBI, newNodeStates(newFn(inputBI).nodes.last._1))

    newFn.jumps(branchIndices(ri + 1)) = Jump.ForLoopBottom(
      branchIndices(ri + 1), branchIndices(ri), secoBI,
      names.map(_._2),
    )
    addJumpState(branchIndices(ri + 1), secoBI, generateState())

    buildNewExit(oldFn, newFn, round, ri, exitBI)
  end buildNewLastJump

  private def buildNewExit(
    oldFn: CDFGFun, newFn: CDFGFun, round: Round, ri: Int, exitBI: BlockIndex,
  )(using pack: BlockPack, newLabs: NewLabs): Unit =

    def convLab(lab: Label) = newLabs.getOrElse(lab -> ri, lab)

    def traverseBlock(
      oldBI: BlockIndex, newBI: BlockIndex, newParentJI: JumpIndex,
      newNodes: mutable.Map[NodeID, Node], newSortedNodess: mutable.ListBuffer[Seq[Node]]
    ): Unit =
      val oldBlock = oldFn(oldBI)
      for (st -> nodes) <- oldBlock.stateToNodes(sche).sets do
        val newState = generateState()
        newSortedNodess += (
          for node <- nodes.toSeq yield
            val newID = NodeID.generate()
            newNodeStates(newID) = newState
            val newNode = node.copyWithID(newID).mapLabel(convLab)
            newNodes(newID) = newNode
            newNode
        )

      val newJI = JumpIndex.generate(oldBlock.outJump)
      val newBlock = Block(
        newBI, Nil, Nil, newNodes.toMap, buildArrayDeps(newSortedNodess), newParentJI, newJI,
      )
      newFn.blocks(newBI) = newBlock

      var jumpState =
        if newNodes.isEmpty
          newJumpStates(newParentJI).soleElement._2
        else
          newNodeStates(newNodes.last._1)
      val oldJump = oldFn(oldBlock.outJump)
      oldJump match
        case _: (Jump.StartFun | Jump.Branch | Jump.Merge) =>
        case _ => jumpState = generateState()
      addJumpState(newJI, newBI, jumpState)
      oldJump match
        case Jump.Return(_, value, _) =>
          newFn.jumps(newJI) = Jump.Return(newJI, convLab(value), newBI)
        case j =>
          throw CancelOptimizationException(j.productPrefix)

    locally:
      val newNodes = mutable.Map.empty[NodeID, Node]
      val newSortedNodess = mutable.ListBuffer.empty[Seq[Node]]
      for ps <- round.exitPSs do
        val nodes = registerFromParallelState(ps)
        newNodes ++= nodes.map(_.pairWithID)
        newSortedNodess += nodes
      traverseBlock(pack.exit.i, exitBI, branchIndices(ri), newNodes, newSortedNodess)
  end buildNewExit

  private def buildArrayDeps(sortedNodess: collection.Seq[Seq[Node]]): UnweightedGraph[NodeID] =
    val currentArrayDeps = UnweightedGraph[NodeID]()
    val currentArrayLastGet = mutable.Map.empty[Label, List[NodeID]]
    val currentArrayLastPut = mutable.Map.empty[Label, Option[NodeID]]
    def arrayLastGet(lab: Label): List[NodeID] =
      currentArrayLastGet.getOrElseUpdate(lab, Nil)
    def arrayLastPut(lab: Label): Option[NodeID] =
      currentArrayLastPut.getOrElseUpdate(lab, None)

    for
      nodes <- sortedNodess
      node <- nodes
    do
      currentArrayDeps.addVertex(node.id)
      node match
        case Node.GetReq(_, _, arr, _) =>
          for parent <- arrayLastPut(arr) do
            currentArrayDeps.addEdge(parent -> node.id)
          currentArrayLastGet(arr) = node.id :: arrayLastGet(arr)
        case Node.GetAwa(_, reqID, _, _) =>
          if currentArrayDeps.edges.contains(reqID) // GetReq may belong to another Block
            currentArrayDeps.addEdge(reqID -> node.id)
          else
            throw CancelOptimizationException(s"req_awa_interblock:$reqID,${node.id}") // TODO
        case Node.Put(_, arr, _, _) =>
          for parent <- arrayLastGet(arr).iterator ++ arrayLastPut(arr) do
            currentArrayDeps.addEdge(parent -> node.id)
          currentArrayLastGet(arr) = Nil
          currentArrayLastPut(arr) = Some(node.id)
        case _ =>

    currentArrayDeps
  end buildArrayDeps

  private def generateState(): State =
    stateCnt += 1
    State(stateCnt - 1)

  private def addJumpState(ji: JumpIndex, ibi: BlockIndex, state: State): Unit =
    newJumpStates.getOrElseUpdate(ji, mutable.Map.empty)(ibi) = state

  private def registerFromParallelState(ps: ParallelState)(using newLabs: NewLabs): Seq[Node] =
    val newState = generateState()
    for
      (ri -> q) <- ps.states
      (_ -> node) <- oldStateToNodes(q)
    yield
      val newID = NodeID.generate()
      newNodeStates(newID) = newState
      node.copyWithID(newID).mapLabel(l => newLabs(l, ri)) match
        case newNode: Node.GetReq =>
          val awaID = NodeID.generate()
          oldReqIDToNewReqID(node.id) = newID
          oldReqIDToNewAwaID(node.id) = awaID
          newNode.copy(awa = awaID)
        case newNode: Node.GetAwa =>
          val awaID = oldReqIDToNewAwaID(newNode.req)
          newNodeStates -= newID
          newNodeStates(awaID) = newState
          newNode.copy(id = awaID, req = oldReqIDToNewReqID(newNode.req))
        case newNode => newNode
  end registerFromParallelState
end SimpleRecParallelism

object SimpleRecParallelism:
  final case class BlockPack(
    start: Jump.StartFun, prim: Block, branch: Jump.Branch, isSecoTruBody: Boolean,
    seco: Block, rec: Jump.TailCall, exit: Block,
  )

  final case class ParallelState(states: Seq[(Int, State)])

  final case class Round(secoPSs: Seq[ParallelState], exitPSs: Seq[ParallelState])

  type NewLabs = Map[(Label, Int), Label]

  final case class CancelOptimizationException(val msg: String) extends RuntimeException
