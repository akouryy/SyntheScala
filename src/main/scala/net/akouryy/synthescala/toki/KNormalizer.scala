// Forked from https://github.com/cpu2019-5/anscaml/blob/master/src/main/scala/net/akouryy/anscaml/knorm/Converter.scala (MIT License)

package net.akouryy.synthescala
package toki

object KNormalizer:
  private[this] def convert(name: Option[String], expr: Expr)(kont: String => Expr): Expr =
    import Expr._
    def insert(expr: Expr): Expr =
      expr match
        case Ref(x) => kont(x)
        case _ =>
          val x = name.getOrElse(ID.temp())
          val kontExpr = kont(x)
          Let(Entry(x, Type.U[0]), expr, kontExpr)

    expr match
      case Num(_) | Ref(_) => insert(expr)
      case Let(Entry(x, t), expr, body) =>
        convert(Some(x), expr) { _ =>
          convert(name, body)(kont)
        }
      case Bin(op, left, right) =>
        convert(None, left) { x =>
          convert(None, right) { y =>
            insert(Bin(op, Ref(x), Ref(y)))
          }
        }
      case Call(name, args) =>
        args.foldLeft { (xs: List[Expr]) => insert(Call(name, xs)) } { (gen, arg) =>
          xs => convert(None, arg)(x => gen(Ref(x) :: xs))
        } (Nil)
      case If(cond, tru, fls) =>
        convert(None, cond) { x =>
          insert(If(Ref(x),
            convert(None, tru)(Expr.Ref(_)),
            convert(None, fls)(Expr.Ref(_)),
          ))
        }
  end convert

  def apply(expr: Expr) = convert(None, expr)(Expr.Ref(_))
end KNormalizer
