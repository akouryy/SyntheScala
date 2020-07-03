package net.akouryy.synthescala
package cdfg
package bind

object Binder:
  type Bindings = collection.Map[(BlockIndex, Node), Calculator]

enum Calculator:
  val id: Int

  case Add(id: Int, lBits: Int, rBits: Int)
  case Sub(id: Int, lBits: Int, rBits: Int)
  case Equal(id: Int, lBits: Int, rBits: Int)

  def shortString: String = s"""$productPrefix$id"""

object Calculator:
  private var maxCalculatorID = -1

  def reset() = maxCalculatorID = -1

  object Add:
    def apply(lBits: Int, rBits: Int): Calculator =
      maxCalculatorID += 1
      Calculator.Add(maxCalculatorID, lBits, rBits)

  object Sub:
    def apply(lBits: Int, rBits: Int): Calculator =
      maxCalculatorID += 1
      Calculator.Sub(maxCalculatorID, lBits, rBits)

  object Equal:
    def apply(lBits: Int, rBits: Int): Calculator =
      maxCalculatorID += 1
      Calculator.Equal(maxCalculatorID, lBits, rBits)
end Calculator
