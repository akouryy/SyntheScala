package net.akouryy.synthescala

import cdfg.Node
import pprint.{PPrinter, Tree}
import Tree.{Apply, Infix, Literal}

val PP: PPrinter = pprint.copy(additionalHandlers = { obj =>
  import PP.treeify

  obj match
  case Register(r) => Literal(s"%r$r")
  case State(q) => Literal(s"q$q")

  case toki.Expr.Let_+(bindings, kont) =>
    Apply(
      "Let+",
      bindings.iterator.map(ex => Infix(treeify(ex._1), ":=", treeify(ex._2))) ++
        Seq(PP.treeify(kont))
    )

  case g: cdfg.CDFG =>
    Apply("CDFG", Seq(g.params, g.blocks, g.jumps).iterator.map(PP.treeify))

  case cdfg.BlockIndex(i) => Literal(s"B${i.mkString("-")}")
  case cdfg.JumpIndex(i) => Literal(s"J${i.mkString("-")}")

  case Node.Const(v, n) => Infix(Literal(n.str), "=N", PP.treeify(v))
  case Node.BinOp(op, l, r, a) =>
    Infix(Literal(a.str), "=B", Infix(Literal(l.str), op, Literal(r.str)))
  case Node.Call(fn, args, ret) =>
    Infix(Literal(ret.str), "=C", Apply(fn, args.iterator.map(PP.treeify)))

  case fsmd.Datapath(map) =>
    Apply("Datapath", map.iterator.map((k, v) => Infix(PP.treeify(k), ":=", PP.treeify(v))))

}: PartialFunction[Any, Tree])

val PPBlack = PP.copy(colorLiteral = fansi.Attrs.Empty, colorApplyPrefix = fansi.Attrs.Empty)
