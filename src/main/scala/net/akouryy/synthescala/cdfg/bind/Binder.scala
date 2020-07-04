package net.akouryy.synthescala
package cdfg
package bind

object Binder:
  type Bindings = collection.Map[(BlockIndex, Node), Calculator]

enum Calculator:
  val id: Int

  case Add(id: Int, lt: toki.Type, rt: toki.Type)
  case Sub(id: Int, lt: toki.Type, rt: toki.Type)
  case Mul(id: Int, lt: toki.Type, rt: toki.Type)
  case Equal(id: Int, lt: toki.Type, rt: toki.Type)

  def shortString: String = s"""$productPrefix$id"""

object Calculator:
  private var maxCalculatorID = -1

  def reset() = maxCalculatorID = -1

  object Add:
    def apply(lt: toki.Type, rt: toki.Type): Calculator =
      maxCalculatorID += 1
      Calculator.Add(maxCalculatorID, lt, rt)

  object Sub:
    def apply(lt: toki.Type, rt: toki.Type): Calculator =
      maxCalculatorID += 1
      Calculator.Sub(maxCalculatorID, lt, rt)

  object Mul:
    def apply(lt: toki.Type, rt: toki.Type): Calculator =
      maxCalculatorID += 1
      Calculator.Mul(maxCalculatorID, lt, rt)

  object Equal:
    def apply(lt: toki.Type, rt: toki.Type): Calculator =
      maxCalculatorID += 1
      Calculator.Equal(maxCalculatorID, lt, rt)
end Calculator
