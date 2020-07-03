package net.akouryy.synthescala
package cdfg
package bind

import Binder.Calculator
import schedule.Schedule
import scala.collection.{immutable, mutable}

class AllocatingBinder(graph: CDFG, sche: Schedule):
  def bind: Binder.Bindings =
    val ret = mutable.Map.empty[(BlockIndex, Node), Calculator]
    val calculators = mutable.Set.empty[Calculator]
    val used = mutable.Set.empty[Int]

    def bindCalc(bi: BlockIndex, node: Node, ifNone: => Calculator)
                (filter: PartialFunction[Calculator, Boolean]): Calculator =
      val calc = calculators.find(c =>
        !used(c.id) && filter.lift(c).exists(identity)
      ).getOrElse:
        val calc = ifNone
        calculators += calc
        calc

      ret((bi, node)) = calc
      used += calc.id
      calc
    end bindCalc

    for b <- graph.blocks.valuesIterator
        (_, nodes) <- b.stateToNodes(sche).sets do
      used.clear()

      nodes.foreach:
        case node @ Node.BinOp(op, _, _, _) =>
          (op: @unchecked) match
            case "+" =>
              bindCalc(b.i, node, Calculator.Add(32, 32)):
                case Calculator.Add(_, lb, rb) if lb >= 32 && rb >= 32 => true
            case "-" =>
              bindCalc(b.i, node, Calculator.Sub(32, 32)):
                case Calculator.Sub(_, lb, rb) if lb >= 32 && rb >= 32 => true
            case "==" =>
              bindCalc(b.i, node, Calculator.Equal(32, 32)):
                case Calculator.Equal(_, lb, rb) if lb >= 32 && rb >= 32 => true
        case _ =>
    end for

    ret

  end bind
end AllocatingBinder
