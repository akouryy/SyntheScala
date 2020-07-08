package net.akouryy.synthescala
package cdfg
package optimize

import toki.Type
import scala.collection.mutable

class SimpleRecParallelism(graph: CDFG, typEnv: toki.TypeEnv)(using sche: schedule.Schedule):
  import SimpleRecParallelism._

  private val newTypEnv = mutable.Map.from(typEnv)

  def run(): (toki.TypeEnv, schedule.Schedule) =
    traverseFn(using graph.main)
    (newTypEnv.toMap, sche)

  private def traverseFn(using cfn: CDFGFun): Unit =
    detectProperPath match
      case None =>
      case Some(pack) =>
        val interval = intervalFromBlocks(pack)
        val (primPSS, rounds) = parallelizedStates(
          interval, pack.prim.states, pack.seco.states,
          pack.exit.states.headOption.getOrElse(sche.jumpStates(pack.exit.outJump)(pack.exit.i)),
        )
        println(interval)
        PP.pprintln(primPSS)
        PP.pprintln(rounds)

        val newLabs = Map.from:
          for
            i <- rounds.indices
            lab <- cfn.params.map(_.name) ++ pack.prim.defs ++ pack.seco.defs
          yield
            (lab, i) -> Label.generate(lab)

        rewriteTyps(newLabs)

  /**
    末尾再帰に至るパスが1つしかなく、さらにそのパス上に分岐が1つしかない場合、そのパスの情報を返す。
  **/
  private def detectProperPath(using cfn: CDFGFun): Option[BlockPack] =
    val recs = cfn.jumps.values.flatMap:
      case j: Jump.TailCall => Some(j)
      case _ => None

    if recs.sizeIs != 1
      None
    else
      val rec = recs.soleElement
      val pb = cfn(cfn.blocks.firstKey)
      val sbi = rec.inBlock
      for
        (branch, xbi, isSecoTruBody) <- cfn(pb.outJump) match
          case j @ Jump.Branch(_, _, _, `sbi`, xbi) => Some((j, xbi, true))
          case j @ Jump.Branch(_, _, _, xbi, `sbi`) => Some((j, xbi, false))
          case _ => None
      yield
        BlockPack(
          start = cfn(pb.inJumpIndex).asInstanceOf[Jump.StartFun],
          prim = pb, seco = cfn(sbi), rec = rec,
          branch = branch, isSecoTruBody = isSecoTruBody, exit = cfn(xbi),
        )
  end detectProperPath

  private def intervalFromBlocks(pack: BlockPack)(using cfn: CDFGFun): Int =
    val recArgs = pack.rec.args
    val states = pack.prim.states ++ pack.seco.states

    (
      for
        (toki.Entry(param, _), arg) <- cfn.params.zipStrict(recArgs)
        if !cfn.params.exists(_.name == arg)
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
    末尾再帰に至らない状態列は並列状態(_, exitState)で終わり、並列化添字は被らない。
    @param interval パイプラインに流す間隔 (`>= primStates.size`)
    @param primStates 分岐前の状態列
    @param secoStates 分岐後末尾再帰に至る状態列
    @param exitState 分岐後末尾再帰に至らない方の最初の状態
  **/
  private def parallelizedStates
  (interval: Int, primStates: IndexedSeq[State], secoStates: Seq[State], exitState: State)
  (using cfn: CDFGFun)
  : (Seq[ParallelState], Seq[Round]) =
    (
      primStates.map(q => ParallelState(Seq(0 -> q))),
      locally:
        val nRounds = 1 + (secoStates.length - 1) / interval
        for round <- 0 until nRounds yield (
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
          )).filter(_.states.nonEmpty) :+
          ParallelState(Seq(round -> exitState)),
        ),
    )
  end parallelizedStates

  private def rewriteTyps(newLabs: Map[(Label, Int), Label]): Unit =
    for ((oldLab, _), newLab) <- newLabs do
      newTypEnv -= oldLab
      newTypEnv(newLab) = typEnv(oldLab)

end SimpleRecParallelism

object SimpleRecParallelism:
  case class BlockPack(
    start: Jump.StartFun, prim: Block, branch: Jump.Branch, isSecoTruBody: Boolean,
    seco: Block, rec: Jump.TailCall, exit: Block,
  )

  case class ParallelState(states: Seq[(Int, State)])

  type Round = (Seq[ParallelState], Seq[ParallelState])
