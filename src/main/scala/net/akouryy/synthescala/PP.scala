package net.akouryy.synthescala

import arch.sv
import sv.Node
import pprint.{PPrinter, Tree}
import Tree.{Apply, Infix, Literal}

val PP: PPrinter = pprint.copy(additionalHandlers = {
  case g: sv.CDFG =>
    Apply("CDFG", Seq(g.blocks, g.jumps).iterator.map(PP.treeify))

  case sv.BlockIndex(i) => Literal(s"B${i.mkString("-")}")
  case sv.JumpIndex(i) => Literal(s"J${i.mkString("-")}")

  case Node.Const(v, n) => Infix(Literal(n), "=Const", PP.treeify(v))
  case Node.BinOp(op, l, r, a) =>
    Infix(PP.treeify(a), "=BinOP", Infix(Literal(l), op, Literal(r)))
  case Node.Call(fn, args, ret) =>
    Infix(PP.treeify(ret), "=Call", Apply(fn, args.iterator.map(PP.treeify)))

}: PartialFunction[Any, Tree])

val PPBlack = PP.copy(colorLiteral = fansi.Attrs.Empty, colorApplyPrefix = fansi.Attrs.Empty)
