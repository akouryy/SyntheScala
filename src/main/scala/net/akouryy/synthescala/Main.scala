package net.akouryy.synthescala

import java.nio.charset.StandardCharsets
import java.nio.file.{Paths, Files}
import toki._

object Main:
  def main(args: Array[String]): Unit =
    var ok, ng = 0
    Examples.map:
      prog =>
        try
          f(prog, true)
          println()
          f(prog, false)
          println()
          ok += 1
        catch err =>
          System.err.print(fansi.Color.Red(s"\n[${prog.main.name}] "))
          err.printStackTrace()
          ng += 1
    println(s"$ok succeeded, $ng failed.")

  private def f(prog: Program, srp: Boolean): Unit =
    val srpSuffix = if srp then ".srp" else ""
    print(fansi.Color.Full(214)(s"[${prog.main.name}$srpSuffix] "))
    reset()
    // PP.pprintln(prog)
    print(fansi.Color.Full(69)(s"KN; "))
    val (typeEnv1, kProg) = KNormalizer(prog).normalize
    // PP.pprintln(kProg)
    print(fansi.Color.Full(69)(s"SP; "))
    val graph1 = cdfg.Specializer()(kProg)
    // PP.pprintln(graph1)
    print(fansi.Color.Full(69)(s"IO; "))
    cdfg.Liveness.insertInOuts(graph1)
    // PP.pprintln(graph1)
    print(fansi.Color.Full(69)(s"CO; "))
    cdfg.optimize.Optimizer(graph1, typeEnv1)
    // PP.pprintln(graph1)
    print(fansi.Color.Full(69)(s"SC; "))
    val schedule1 = cdfg.schedule.GorgeousScheduler(graph1).schedule
    // PP.pprintln(schedule)
    print(fansi.Color.Full(69)(s"SO; "))
    val (graph2, typeEnv2, schedule2) =
      if srp
        cdfg.optimize.ScheduledOptimizer(graph1, typeEnv1, schedule1)
      else
        (graph1, typeEnv1, schedule1)
    // PP.pprintln(graph2)
    // PP.pprintln(schedule2)
    if graph1 ne graph2
      print(fansi.Color.Full(69)(s"IO2; "))
      cdfg.Liveness.insertInOuts(graph2)
    // PP.pprintln(graph2)
    print(fansi.Color.Full(69)(s"RA; "))
    val regAlloc = cdfg.bind.RegisterAllocator(graph2, schedule2).allocate(graph2.main)
    // PP.pprintln(regAlloc)
    print(fansi.Color.Full(69)(s"BI; "))
    val bindings = cdfg.bind.AllocatingBinder(graph2, typeEnv2, schedule2).bind
    // PP.pprintln(bindings)
    try
      Files.write(Paths.get(s"dist/${prog.main.name}$srpSuffix.dot"),
        cdfg.GraphDrawer(graph2, typeEnv2, schedule2, regAlloc, bindings)
            .draw.getBytes(StandardCharsets.UTF_8),
      )
    catch err =>
      System.err.print(fansi.Color.Red(s"\n[GD] "))
      err.printStackTrace()
    print(fansi.Color.Full(69)(s"CP; "))
    val fd = fsmd.Composer(graph2, schedule2, regAlloc, bindings).compose
    // PP.pprintln(fd)
    print(fansi.Color.Full(69)(s"EM; "))
    val sv = emit.Emitter(graph2, regAlloc, bindings, fd).emit
    // println(sv)
    Files.write(Paths.get(s"dist/${prog.main.name}$srpSuffix.sv"), sv.getBytes(StandardCharsets.UTF_8))

  private def reset(): Unit =
    Label.reset()
    NodeID.reset()
    cdfg.BlockIndex.reset()
    cdfg.JumpIndex.reset()
    cdfg.bind.Calculator.reset()
