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
          ok += 1
        catch err =>
          err.printStackTrace()
          ng += 1
    println(s"$ok succeeded, $ng failed.")

  private def f(prog: Program): Unit =
    println(s"[${prog.main.name}]")
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
