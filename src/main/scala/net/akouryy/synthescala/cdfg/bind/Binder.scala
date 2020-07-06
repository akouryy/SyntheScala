package net.akouryy.synthescala
package cdfg
package bind

object Binder:
  type Bindings = collection.Map[(BlockIndex, NodeID), Calculator]

enum Calculator:
  val id: Int

  case Bin(id: Int, op: BinOp, lt: toki.Type, rt: toki.Type)

  def shortString: String = s"""$productPrefix$id"""

object Calculator:
  private var maxCalculatorID = -1

  def reset() = maxCalculatorID = -1

  object Bin:
    def apply(op: BinOp, lt: toki.Type, rt: toki.Type): Calculator =
      maxCalculatorID += 1
      Calculator.Bin(maxCalculatorID, op, lt, rt)

    def retTyp(bin: Bin): toki.Type =
      bin.op.calcTyp(bin.lt, bin.rt)

end Calculator
