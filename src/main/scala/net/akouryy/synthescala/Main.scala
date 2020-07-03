package net.akouryy.synthescala

import java.nio.charset.StandardCharsets
import java.nio.file.{Paths, Files}
import toki._

object Main:

  val add7 =
    import Expr._
    Fun(
      "add7", Type.U(13),
      "abcdefg".map(c => Entry(Label(c.toString), Type.U(10))),
      Bin("+", Ref(Label("a")),
        Bin("+", Ref(Label("b")),
          Bin("+", Ref(Label("c")),
            Bin("+",
              Bin("+", Ref(Label("d")), Ref(Label("e"))),
              Bin("+", Ref(Label("f")), Ref(Label("g"))),
            )
          )
        )
      )
    )

  val fib =
    import Expr._
    Fun(
      "fib", Type.U(32),
      Seq(Entry(Label("n"), Type.U(6)), Entry(Label("a"), Type.U(32)), Entry(Label("b"), Type.U(32))),
      Let(Entry(Label("n0"), Type.U(6)), Bin("+", Ref(Label("n")), Num(0)),
        If(Bin("==", Ref(Label("n0")), Num(0)),
          Ref(Label("a")),
          Call("fib", Seq(
            Bin("-", Ref(Label("n0")), Num(1)),
            Bin("+", Ref(Label("a")), Ref(Label("b"))),
            Ref(Label("a")),
          )),
        ),
      ),
    )

  def main(args: Array[String]): Unit =
    Seq(add7, fib).map(f)

  private def f(fun: Fun): Unit =
    reset()
    val k = KNormalizer(fun.body)
    // PP.pprintln(k)
    val graph = cdfg.Specializer()(fun.copy(body=k))
    // PP.pprintln(graph)
    cdfg.Liveness.insertInOuts(graph)
    // PP.pprintln(graph)
    cdfg.optimize.Optimizer(graph)
    // PP.pprintln(graph)
    val schedule = cdfg.schedule.GorgeousScheduler(graph).schedule
    // PP.pprintln(schedule)
    val regAlloc = cdfg.bind.RegisterAllocator(graph, schedule).allocate
    // PP.pprintln(regAlloc)
    val bindings = cdfg.bind.AllocatingBinder(graph, schedule).bind
    // PP.pprintln(bindings)
    val fd = fsmd.Composer(graph, schedule, regAlloc, bindings).compose
    // PP.pprintln(fd)
    val sv = emit.Emitter(graph, regAlloc, bindings, fd).emit
    // println(sv)
    Files.write(Paths.get(s"dist/${fun.name}.sv"), sv.getBytes(StandardCharsets.UTF_8))

    Files.write(Paths.get(s"dist/${fun.name}.dot"),
      cdfg.GraphDrawer(graph, schedule, regAlloc, bindings).draw.getBytes(StandardCharsets.UTF_8),
    )

  private def reset(): Unit =
    Label.reset()
    cdfg.BlockIndex.reset()
    cdfg.JumpIndex.reset()
    cdfg.bind.Calculator.reset()
