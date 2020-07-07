package net.akouryy.synthescala
package cdfg
package optimize

class SimpleRecParallelism(graph: CDFG, typEnv: toki.TypeEnv)(using sche: schedule.Schedule):
  import SimpleRecParallelism.ParallelState

  def run(): (toki.TypeEnv, schedule.Schedule) =
    traverseFn(using graph.main)
    (typEnv, sche)

  private def traverseFn(using cfn: CDFGFun): Unit =
    detectProperPath match
      case None =>
      case Some(j -> isTruRec) =>
        val prim = cfn(j.inBlock)
        val seco = cfn(if isTruRec then j.truBlock else j.flsBlock)
        val exit = cfn(if isTruRec then j.flsBlock else j.truBlock)
        val pss = parallelizedStates(
          prim.states.size + 1, prim.states, seco.states,
          exit.states.headOption.getOrElse(sche.jumpStates(exit.outJump)(exit.i)),
        )
        println(prim.states.size + 1)
        PP.pprintln(pss)

  /**
    末尾再帰に至るパスが1つしかなく、さらにそのパス上に分岐が1つしかない場合、そのパスの情報を返す。
    @return 見つかった場合、(その分岐, 分岐後末尾再帰に至る方が真ブロックかどうか)
  **/
  private def detectProperPath(using cfn: CDFGFun): Option[(Jump.Branch, Boolean)] =
    val recs = cfn.jumps.values.flatMap:
      case j: Jump.TailCall => Some(j)
      case _ => None

    if recs.sizeIs != 1
      None
    else
      val rec = recs.soleElement
      val b0 = cfn(cfn.blocks.firstKey)
      val bi1 = rec.inBlock
      cfn(b0.outJump) match
        case j @ Jump.Branch(_, _, _, `bi1`, _) => Some(j -> true)
        case j @ Jump.Branch(_, _, _, _, `bi1`) => Some(j -> false)
        case _ => None
  end detectProperPath

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
  : (Seq[ParallelState], Seq[(Seq[ParallelState], Seq[ParallelState])]) =
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

end SimpleRecParallelism

object SimpleRecParallelism:
  case class ParallelState(states: Seq[(Int, State)])
