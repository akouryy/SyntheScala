package net.akouryy.synthescala

final case class State(id: Int) extends Ordered[State] derives Eql:
  override def toString = s"q$id"

  def compare(that: State) = this.id.compare(that.id)

  def succ = State(id + 1)

final case class Register(id: Int) extends Ordered[Register] derives Eql:
  override def toString = s"r$id"

  def compare(that: Register) = this.id.compare(that.id)
