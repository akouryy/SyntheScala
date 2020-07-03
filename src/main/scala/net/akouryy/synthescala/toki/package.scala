package net.akouryy.synthescala
package toki

case class Fun(name: String, ret: Type, params: Seq[Entry], body: Expr)

enum Expr:
  case Num(num: Long)
  case Ref(name: Label)
  case Bin(op: String, left: Expr, right: Expr)
  // case Update(name: String, expr: Expr)
  case Call(fn: String, args: Seq[Expr])
  case Let(entry: Entry, expr: Expr, body: Expr)
  // case Semi(x1: Expr, x2: Expr)
  case If(cond: Expr, tru: Expr, fls: Expr)

object Expr:
  object Bin:
    def calcTyp(op: String, lt: Type, rt: Type): Type =
      import Type._
      (op, lt, rt) match
        case ("+" | "-" | "*" | "/" | "%", U(lw), U(rw)) => U(lw.max(rw))
        case ("+" | "-" | "*" | "/" | "%", S(lw), S(rw)) => S(lw.max(rw))
        case ("==" | "<", _: (U | S), _: (U | S)) => U(1)
        case _ => ???

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
