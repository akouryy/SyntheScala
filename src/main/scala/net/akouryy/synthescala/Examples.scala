package net.akouryy.synthescala

import dsl.{_, given _}
import toki._

def Examples = Seq[Program](
  TastyReflector.reflect:
    def add7(a: U[10], b: U[10], c: U[10], d: U[10], e: U[13], f: U[10], g: U[13]): U[13] =
      a + (b + (c + ((d + e) + (f + g)))),

  TastyReflector.reflect:
    def fib(n: U[6], a: U[32], b: U[32]): U[32] =
      val n0 = n + 0.U[1]
      if n0 == 0.U[1]
        a
      else
        fib(n0 - 1.U[1], a + b, a),

  TastyReflector.reflect:
    val a = new Array[S[27]](1000)

    def norm2(i: U[10], acc: S[64]): S[64] =
      if i == 1000.U[10]
        acc
      else
        val a64 = a(i).cast[64]
        norm2(i + 1.U[1], acc + a64 * a(i)),

  TastyReflector.reflect:
    val a = new Array[S[27]](1000)
    val b = new Array[S[27]](1000)

    def dotProd(i: U[10], acc: S[64]): S[64] =
      if i == 1000.U[10]
        acc
      else
        val a64 = a(i).cast[64]
        dotProd(i + 1.U[1], acc + a64 * b(i)),

  TastyReflector.reflect:
    val a = new Array[S[64]](1000)

    def accumulate(i: U[10], acc: S[64]): U[1] =
      if i == 1000.U[10]
        0.U[1]
      else
        val b = acc + a(i)
        a(i) = b
        accumulate(i + 1.U[1], b),

  TastyReflector.reflect:
    val a = new Array[U[1]](1)

    def complexIf(i: U[1]): U[2] =
      1.U[2] + (
        if i == 0.U[1]
          val b = 1.U[2]
          a(0.U[1]) = i
          b
        else
          2.U[2]
      ),

  TastyReflector.reflect:
    val a = new Array[S[64]](1)

    def dependencyTest(i: S[64]): S[64] =
      val zero = 0.U[1]
      a(zero) = i
      a(zero) = 1.S[64]
      a(zero) = i
      a(zero) = if a(zero) >= 0.S[1] then a(zero) * 2.S[64] else a(zero) * -3.S[64]
      a(zero),
)
