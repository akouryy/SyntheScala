package net.akouryy.synthescala
package dsl

import scala.compiletime.ops.int._
import scala.util.FromDigits

case class U[A <: Int](val num: Long) derives Eql:
  assert(num >= 0)

  def +[B <: Int](that: U[B]) = U[Max[A, B]](num + that.num)
  def -[B <: Int](that: U[B]) = U[Max[A, B]](num - that.num)
  def *[B <: Int](that: U[B]) = U[Max[A, B]](num * that.num)
  def /[B <: Int](that: U[B]) = U[Max[A, B]](num / that.num)
  def %[B <: Int](that: U[B]) = U[Max[A, B]](num % that.num)
  def <[B <: Int](that: U[B]): Boolean = num < that.num
  def <=[B <: Int](that: U[B]): Boolean = num <= that.num
  def >[B <: Int](that: U[B]): Boolean = num > that.num
  def >=[B <: Int](that: U[B]): Boolean = num >= that.num
  def cast[B <: Int] = U[B](num)

case class S[A <: Int](val num: Long) derives Eql:
  def +[B <: Int](that: S[B]) = S[Max[A, B]](num + that.num)
  def -[B <: Int](that: S[B]) = S[Max[A, B]](num - that.num)
  def *[B <: Int](that: S[B]) = S[Max[A, B]](num * that.num)
  def /[B <: Int](that: S[B]) = S[Max[A, B]](num / that.num)
  def %[B <: Int](that: S[B]) = S[Max[A, B]](num % that.num)
  def <[B <: Int](that: S[B]): Boolean = num < that.num
  def <=[B <: Int](that: S[B]): Boolean = num <= that.num
  def >[B <: Int](that: S[B]): Boolean = num > that.num
  def >=[B <: Int](that: S[B]): Boolean = num >= that.num
  def cast[B <: Int] = S[B](num)

given[A <: Int] as FromDigits.WithRadix[U[A]]:
  def fromDigits(digits: String, radix: Int) = U(java.lang.Long.parseLong(digits, radix))

given[A <: Int] as FromDigits.WithRadix[S[A]]:
  def fromDigits(digits: String, radix: Int) = S(java.lang.Long.parseLong(digits, radix))

extension ConversionFromLong on (i: Long):
  def U[A <: Int]: U[A] = dsl.U[A](i)
  def S[A <: Int]: S[A] = dsl.S[A](i)

extension ArrayAccess on [A, B <: Int](arr: Array[A]):
  def apply(i: U[B]): A = arr(i.num.toInt)
  def update(i: U[B], v: A): Unit = arr(i.num.toInt) = v
