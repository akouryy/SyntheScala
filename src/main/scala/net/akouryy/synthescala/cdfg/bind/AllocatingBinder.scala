package net.akouryy.synthescala
package cdfg
package bind

import schedule.Schedule
import scala.collection.{immutable, mutable}

class AllocatingBinder(graph: CDFG, typeEnv: toki.TypeEnv, sche: Schedule):
  def bind: Binder.Bindings =
    val ids = mutable.Map.empty[(BlockIndex, NodeID), Int]
    val calculators = mutable.Map.empty[Int, Calculator]
    val used = mutable.Set.empty[Int]

    def bindCalc(bi: BlockIndex, node: Node, ifNone: => Calculator)
                (perfectFilter: PartialFunction[Calculator, Boolean])
                (mendingFilter: PartialFunction[Calculator, Option[Calculator]]): Unit =
      val id -> _ =
        calculators.find((i, c) =>
          !used(i) && perfectFilter.lift(c).exists(identity)
        ).getOrElse:
          calculators.find { (i, c) =>
            !used(i) && {
              val c2 = mendingFilter.lift(c).flatten
              c2.foreach(calculators(i) = _)
              c2.nonEmpty
            }
          } match
            case Some(i -> _) if i >= 0 => i -> () // c2
            case _ =>
              val calc = ifNone
              calculators(calc.id) = calc
              calc.id -> ()

      ids((bi, node.id)) = id
      used += id
      // calc
    end bindCalc

    for b <- graph.main.blocks.valuesIterator
        (_, nodes) <- b.stateToNodes(sche).sets do
      used.clear()

      nodes.foreach:
        case node @ Node.BinOp(_, op, l, r, _) =>
          val lt = l.fold(typeEnv)((_, typ) => typ)
          val rt = r.fold(typeEnv)((_, typ) => typ)
          bindCalc(b.i, node, Calculator.Bin(op, lt, rt)) {
            case Calculator.Bin(_, `op`, clt, crt) =>
              clt.getMax(lt) == Some(clt) && crt.getMax(rt) == Some(crt)
          } {
            case Calculator.Bin(id, `op`, clt, crt) =>
              for mlt <- clt.getMax(lt); mrt <- crt.getMax(rt) yield
                Calculator.Bin(id, op, mlt, mrt)
          }
        case _ =>
    end for

    ids.view.mapValues(calculators).toMap
  end bind
end AllocatingBinder
