package net.akouryy.synthescala
package cdfg
package optimize

import toki.Type
import scala.collection.mutable

final class SimpleRecParallelism(graph: CDFG, typEnv: toki.TypeEnv)(using sche: schedule.Schedule):
  import SimpleRecParallelism._

  private var stateCnt = 0
  private val newTypEnv = mutable.Map.from(typEnv)
  private var oldStateToNodes: Map[State, Seq[(BlockIndex, Node)]] = _
  private val newJumpStates = mutable.Map.empty[JumpIndex, mutable.Map[BlockIndex, State]]
  private val newNodeStates = mutable.Map.empty[NodeID, State]
  private var branchIndices: IndexedSeq[JumpIndex] = _
  private var secoIndices: IndexedSeq[BlockIndex] = _

  def run(): (CDFG, toki.TypeEnv, schedule.Schedule) =
    val newMain = CDFGFun(graph.main.fnName, graph.main.retTyp, graph.main.params)

    try
      traverseFn(graph.main, newMain)
      (
        CDFG(graph.arrayDefs, newMain),
        newTypEnv.toMap,
        schedule.Schedule(newJumpStates, newNodeStates),
      )
    catch case CancelOptimizationException(msg) =>
      print(s"${fansi.Color.LightMagenta(s"SRP.cancel:$msg")}; ")
      (graph, typEnv, sche)

  private def traverseFn(oldFn: CDFGFun, newFn: CDFGFun): Unit =
    val oldNodeIDToBlockIndex = oldFn.nodeIDToBlockIndex
    oldStateToNodes = locally:
      val ret = mutable.Map.empty[State, List[(BlockIndex, Node)]]
      for (nid -> st) <- sche.nodeStates do
        val b = oldFn(oldNodeIDToBlockIndex(nid))
        ret(st) = b.i -> b.nodes(nid) :: ret.getOrElse(st, Nil)
      ret.toMap

    given pack as BlockPack = detectProperPath(oldFn).getOrElse:
      throw CancelOptimizationException("detect")

    val interval = intervalFromBlocks(oldFn)
    val (primPSS, rounds) = parallelizedStates(
      interval, pack.prim.states, pack.seco.states,
      oldFn,
    )
    println(interval)
    PP.pprintln(primPSS)
    PP.pprintln(rounds)

    given NewLabs = Map.from:
      for
        i <- 0 to rounds.length
        lab <- oldFn.params.map(_.name) ++ pack.prim.defs ++ pack.seco.defs
      yield
        (lab, i) -> Label.generate(lab)

    rewriteTyps()
    buildNewGraph(oldFn, newFn, primPSS, rounds)
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

    (
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
        val diff = states.indexOf(defState) - states.indexOf(firstUseState)
        println((param, arg, diff))
        diff
    ).max.clamp(pack.prim.states.size + 1, Int.MaxValue)

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
    newFn.jumps(pack.start.i) = pack.start//.mapLabel(l => newLabs(l -> 0))

    val newNodes = mutable.Map.empty[NodeID, Node]

    for ps <- primPSs do
      newNodes ++= registerFromParallelState(ps).map(_.pairWithID)

    newFn.blocks(pack.prim.i) = Block(
      pack.prim.i,
      Nil, Nil, /*pack.prim.inputs.map(newLabs(_, 0)), pack.prim.outputs.map(newLabs(_, 0)),*/
      newNodes.toMap, UnweightedGraph(), pack.start.i, branchIndices(0),
    )
  end buildNewPrim

  private def buildNewSeco(oldFn: CDFGFun, newFn: CDFGFun, round: Round, ri: Int)
  (using pack: BlockPack, newLabs: NewLabs): Unit =
    val newNodes = mutable.Map.empty[NodeID, Node]
    for ps <- round.secoPSs do
      newNodes ++= registerFromParallelState(ps).map(_.pairWithID)
    newFn.blocks(secoIndices(ri)) = Block(
      secoIndices(ri), Nil, Nil,
      newNodes.toMap, UnweightedGraph(), branchIndices(ri), branchIndices(ri + 1),
    )

  private def buildNewMiddleJump(oldFn: CDFGFun, newFn: CDFGFun, round: Round, ri: Int)
  (using pack: BlockPack, newLabs: NewLabs): Unit =
    val exitBI = BlockIndex.generate(pack.exit.i)

    newFn.jumps(branchIndices(ri)) = Jump.Branch(
      branchIndices(ri), newLabs(pack.branch.cond, ri),
      if ri == 0 then pack.prim.i else secoIndices(ri - 1),
      if pack.isSecoTruBody then secoIndices(ri) else exitBI,
      if pack.isSecoTruBody then exitBI else secoIndices(ri),
    )

    buildNewExit(oldFn, newFn, round, ri, exitBI)
  end buildNewMiddleJump

  private def buildNewLastJump(oldFn: CDFGFun, newFn: CDFGFun, round: Round, ri: Int)
  (using pack: BlockPack, newLabs: NewLabs): Unit =
    val exitBI = BlockIndex.generate(pack.exit.i)

    newFn.jumps(branchIndices(ri)) = Jump.ForLoopTop(
      branchIndices(ri), branchIndices(ri + 1), newLabs(pack.branch.cond, ri),
      pack.isSecoTruBody,
      if ri == 0 then pack.prim.i else secoIndices(ri - 1),
      secoIndices(ri), exitBI,
      IndexedSeq.empty, // TODO
    )

    newFn.jumps(branchIndices(ri + 1)) = Jump.ForLoopBottom(
      branchIndices(ri + 1), branchIndices(ri), secoIndices(ri),
      IndexedSeq.empty, // TODO
    )

    buildNewExit(oldFn, newFn, round, ri, exitBI)
  end buildNewLastJump

  private def buildNewExit(
    oldFn: CDFGFun, newFn: CDFGFun, round: Round, ri: Int, exitBI: BlockIndex,
  )(using pack: BlockPack, newLabs: NewLabs): Unit =

    def convLab(lab: Label) = newLabs.getOrElse(lab -> ri, lab)

    def traverseBlock(
      oldBI: BlockIndex, newBI: BlockIndex, newParentJI: JumpIndex,
      newNodes: mutable.Map[NodeID, Node],
    ): Unit =
      val oldBlock = oldFn(oldBI)
      for
        (st -> nodes) <- oldBlock.stateToNodes(sche).sets
        newState = generateState()
        node <- nodes
      do
        val newID = NodeID.generate()
        newNodeStates(newID) = newState
        newNodes(newID) = node.copyWithID(newID).mapLabel(l => newLabs(l, ri))

      val newJI = JumpIndex.generate(oldBlock.outJump)
      newFn.blocks(newBI) = Block(
        newBI, Nil, Nil, newNodes.toMap, UnweightedGraph(), newParentJI, newJI,
      )
      oldFn(oldBlock.outJump) match
        case Jump.Return(_, value, _) =>
          newFn.jumps(newJI) = Jump.Return(newJI, convLab(value), newBI)
        case j =>
          throw CancelOptimizationException(j.productPrefix)

    locally:
      val newNodes = mutable.Map.empty[NodeID, Node]
      for ps <- round.exitPSs do
        newNodes ++= registerFromParallelState(ps).map(_.pairWithID)
      traverseBlock(pack.exit.i, exitBI, branchIndices(ri), newNodes)
  end buildNewExit

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
      node.copyWithID(newID).mapLabel(l => newLabs(l, ri))

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
