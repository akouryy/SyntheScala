// Forked from https://github.com/cpu2019-5/anscaml/blob/master/src/main/scala/net/akouryy/anscaml/arch/tig/GraphDrawer.scala (MIT License)

package net.akouryy.synthescala
package cdfg

import scala.collection.mutable

class GraphDrawer(
  graph: CDFG, typeEnv: toki.TypeEnv = Map.empty,
  sche: schedule.Schedule = schedule.Schedule(Map.empty, Map.empty),
  regs: bind.RegisterAllocator.Allocations = Map.empty,
  bindings: bind.Binder.Bindings = Map.empty,
):
  private def unsafeEscape(str: String): String =
    str.replaceAll("&", "&nbsp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;")
       .replaceAll("\"", "&quot;").replaceAll("\n", "<br />")

  private def e(str: String) = unsafeEscape(str)

  private def sub(color: String, escapedText: Any): String =
    s"""<font color="$color" point-size="8"><sub>$escapedText</sub></font>"""

  private def sup(color: String, escapedText: Any): String =
    s"""<font color="$color" point-size="8"><sup>$escapedText</sup></font>"""

  private def stateStr(state: Option[State | collection.Set[State]]): String =
    state match
      case Some(state: State) => sup("#ff4411", state)
      case Some(state: collection.Set[?]) => sup("#ff4411", state.mkString("|"))
      case None => sup("#ff4411", "q?")

  private def baseLabStr(id: Label): String =
    """&lt;(\w+)&gt;""".r.replaceAllIn(
      unsafeEscape(id.str),
      matc => sub("#999999", matc.group(1)),
    )

  private def idStr(id: Label): String =
    baseLabStr(id) +
      typeEnv.get(id).fold("")(t => sup("#00aa11", unsafeEscape(t.toString))) +
      sup("#3311ff", unsafeEscape(regs.getOrElse(id, "r?").toString))

  private def vcStr(vc: VC): String = vc.fold(v => baseLabStr(v)):
    (c, typ) => unsafeEscape(c.toString) + sup("#00aa11", unsafeEscape(typ.toString))

  private def (s: String).singleLine: String = s"""\n *""".r.replaceAllIn(s, "")

  //noinspection SpellCheckingInspection
  def draw: String =
    val r = util.IndentedStringBuilder()
    val forColors = mutable.Map[JumpIndex, Int]()

    r.indent("digraph Program_ {", "}"):
      r ++= """graph [fontname = "Monaco", fontsize = 12, ranksep = 0.5];"""
      r ++= """node [shape = box, fontname = "Monaco", fontsize = 11; colorscheme = pastel19];"""
      r ++= """edge [fontname = "Monaco", fontsize = 11; colorscheme = pastel19];"""

      for (ji -> j) <- graph.main.jumps do
        val label = s"""${stateStr(sche.jumpStates.get(ji).map(_.values.toSet))}
                        ${j.productPrefix}.${ji.indexString}
                        """.singleLine

        j.match
          case Jump.StartFun(i, ob) =>
            r ++= s"$i[label = <$label<br/>" +
                  s"${graph.main.params.map(e => idStr(e.name)).mkString(",")}>; " +
                   "shape = component];"
            r ++= s"$i -> $ob;"
          case Jump.Return(i, v, ib) =>
            r ++= s"$i[label = <$label>; shape = lpromoter];"
            r ++= s"""$ib -> $i [label=<${baseLabStr(v)}>];"""
          case Jump.TailCall(i, fn, args, ib) =>
            r ++= s"$i[label = <$label<br/>$fn(${args.map(baseLabStr).mkString(",")})>; " +
                   "shape = component];"
            r ++= s"$ib -> $i;"
          case Jump.Branch(i, cond, ib, tob, fob) =>
            r.indent(s"$i[", "];"):
              r ++= s"label = <$label>;"
              r ++= s"shape = trapezium; style = rounded;"
            r ++= s"$ib -> $i;"
            r ++= s"""$i -> $tob [label=<${baseLabStr(cond)}>];"""
            r ++= s"""$i -> $fob [label=<!${baseLabStr(cond)}>];"""
          case Jump.Merge(i, ibs, inss, ob, ons) =>
            r ++= s"$i[label = <$label>; shape = invtrapezium; style = rounded];"
            for (ib, ins) <- ibs.zip(inss) do
              r ++= s"""$ib -> $i [label=<${ins.map(baseLabStr).mkString(",")}>];"""
            r ++= s"""$i -> $ob [label=<${ons.map(baseLabStr).mkString(",")}>];"""
          case Jump.ForLoopTop(i, bottom, cond, isSecoTru, ibi, sbi, xbi, names) =>
            forColors(i) = forColors.size % 5 + 1
            val secoStr = if isSecoTru then baseLabStr(cond) else s"!${baseLabStr(cond)}"
            val exitStr = if isSecoTru then s"!${baseLabStr(cond)}" else baseLabStr(cond)
            val namesStr = names.map(idStr).mkString(",")
            r.indent(s"$i[", "];"):
              r ++= s"""label = <$label>;"""
              r ++= "shape = house; style = filled;"
              r ++= s"fillcolor = ${forColors(i)};"
            r ++= s"""$ibi -> $i [label=<$namesStr>; fontcolor=${forColors(i)}];"""
            r ++= s"""$i -> $sbi [label = <$secoStr>];"""
            r ++= s"""$bottom -> $i [constraint = false; color = ${forColors(i)}];"""
            r ++= s"""$i -> $xbi [label = <$exitStr>];"""
          case Jump.ForLoopBottom(i, top, ibi, names) =>
            val namesStr = names.map(idStr).mkString(",")
            r.indent(s"$i[", "];"):
              r ++= s"""label = <$label>"""
              r ++= "shape = invhouse; style = filled;"
              r ++= s"fillcolor = ${forColors(top)};"
            r ++= s"""$ibi -> $i [label=<$namesStr>; fontcolor=${forColors(top)}];"""
      end for

      for Block(i, _, _, nodes, _, _, _) <- graph.main.blocks.valuesIterator do
        if nodes.isEmpty then
          r ++= s"""$i [label = "$i\\l(0行)"];"""
        else
          r ++= s"""$i [label = "$i"];"""

      for
        Block(bi, inputs, outputs, nodes, arrayDeps, _, _) <- graph.main.blocks.valuesIterator
        // if nodes.nonEmpty
      do
        val ids = mutable.Map.empty[Node, Int]

        r.indent(s"subgraph cluster_dfg_$bi {", "}"):
          r ++= s"node [shape = oval];"
          r ++= s"""label = <$bi<br/>(${inputs.map(idStr).mkString(",")}<br/>=&gt;<br/>""" +
                s"""${outputs.map(idStr).mkString(",")})>;"""

          if nodes.isEmpty
            r ++= "dummy [style=invis];"

          for nd <- nodes.valuesIterator do
            val labelBase = nd match
              case Node.Nop(_) => "nop"
              case Node.Const(_, v, n) => s"""${idStr(n)}:$v"""
              case Node.BinOp(nid, op, l, r, a) =>
                val bound = sup("#3311ff", unsafeEscape(
                  bindings.get(bi, nid).fold("?")(_.shortString)
                ))
                s"""${idStr(a)}:${vcStr(l)}${unsafeEscape(op.operatorString)}$bound${vcStr(r)}"""
              case Node.Call(_, fn, args, ret) =>
                s"""${idStr(ret)}:$fn(${args.map(baseLabStr).mkString(",")})"""
              case Node.GetReq(_, _, arr, index) =>
                s"""req ${baseLabStr(arr)}[${baseLabStr(index)}&#93;"""
              case Node.GetAwa(_, _, arr, ret) =>
                s"""${idStr(ret)}:${baseLabStr(arr)}[&#93;"""
              case Node.Put(_, arr, index, value) =>
                s"""${baseLabStr(arr)}[${baseLabStr(index)}&#93;=${baseLabStr(value)}"""

            r ++=
              s"""${nd.id} [label=<
                    ${stateStr(sche.nodeStates.get(nd.id))}
                    $labelBase
                  >];""".singleLine

          (
            for
              (toID, to) <- nodes.toSeq
              (fromID, from) <- nodes
              a <- from.written
              if to.read.contains(a)
            yield fromID -> toID
          ).toSeq.sorted.foreach((f, t) =>
            r ++= s"""$f -> $t;"""
          )

          (
            for
              fromID <- nodes.keysIterator
              if nodes(fromID).isMemoryRelated
              to <- arrayDeps.goForward(fromID)
            yield fromID -> to
          ).toSeq.sorted.foreach { (f, t) =>
            val style =
              nodes(f) match
                case Node.GetReq(_, Some(awa), _, _) if awa == t => "solid"
                case _ => "dotted"
            r ++= s"""$f -> $t [style = $style];"""
          }
      end for

    r.toString
  end draw
end GraphDrawer
