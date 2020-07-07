package net.akouryy.synthescala
package cdfg
package bind

import schedule.Schedule
import scala.collection.{immutable, mutable}

class RegisterAllocator(graph: CDFG, sche: Schedule):
  import RegisterAllocator._

  val interferenceGraph: IGraph =
    val liveInsForState = Liveness.liveInsForState(graph.main, sche)

    mutable.MultiDict.from((
      for
        liveIn <- liveInsForState.valuesIterator
        v <- liveIn
        w <- liveIn
        if v != w
        intf <- Seq(v -> w, w -> v)
      yield intf
    ) ++ (
      for
        b <- graph.main.blocks.valuesIterator
        v <- b.outputs
        w <- b.outputs
        if v != w
        intf <- Seq(v -> w, w -> v)
      yield intf
    ))
  end interferenceGraph

  def allocate(fn: CDFGFun): Allocations =
    val ret = mutable.Map.empty[Label, Register]
    val preferred = mutable.MultiDict.empty[Label, Register]

    def getOrAlloc(id: Label): Register =
      ret.getOrElseUpdate(id, {
        val used = interferenceGraph.get(id).flatMap(ret.get)

        (preferred.get(id).toSeq ++ 0.to(used.size).map(Register(_))).find(r => !used(r)).get
      })

    fn.params.foreach(e => getOrAlloc(e.name))

    for j <- fn.jumps.valuesIterator do j match
      // case Jump.Return(_, lab, _) => preferred += lab -> Register(0)
      case Jump.TailCall(_, _, args, _) =>
        preferred ++= (
          for (arg, i) <- args.zipWithIndex yield arg -> Register(i)
        )
      case _ =>

    for (bi -> b) <- fn.blocks do
      fn.inJump(b) match
        case Jump.Branch(_, cond, _, _, _) => getOrAlloc(cond)
        case _ =>

      b.inputs.foreach(getOrAlloc)
      for node <- b.stateToNodes(sche).values
          w <- node.written
      do getOrAlloc(w)

    ret
  end allocate
end RegisterAllocator

object RegisterAllocator:
  type IGraph = collection.MultiDict[Label, Label]

  type Allocations = collection.Map[Label, Register]
