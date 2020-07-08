package net.akouryy.synthescala
package fsmd

import scala.collection.mutable

case class FSMD(
  fsm: collection.SortedMap[State, Transition],
  datapath: Datapath,
)

enum Transition:
  case Always(result: State)
  case Conditional(cond: Source, condReg: ConnPort.Reg, tru: State, fls: State)
  case LinkReg

enum Source:
  case Always(conn: ConnPort.Src)
  case Conditional(reg: Register, tru: ConnPort.Src, fls: ConnPort.Src)

enum ConnPort:
  case CalcIn(id: Int, port: Int) extends ConnPort with ConnPort.Dst
  case CalcOut(id: Int, port: Int) extends ConnPort with ConnPort.Src
  case ArrWriteEnable(arr: Label) extends ConnPort with ConnPort.Dst
  case ArrIndex(arr: Label) extends ConnPort with ConnPort.Dst
  case ArrReadValue(arr: Label) extends ConnPort with ConnPort.Src
  case ArrWriteValue(arr: Label) extends ConnPort with ConnPort.Dst
  case Reg(reg: Register) extends ConnPort with ConnPort.Dst with ConnPort.Src
  case RegStation(reg: Register) extends ConnPort with ConnPort.Dst with ConnPort.Src
  case Const(num: Long) extends ConnPort with ConnPort.Src
  case Inherit extends ConnPort with ConnPort.Src

object ConnPort:
  sealed trait Dst
  sealed trait Src

case class Datapath(
  map: collection.Map[ConnPort.Dst, collection.SortedMap[State, Source]],
)
