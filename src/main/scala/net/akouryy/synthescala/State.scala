package net.akouryy.synthescala

final case class State(id: Int) extends Ordered[State]:
  override def toString = s"q$id"

  def compare(that: State) = this.id.compare(that.id)

  def succ = State(id + 1)
