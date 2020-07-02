package net.akouryy.synthescala
package cdfg
package bind

import schedule.Scheduler.Schedule
import scala.collection.{immutable, mutable}

class RegisterAllocator(graph: CDFG, sche: Schedule):
  type IGraph = collection.MultiDict[String, String]

  def interferenceGraph: IGraph =
    val liveInsForState = Liveness.liveInsForState(graph, sche)

    mutable.MultiDict.from:
      for
        liveIn <- liveInsForState.valuesIterator
        v <- liveIn
        w <- liveIn
        if v != w
        intf <- Seq(v -> w, w -> v)
      yield intf
  end interferenceGraph
