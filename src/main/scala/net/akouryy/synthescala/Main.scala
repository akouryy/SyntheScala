package net.akouryy.synthescala

import java.nio.charset.StandardCharsets
import java.nio.file.{Paths, Files}
import arch.sv.Specializer
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
    Seq(add7, fib).map(f)

  private def f(fun: Fun): Unit =
    val k = KNormalizer(fun.body)
    PP.pprintln(k)
    val cdfg = Specializer()(fun.copy(body=k))
    PP.pprintln(cdfg)
    Files.write(Paths.get(s"dist/${fun.name}.dot"),
      arch.sv.GraphDrawer()(cdfg).getBytes(StandardCharsets.UTF_8),
    )
