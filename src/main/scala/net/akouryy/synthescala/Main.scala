package net.akouryy.synthescala

import toki._

object Main:

  val add7 =
    import Expr._
    Fun(
      "add7", Type.U[13],
      "abcdefg".map(c => Entry(c.toString, Type.U[10])),
      Bin("+", Ref("a"),
        Bin("+", Ref("b"),
          Bin("+", Ref("c"),
            Bin("+",
              Bin("+", Ref("d"), Ref("e")),
              Bin("+", Ref("f"), Ref("g")),
            )
          )
        )
      )
    )

  val fib =
    import Expr._
    Fun(
      "fib", Type.U[32],
      Seq(Entry("n", Type.U[6]), Entry("a", Type.U[32]), Entry("b", Type.U[32])),
      If(Bin("==", Ref("n"), Num(0)),
        Num(1),
        Call("fib", Seq(
          Bin("-", Ref("n"), Num(1)),
          Bin("+", Ref("a"), Ref("b")),
          Ref("a"),
        )),
      ),
    )

  def main(args: Array[String]): Unit =
    println("Hello world!")
    println(KNormalizer(add7.body))
    println(KNormalizer(fib.body))
