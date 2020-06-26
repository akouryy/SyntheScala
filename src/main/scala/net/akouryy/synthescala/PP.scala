package net.akouryy.synthescala

import cdfg.Node
import pprint.{PPrinter, Tree}
import Tree.{Apply, Infix, Literal}

val PP: PPrinter = pprint.copy(additionalHandlers = {
  case g: cdfg.CDFG =>
    Apply("CDFG", Seq(g.blocks, g.jumps).iterator.map(PP.treeify))

  case cdfg.BlockIndex(i) => Literal(s"B${i.mkString("-")}")
  case cdfg.JumpIndex(i) => Literal(s"J${i.mkString("-")}")

  case Node.Const(v, n) => Infix(Literal(n), "=N", PP.treeify(v))
  case Node.BinOp(op, l, r, a) =>
    Infix(Literal(a), "=B", Infix(Literal(l), op, Literal(r)))
  case Node.Call(fn, args, ret) =>
    Infix(Literal(ret), "=C", Apply(fn, args.iterator.map(PP.treeify)))

}: PartialFunction[Any, Tree])

val PPBlack = PP.copy(colorLiteral = fansi.Attrs.Empty, colorApplyPrefix = fansi.Attrs.Empty)
