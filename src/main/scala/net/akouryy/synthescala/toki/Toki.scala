package net.akouryy.synthescala
package toki

import scala.collection.{immutable, mutable}

case class Program(arrayDefs: ArrayDefMap, main: Fun)

type ArrayDefMap = immutable.SortedMap[Label, ArrayDef]

case class ArrayDef(name: Label, elemTyp: Type, length: Int)

case class Fun(name: String, ret: Type, params: Seq[Entry], body: Expr)

enum Expr:
  case Num(num: Long, typ: Type)
  case Ref(name: Label)
  case Bin(op: BinOp, left: Expr, right: Expr)
  // case Update(name: String, expr: Expr)
  case Call(fn: String, args: Seq[Expr])
  case Get(arr: Label, index: Expr)
  case Put(arr: Label, index: Expr, value: Expr, kont: Expr)
  case Let(entry: Entry, expr: Expr, body: Expr)
  // case Semi(x1: Expr, x2: Expr)
  case If(cond: Expr, tru: Expr, fls: Expr)

object Expr:
  object Let_+ :
    def unapply(expr: Expr): Option[(List[(Entry, Expr)], Expr)] =
      expr match
        case Let(entry, bound, kont) =>
          val (bindings, lastKont) = kont match
            case Let_+(bindings, lastKont) => (bindings, lastKont)
            case _ => (Nil, kont)
          Some(((entry -> bound) :: bindings, lastKont))
        case _ => None

enum Type(str: => String) derives Eql:
  case U(width: Int) extends Type(s"U$width")
  case S(width: Int) extends Type(s"S$width")

  override def toString = str

  def getMax(that: Type): Option[Type] = (this, that) match
    case (U(a), U(b)) => Some(U(a max b))
    case (S(a), S(b)) => Some(S(a max b))
    case _ => None
end Type

type TypeEnv = Map[Label, Type]

case class Entry(name: Label, typ: Type):
  override def toString = s"$name:$typ"
