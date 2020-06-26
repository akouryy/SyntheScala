package net.akouryy.synthescala.toki

case class Fun(name: String, ret: Type, params: Seq[Entry], body: Expr)

enum Expr:
  case Num(num: Long)
  case Ref(name: String)
  case Bin(op: String, left: Expr, right: Expr)
  // case Update(name: String, expr: Expr)
  case Call(fn: String, args: Seq[Expr])
  case Let(entry: Entry, expr: Expr, body: Expr)
  // case Semi(x1: Expr, x2: Expr)
  case If(cond: Expr, tru: Expr, fls: Expr)

enum Type(str: => String):
  case Unsigned[T <: Int : ValueOf]() extends Type(s"U${valueOf[T]}")
  case Signed[T <: Int : ValueOf]() extends Type(s"S${valueOf[T]}")

  override def toString = str

object Type:
  def U[T <: Int : ValueOf] = Unsigned[T]()
  def S[T <: Int : ValueOf] = Signed[T]()

case class Entry(name: String, typ: Type):
  override def toString = s"$name:$typ"
