package net.akouryy.synthescala

import java.nio.charset.StandardCharsets
import java.nio.file.{Paths, Files}
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
      Let(Entry("n0", Type.U[6]), Bin("+", Ref("n"), Num(0)),
        If(Bin("==", Ref("n0"), Num(0)),
          Num(1),
          Call("fib", Seq(
            Bin("-", Ref("n0"), Num(1)),
            Bin("+", Ref("a"), Ref("b")),
            Ref("a"),
          )),
        ),
      ),
    )

  def main(args: Array[String]): Unit =
    Seq(add7, fib).map(f)

  private def f(fun: Fun): Unit =
    val k = KNormalizer(fun.body)
    PP.pprintln(k)
    val graph = cdfg.Specializer()(fun.copy(body=k))
    PP.pprintln(graph)
    cdfg.Liveness.insertInOuts(graph)
    PP.pprintln(graph)
    cdfg.optimize.Optimizer(graph)
    PP.pprintln(graph)
    val schedule = cdfg.schedule.GorgeousScheduler(graph).schedule
    PP.pprintln(schedule)
    val regAlloc = cdfg.bind.RegisterAllocator(graph, schedule).allocate
    PP.pprintln(regAlloc)
    val bindings = cdfg.bind.AllocatingBinder(graph, schedule).bind
    println(bindings)
    Files.write(Paths.get(s"dist/${fun.name}.dot"),
      cdfg.GraphDrawer(graph, schedule, regAlloc, bindings).draw.getBytes(StandardCharsets.UTF_8),
    )
