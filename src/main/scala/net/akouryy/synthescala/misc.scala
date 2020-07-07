package net.akouryy.synthescala

final case class State(id: Int) extends Ordered[State] derives Eql:
  override def toString = s"q$id"

  def compare(that: State) = this.id.compare(that.id)

  def pred = State(id - 1)
  def succ = State(id + 1)

final case class Register(id: Int) extends Ordered[Register] derives Eql:
  override def toString = s"r${id.toString.replace("-", "_")}"

  def compare(that: Register) = this.id.compare(that.id)

object Register:
  val STATE = Register(-1)

/** identifier */
final case class Label(str: String) extends Ordered[Label] derives Eql:
  def compare(that: Label) = this.str compare that.str

/** identifier */
object Label:
  private var maxTemp: Int = 9

  def temp(): Label =
    maxTemp += 1
    Label("@" + java.lang.Integer.toString(maxTemp, 36))

  def reset(): Unit = maxTemp = 9

enum VC derives Eql:
  case V(v: Label)
  case C(c: Long, typ: toki.Type)

  def fold[T](fv: Label => T)(fc: (Long, toki.Type) => T) = this match
    case V(v) => fv(v)
    case C(c, typ) => fc(c, typ)

  def getV: Option[Label] = fold(Some(_))((_, _) => None)

  def getC: Option[(Long, toki.Type)] = fold(_ => None)(Some(_, _))

  def shortString: String = fold(_.str)((c, _) => c.toString)

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
