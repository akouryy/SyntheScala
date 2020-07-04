// Forked from https://github.com/cpu2019-5/anscaml/blob/master/src/main/scala/net/akouryy/anscaml/knorm/Converter.scala (MIT License)

package net.akouryy.synthescala
package toki

import scala.collection.mutable

class KNormalizer(prog: Program):
  private val types = mutable.Map.empty[Label, Type]
  private val retTypes = mutable.Map.empty[String, Type]

  private def convert(lab: Option[Label], expr: Expr)(kont: Label => (Type, Expr)): (Type, Expr) =
    import Expr._
    def insert(typ: Type)(expr: Expr): (Type, Expr) =
      expr match
        case Ref(x) => kont(x)
        case _ =>
          val x = lab.getOrElse(Label.temp())
          val typ2 = types.getOrElseUpdate(x, typ)
          val kt -> kx = kont(x)
          kt -> Let(Entry(x, typ2), expr, kx)

    expr match
      case Num(n) =>
        def width(m: Long) = m.toBinaryString.length
        insert(
          if n >= 0 then Type.U(width(n)) else Type.S(1 + width(1 - n))
        )(expr)
      case Ref(v) => insert(types(v))(expr)
      case Let(Entry(x, t), expr, body) =>
        types(x) = t
        convert(Some(x), expr):
          _ =>
            convert(lab, body)(kont)
      case Bin(op, left, right) =>
        convert(None, left):
          x =>
            convert(None, right):
              y =>
                insert(op.calcTyp(types(x), types(y)))(Bin(op, Ref(x), Ref(y)))
      case Call(name, args) =>
        args.foldLeft {
          (xs: List[Expr]) => insert(retTypes(name))(Call(name, xs))
        } { (gen, arg) =>
          xs => convert(None, arg)(x => gen(Ref(x) :: xs))
        } (Nil)
      case Get(arr, index) =>
        convert(None, index):
          index =>
            insert(prog.arrayDefs(arr).elemTyp)(Get(arr, Ref(index)))
      case Put(arr, index, value, body) =>
        convert(None, index):
          index =>
            convert(None, value):
              value =>
                val resTyp -> resExpr = convert(lab, body)(kont)
                resTyp -> Put(arr, Ref(index), Ref(value), resExpr)
      case If(cond, tru, fls) =>
        convert(None, cond):
         x =>
          val tt -> tx = convert(None, tru)(l => types(l) -> Expr.Ref(l))
          val ft -> fx = convert(None, fls)(l => types(l) -> Expr.Ref(l))
          assert(tt == ft)
          insert(tt)(If(Ref(x), tx, fx))
  end convert

  def normalize: (TypeEnv, Program) =
    types.clear()
    retTypes.clear()
    prog.main.params.foreach(e => types(e.name) = e.typ)
    retTypes(prog.main.name) = prog.main.ret

    val (_, e) = convert(None, prog.main.body)(l => types(l) -> Expr.Ref(l))
    types.toMap -> prog.copy(main = prog.main.copy(body = e))
end KNormalizer
