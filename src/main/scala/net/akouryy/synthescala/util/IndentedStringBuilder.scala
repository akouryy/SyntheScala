package net.akouryy.synthescala
package util

class IndentedStringBuilder:
  private val builder = StringBuilder()
  private var level = 0

  override def toString =
    assert(level == 0)
    builder.toString

  def indent(): Unit =
    level += 1
  def outdent(): Unit =
    assert(level >= 1)
    level -= 1

  def indent(fn: => Unit): Unit =
    indent()
    fn
    outdent()

  def indent(start: String, stop: String)(fn: => Unit): Unit =
    if start.nonEmpty then this ++= start
    indent(fn)
    if stop.nonEmpty then this ++= stop

  def ++=(s: String): Unit =
    builder ++= (
      if s.isEmpty
        "\n"
      else
        "  " * level + s + "\n"
    )

  def +=(s: String): Unit = builder ++= s
end IndentedStringBuilder
