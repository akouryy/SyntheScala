package net.akouryy.synthescala

/** identifier */
case class Label(str: String) derives Eql

/** identifier */
object Label:
  private var maxTemp: Int = -1

  def temp(): Label =
    maxTemp += 1
    Label(s"@$maxTemp")

  def reset(): Unit = maxTemp = -1
