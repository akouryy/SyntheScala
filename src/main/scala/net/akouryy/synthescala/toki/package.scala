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

enum Type(str: => String):
  case Unsigned(width: Int) extends Type(s"U$width")
  case Signed(width: Int) extends Type(s"S$width")

  override def toString = str

object Type:
  def U(width: Int) = Unsigned(width)
  def S(width: Int) = Signed(width)

case class Entry(name: Label, typ: Type):
  override def toString = s"$name:$typ"
