// Forked from https://github.com/cpu2019-5/anscaml/blob/master/src/main/scala/net/akouryy/anscaml/arch/tig/GraphDrawer.scala (MIT License)

package net.akouryy.synthescala
package arch.sv

import scala.collection.mutable

class GraphDrawer:
  private val current = new StringBuilder

  private def unsafeEscape(str: String): String = {
    str.replaceAll("&", "&nbsp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;")
      .replaceAll("\"", "&quot;").replaceAll("\n", "<br />")
  }

  //noinspection SpellCheckingInspection
  def apply(f: CDFG): String =
    current.clear()
    current ++=
    s"""digraph Program_ {
        |graph [fontname = "Monaco", fontsize = 12, ranksep = 0.5];
        |node [shape = box, fontname = "Monaco", fontsize = 11; colorscheme = pastel19];
        |edge [fontname = "Monaco", fontsize = 11; colorscheme = pastel19];
        |""".stripMargin

    for (j <- f.jumps.valuesIterator) {
      current ++= (j match {
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
              |  label = "Branch.${i.indexString}(${cond})";
              |  shape = trapezium; style = rounded;
              |];
              |$ib -> $i;
              |$i -> $tob [label=true];
              |$i -> $fob [label=false];
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
      })
    }

    for (Block(i, nodes, _, _) <- f.blocks.valuesIterator) {
      if (nodes.isEmpty) {
        current ++= s"""$i [label = "$i\\l(0行)"]""" + "\n"
      } else {
        current ++=
          s"""$i [shape = plain; label = <
              |<table border="0" cellborder="1" cellspacing="0">
              |  <tr><td align="left" balign="left" valign="top">${
                   unsafeEscape(PPBlack.tokenize(nodes).mkString)
                 }</td></tr>
              |</table>
              |>];""".stripMargin
      }
    }
    current ++= s"}\n"
    current.toString
  end apply
end GraphDrawer
