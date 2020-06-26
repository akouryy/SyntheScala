package net.akouryy.synthescala

object ID:
  private var maxTemp: Int = -1

  def temp(): String =
    maxTemp += 1
    s"~$maxTemp"
