package net.akouryy.synthescala

import scala.quoted._

def !!!(cause: Any): Nothing =
  throw IllegalArgumentException(s"illegal: $cause")
