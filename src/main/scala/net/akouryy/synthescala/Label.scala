package net.akouryy.synthescala

/** identifier */
case class Label(str: String) derives Eql

/** identifier */
object Label:
  private var maxTemp: Int = 9

  def temp(): Label =
    maxTemp += 1
    Label("@" + java.lang.Integer.toString(maxTemp, 36))

  def reset(): Unit = maxTemp = 9
