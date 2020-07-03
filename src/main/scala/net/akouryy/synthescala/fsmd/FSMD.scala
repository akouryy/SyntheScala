package net.akouryy.synthescala
package fsmd

case class FSMD(fsm: collection.Map[State, Transition], dataPath: DataPath)

enum Transition:
  case Always(result: State)
  case Conditional(reg: Register, tru: State, fls: State)
  case LinkReg

class DataPath
