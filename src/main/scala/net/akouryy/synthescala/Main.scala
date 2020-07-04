package net.akouryy.synthescala

import java.nio.charset.StandardCharsets
import java.nio.file.{Paths, Files}
import toki._

object Main:

  lazy val add7: List[Fun] =
    import dsl._
    TastyReflector.reflect:
      def add7(a: U[10], b: U[10], c: U[10], d: U[10], e: U[10], f: U[10], g: U[10]): U[13] =
        a + (b + (c + ((d + e) + (f + g))))

  lazy val fib: List[Fun] =
    import dsl._
    TastyReflector.reflect:
      def fib(n: U[6], a: U[32], b: U[32]): U[32] =
        val n0: U[6] = n + 0
        if n0 == 0
          a
        else
          fib(n0 - 1, a + b, a)

  def main(args: Array[String]): Unit =
    Seq(add7(0), fib(0)).map(f)

  private def f(fun: Fun): Unit =
    reset()
    val (typeEnv, k) = KNormalizer(fun).normalize
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
    val bindings = cdfg.bind.AllocatingBinder(graph, typeEnv, schedule).bind
    // PP.pprintln(bindings)
    val fd = fsmd.Composer(graph, schedule, regAlloc, bindings).compose
    // PP.pprintln(fd)
    val sv = emit.Emitter(graph, regAlloc, bindings, fd).emit
    // println(sv)
    Files.write(Paths.get(s"dist/${fun.name}.sv"), sv.getBytes(StandardCharsets.UTF_8))

    Files.write(Paths.get(s"dist/${fun.name}.dot"),
      cdfg.GraphDrawer(graph, typeEnv, schedule, regAlloc, bindings).draw.getBytes(StandardCharsets.UTF_8),
    )

  private def reset(): Unit =
    Label.reset()
    cdfg.BlockIndex.reset()
    cdfg.JumpIndex.reset()
    cdfg.bind.Calculator.reset()
