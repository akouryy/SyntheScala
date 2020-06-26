// Forked from https://github.com/cpu2019-5/anscaml/blob/master/src/main/scala/net/akouryy/anscaml/arch/tig/GraphDrawer.scala (MIT License)

package net.akouryy.synthescala
package cdfg

import scala.collection.mutable

class GraphDrawer:
  private val current = new StringBuilder

  private def unsafeEscape(str: String): String =
    str.replaceAll("&", "&nbsp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;")
       .replaceAll("\"", "&quot;").replaceAll("\n", "<br />")

  //noinspection SpellCheckingInspection
  def apply(f: CDFG): String =
    current.clear()
    current ++=
      s"""digraph Program_ {
          |graph [fontname = "Monaco", fontsize = 12, ranksep = 0.5];
          |node [shape = box, fontname = "Monaco", fontsize = 11; colorscheme = pastel19];
          |edge [fontname = "Monaco", fontsize = 11; colorscheme = pastel19];
          |""".stripMargin

    for j <- f.jumps.valuesIterator do
      current ++= j.match
        case Jump.StartFun(i, ob) =>
          s"""$i[label = "StartFun.${i.indexString}"; shape = component];
              |$i -> $ob;
              |""".stripMargin
        case Jump.Return(i, v, ib) =>
          s"""$i[label = "Return.${i.indexString}"; shape = lpromoter];
              |$ib -> $i [label="$v"];
              |""".stripMargin
        case Jump.Branch(i, cond, ib, tob, fob) =>
          s"""$i[
              |  label = "Branch.${i.indexString}";
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
          s"""$i[label = "Merge.${i.indexString}"; shape = invtrapezium; style = rounded];
              |$inputEdges
              |$i -> $ob [label="${ons.mkString(",")}"];
              |""".stripMargin
      // end match
    end for

    for Block(i, nodes, _, _) <- f.blocks.valuesIterator do
      if nodes.isEmpty then
        current ++= s"""$i [label = "$i\\l(0行)"]""" + "\n"
      else
        current ++= s"""$i [label = "$i"];""" + "\n"

    for
      Block(i, nodes, _, _) <- f.blocks.valuesIterator
      if nodes.nonEmpty
    do
      val ids = mutable.Map.empty[Node, Int]
      def id(node: Node) = s"nd${i}_${ids.getOrElseUpdate(node, ids.size)}"

      current ++=
        s"""|subgraph cluster_dfg_$i{
            |node [shape = oval];
            |label = "$i";
            |""".stripMargin

      for nd <- nodes do
        current ++= nd.match
          case Node.Input(n) => s"""${id(nd)} [label="$n: in"];"""
          case Node.Const(v, n) => s"""${id(nd)} [label="$n: $v"];"""
          case Node.Output(_) => s"""${id(nd)} [label="out"];"""
          case Node.BinOp(op, _, _, a) => s"""${id(nd)} [label="$a: $op"];"""
          case Node.Call(fn, args, ret) =>
            s"""${id(nd)} [label="$ret: $fn(${args.mkString(",")})"];"""

      for
        from <- nodes
        a <- from.written
        to <- nodes
        if to.read.contains(a)
      do
        current ++= s"""${id(from)} -> ${id(to)}; """

      current ++= "}"
    end for

    current ++= s"}\n"
    current.toString
  end apply
end GraphDrawer
