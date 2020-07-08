// Partially forked from https://github.com/cpu2019-5/anscaml/blob/master/src/main/scala/net/akouryy/anscaml/base/ID.scala

package net.akouryy.synthescala

import scala.collection.mutable

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
  private[this] val cntMap = mutable.Map[String, Int]()

  def suffix(c0: Int): String =
    assert(c0 >= 0)
    var c = c0
    var doNext = true
    val res = new StringBuilder
    while (doNext) {
      doNext = c >= 26
      val start = 'a'
      res += (start + c % 26).toChar
      c = c / 26 - 1
    }
    res.reverseInPlace().toString

  private def generateBase(str: String): String =
    val c = cntMap.getOrElse(str, -1) + 1
    cntMap(str) = c
    s"${str}${suffix(c)}"

  def generate(lab: Label): Label = Label(generateBase(lab.str + "<") + ">")
  def temp(): Label = Label(generateBase("@"))

  def reset(): Unit = cntMap.clear()

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
