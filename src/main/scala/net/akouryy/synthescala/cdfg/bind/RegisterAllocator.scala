package net.akouryy.synthescala
package cdfg
package bind

import schedule.Schedule
import scala.collection.{immutable, mutable}

class RegisterAllocator(graph: CDFG, sche: Schedule):
  import RegisterAllocator._

  val interferenceGraph: IGraph =
    val liveInsForState = Liveness.liveInsForState(graph.main, sche)

    mutable.MultiDict.from:
      for liveIn <- liveInsForState.valuesIterator
          v <- liveIn
          w <- liveIn
          if v != w
          intf <- Seq(v -> w, w -> v)
      yield intf
  end interferenceGraph

  def allocate(fn: CDFGFun): Allocations =
    val ret = mutable.Map.empty[Label, Register]

    def getOrAlloc(id: Label): Register =
      ret.getOrElseUpdate(id, {
        val used = interferenceGraph.get(id).flatMap(ret.get)

        Register(0.to(used.size).find(i => !used(Register(i))).get)
      })

    fn.params.foreach(getOrAlloc)

    for (bi -> b) <- fn.blocks do
      fn.inJump(b) match
        case Jump.Branch(_, cond, _, _, _) => getOrAlloc(cond)
        case _ =>

      for node <- b.inputs.iterator ++ b.stateToNodes(sche).values
          w <- node.written
      do getOrAlloc(w)

    ret
  end allocate
end RegisterAllocator

object RegisterAllocator:
  type IGraph = collection.MultiDict[Label, Label]

  type Allocations = collection.Map[Label, Register]
