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
          f(prog)
          println()
          ok += 1
        catch err =>
          System.err.print(fansi.Color.Red(s"\n[${prog.main.name}] "))
          err.printStackTrace()
          ng += 1
    println(s"$ok succeeded, $ng failed.")

  private def f(prog: Program): Unit =
    print(fansi.Color.Full(214)(s"[${prog.main.name}] "))
    reset()
    // PP.pprintln(prog)
    print(fansi.Color.Full(69)(s"KN; "))
    val (typeEnv, kProg) = KNormalizer(prog).normalize
    // PP.pprintln(kProg)
    print(fansi.Color.Full(69)(s"SP; "))
    val graph = cdfg.Specializer()(kProg)
    // PP.pprintln(graph)
    print(fansi.Color.Full(69)(s"IO; "))
    cdfg.Liveness.insertInOuts(graph)
    // PP.pprintln(graph)
    print(fansi.Color.Full(69)(s"CO; "))
    cdfg.optimize.Optimizer(graph, typeEnv)
    // PP.pprintln(graph)
    Files.write(Paths.get(s"dist/${prog.main.name}.dot"),
      cdfg.GraphDrawer(graph, typeEnv)
          .draw.getBytes(StandardCharsets.UTF_8),
    )
    print(fansi.Color.Full(69)(s"SC; "))
    val schedule = cdfg.schedule.GorgeousScheduler(graph).schedule
    // PP.pprintln(schedule)
    print(fansi.Color.Full(69)(s"RA; "))
    val regAlloc = cdfg.bind.RegisterAllocator(graph, schedule).allocate(graph.main)
    // PP.pprintln(regAlloc)
    print(fansi.Color.Full(69)(s"BI; "))
    val bindings = cdfg.bind.AllocatingBinder(graph, typeEnv, schedule).bind
    // PP.pprintln(bindings)
    try
      Files.write(Paths.get(s"dist/${prog.main.name}.dot"),
        cdfg.GraphDrawer(graph, typeEnv, schedule, regAlloc, bindings)
            .draw.getBytes(StandardCharsets.UTF_8),
      )
    catch err =>
      System.err.print(fansi.Color.Red(s"\n[GD] "))
      err.printStackTrace()
    print(fansi.Color.Full(69)(s"CP; "))
    val fd = fsmd.Composer(graph, schedule, regAlloc, bindings).compose
    // PP.pprintln(fd)
    print(fansi.Color.Full(69)(s"EM; "))
    val sv = emit.Emitter(graph, regAlloc, bindings, fd).emit
    // println(sv)
    Files.write(Paths.get(s"dist/${prog.main.name}.sv"), sv.getBytes(StandardCharsets.UTF_8))

  private def reset(): Unit =
    Label.reset()
    NodeID.reset()
    cdfg.BlockIndex.reset()
    cdfg.JumpIndex.reset()
    cdfg.bind.Calculator.reset()
