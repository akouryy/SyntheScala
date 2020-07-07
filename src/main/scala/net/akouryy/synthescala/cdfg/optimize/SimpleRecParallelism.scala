package net.akouryy.synthescala
package cdfg
package optimize

class SimpleRecParallelism(graph: CDFG, typEnv: toki.TypeEnv, sche: schedule.Schedule):
  def run(): (toki.TypeEnv, schedule.Schedule) =
    traverseFn(graph.main)
    (typEnv, sche)

  private def traverseFn(fn: CDFGFun): Unit =
    detectProperPath(fn) match
      case None =>
      case Some(ji -> isTruRec) =>
        println(ji -> isTruRec);

  /**
    末尾再帰に至るパスが1つしかなく、さらにそのパス上に分岐が1つしかない場合、そのパスの情報を返す。
    @return 見つかった場合、(その分岐, 分岐後末尾再帰に至る方が真ブロックかどうか)
  **/
  private def detectProperPath(cfn: CDFGFun): Option[(JumpIndex, Boolean)] =
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
        case Jump.Branch(ji, _, _, `bi1`, _) => Some(ji -> true)
        case Jump.Branch(ji, _, _, _, `bi1`) => Some(ji -> false)
        case _ => None

end SimpleRecParallelism
