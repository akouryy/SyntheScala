// Forked from https://github.com/cpu2019-5/anscaml/blob/master/src/main/scala/net/akouryy/anscaml/arch/tig/GraphDrawer.scala (MIT License)

package net.akouryy.synthescala
package cdfg

import scala.collection.mutable

class GraphDrawer(
  graph: CDFG, sche: schedule.Schedule = schedule.Schedule(Map.empty, Map.empty),
  regs: bind.RegisterAllocator.Allocations = Map.empty,
  bindings: bind.Binder.Bindings = Map.empty,
):
  private val current = new StringBuilder

  private def unsafeEscape(str: String): String =
    str.replaceAll("&", "&nbsp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;")
       .replaceAll("\"", "&quot;").replaceAll("\n", "<br />")

  private def sup(color: String, escapedText: Any): String =
    s"""<font color="$color" point-size="8"><sup>$escapedText</sup></font>"""

  private def stateStr(state: Option[State | collection.Set[State]]): String =
    state match
      case Some(state: State) => sup("#ff4411", state)
      case Some(state: collection.Set[?]) => sup("#ff4411", state.mkString("|"))
      case None => sup("#ff4411", "q?")

  private def idStr(id: String): String =
    s"""$id${sup("#3311ff", regs.getOrElse(id, "r?"))}"""

  private def (s: String).singleLine: String = s"""\n *""".r.replaceAllIn(s, "")

  //noinspection SpellCheckingInspection
  def draw: String =
    current.clear()
    current ++=
      s"""digraph Program_ {
          |graph [fontname = "Monaco", fontsize = 12, ranksep = 0.5];
          |node [shape = box, fontname = "Monaco", fontsize = 11; colorscheme = pastel19];
          |edge [fontname = "Monaco", fontsize = 11; colorscheme = pastel19];
          |""".stripMargin

    for (ji -> j) <- graph.jumps do
      val label = s"""${j.productPrefix}.${ji.indexString}
                      ${stateStr(sche.jumpStates.get(ji))}
                      """.singleLine

      current ++= j.match
        case Jump.StartFun(i, ob) =>
          s"""$i[label = <$label>; shape = component];
              |$i -> $ob;
              |""".stripMargin
        case Jump.Return(i, v, ib) =>
          s"""$i[label = <$label>; shape = lpromoter];
              |$ib -> $i [label="$v"];
              |""".stripMargin
        case Jump.TailCall(i, fn, args, ib) =>
          s"""$i[label = <$label<br/>$fn(${args.mkString(",")})>; shape = component];
              |$ib -> $i;
              |""".stripMargin
        case Jump.Branch(i, cond, ib, tob, fob) =>
          s"""$i[
              |  label = <$label>;
              |  shape = trapezium; style = rounded;
              |];
              |$ib -> $i;
              |$i -> $tob [label="$cond"];
              |$i -> $fob [label="!$cond"];
              |""".stripMargin
        case Jump.Merge(i, ibs, inss, ob, ons) =>
          val inputEdges =
            ibs.zip(inss).map((ib, ins) =>
              s"""$ib -> $i [label="${ins.mkString(",")}"];"""
            ).mkString
          s"""$i[label = <$label>; shape = invtrapezium; style = rounded];
              |$inputEdges
              |$i -> $ob [label="${ons.mkString(",")}"];
              |""".stripMargin
      // end match
    end for

    for Block(i, nodes, _, _) <- graph.blocks.valuesIterator do
      if nodes.isEmpty then
        current ++= s"""$i [label = "$i\\l(0行)"]""" + "\n"
      else
        current ++= s"""$i [label = "$i"];""" + "\n"

    for
      Block(bi, nodes, _, _) <- graph.blocks.valuesIterator
      if nodes.nonEmpty
    do
      val ids = mutable.Map.empty[Node, Int]
      def id(node: Node) = s"nd${bi}_${ids.getOrElseUpdate(node, ids.size)}"

      current ++=
        s"""|subgraph cluster_dfg_$bi{
            |node [shape = oval];
            |label = "$bi";
            |""".stripMargin

      for nd <- nodes do
        val labelBase = nd match
          case Node.Input(n) => s"""${idStr(n)}:in"""
          case Node.Const(v, n) => s"""${idStr(n)}:$v"""
          case Node.Output(n) => s"""out($n)"""
          case Node.BinOp(op, l, r, a) =>
            val bound = sup("#3311ff", unsafeEscape(
              bindings.get(bi, nd).fold("?")(_.shortString)
            ))
            s"""${idStr(a)}:$l${unsafeEscape(op)}$bound$r"""
          case Node.Call(fn, args, ret) =>
            s"""${idStr(ret)}:$fn(${args.mkString(",")})"""

        current ++=
          s"""${id(nd)} [label=<
                $labelBase
                ${stateStr(sche.getStateOf(graph, bi, nd))}
              >];""".singleLine

      for
        to <- nodes
        read = to.read
        from <- nodes
        a <- from.written
        i = read.indexOf(a)
        if i >= 0
      do
        current ++= s"""${id(from)} -> ${id(to)}; """

      current ++= "}"
    end for

    current ++= s"}\n"
    current.toString
  end draw
end GraphDrawer
