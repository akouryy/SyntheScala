package net.akouryy.synthescala
package util

object Identity:
  def unapply[T](x: T): Option[T] = Some(x)
