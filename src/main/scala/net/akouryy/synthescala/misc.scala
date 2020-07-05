package net.akouryy.synthescala

final case class State(id: Int) extends Ordered[State] derives Eql:
  override def toString = s"q$id"

  def compare(that: State) = this.id.compare(that.id)

  def succ = State(id + 1)

final case class Register(id: Int) extends Ordered[Register] derives Eql:
  override def toString = s"r$id"

  def compare(that: Register) = this.id.compare(that.id)

given[T](using Eql[T, T]) as Eql[Option[T], Option[T]] = Eql.derived

enum BinOp:
  case Add, Sub, Mul, Div, Mod, Eq, Lt, Le, Gt, Ge

  def operatorString = this match
    case Add => "+"
    case Sub => "-"
    case Mul => "*"
    case Div => "/"
    case Mod => "%"
    case Eq => "=="
    case Lt => "<"
    case Le => "<="
    case Gt => ">"
    case Ge => ">="

  def calcTyp(lt: toki.Type, rt: toki.Type): toki.Type =
    import toki.Type._
    (this, lt, rt) match
      case (Add | Sub | Mul | Div | Mod, U(lw), U(rw)) => U(lw.max(rw))
      case (Add | Sub | Mul | Div | Mod, S(lw), S(rw)) => S(lw.max(rw))
      case (Eq | Lt | Le | Gt | Ge, _: (U | S), _: (U | S)) => U(1)
      case _ => !!!((this, lt, rt))

end BinOp

object BinOp:
  def from(str: String): Option[BinOp] =
    Seq(Add, Sub, Mul, Div, Mod, Eq, Lt, Le, Gt, Ge).find(_.operatorString == str)

  object StringificationOf:
    def unapply(str: String): Option[BinOp] = BinOp.from(str)

end BinOp
