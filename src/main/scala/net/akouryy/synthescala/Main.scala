package net.akouryy.synthescala

import java.nio.charset.StandardCharsets
import java.nio.file.{Paths, Files}
import toki._

object Main:

  lazy val add7: Program =
    import dsl._
    TastyReflector.reflect:
      def add7(a: U[10], b: U[10], c: U[10], d: U[10], e: U[13], f: U[10], g: U[13]): U[13] =
        a + (b + (c + ((d + e) + (f + g))))

  lazy val fib: Program =
    import dsl._
    TastyReflector.reflect:
      def fib(n: U[6], a: U[32], b: U[32]): U[32] =
        val n0: U[6] = n + 0
        if n0 == 0
          a
        else
          fib(n0 - 1, a + b, a)

  lazy val norm2: Program =
    import dsl._
    TastyReflector.reflect:
      val a = new Array[S[32]](1000)

      def norm2(i: U[10], acc: S[64]): S[64] =
        if i == 1000
          acc
        else
          val a64: S[64] = a(i)
          norm2(i + 1, acc + a64 * a(i))

  lazy val dotProd: Program =
    import dsl._
    TastyReflector.reflect:
      val a = new Array[S[32]](1000)
      val b = new Array[S[32]](1000)

      def dotProd(i: U[10], acc: S[64]): S[64] =
        if i == 1000
          acc
        else
          val a64: S[64] = a(i)
          dotProd(i + 1, acc + a64 * b(i))

  lazy val accumulate: Program =
    import dsl._
    TastyReflector.reflect:
      val a = new Array[S[32]](1000)

      def accumulate(i: U[10], acc: S[64]): S[32] =
        if i == 1000
          a(800)
        else
          val b: S[64] = acc + a(i)
          a(i) = b
          accumulate(i + 1, b)

  def main(args: Array[String]): Unit =
    var ok, ng = 0
    Seq(add7, fib, norm2, dotProd, accumulate).map:
      prog =>
        try
          f(prog)
          ok += 1
        catch err =>
          err.printStackTrace()
          ng += 1
    println(s"$ok succeeded, $ng failed.")

  private def f(prog: Program): Unit =
    reset()
    // PP.pprintln(prog)
    val (typeEnv, kProg) = KNormalizer(prog).normalize
    // PP.pprintln(kProg)
    val graph = cdfg.Specializer()(kProg)
    // PP.pprintln(graph)
    cdfg.Liveness.insertInOuts(graph)
    // PP.pprintln(graph)
    cdfg.optimize.Optimizer(graph)
    // PP.pprintln(graph)
    val schedule = cdfg.schedule.GorgeousScheduler(graph).schedule
    // PP.pprintln(schedule)
    val regAlloc = cdfg.bind.RegisterAllocator(graph, schedule).allocate(graph.main)
    // PP.pprintln(regAlloc)
    val bindings = cdfg.bind.AllocatingBinder(graph, typeEnv, schedule).bind
    // PP.pprintln(bindings)
    Files.write(Paths.get(s"dist/${prog.main.name}.dot"),
      cdfg.GraphDrawer(graph, typeEnv, schedule, regAlloc, bindings)
          .draw.getBytes(StandardCharsets.UTF_8),
    )
    val fd = fsmd.Composer(graph, schedule, regAlloc, bindings).compose
    // PP.pprintln(fd)
    val sv = emit.Emitter(graph, regAlloc, bindings, fd).emit
    // println(sv)
    Files.write(Paths.get(s"dist/${prog.main.name}.sv"), sv.getBytes(StandardCharsets.UTF_8))

  private def reset(): Unit =
    Label.reset()
    cdfg.BlockIndex.reset()
    cdfg.JumpIndex.reset()
    cdfg.bind.Calculator.reset()
