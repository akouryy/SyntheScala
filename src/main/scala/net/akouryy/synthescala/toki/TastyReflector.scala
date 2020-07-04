package net.akouryy.synthescala
package toki

import toki.{Expr => EX, Type => TY}
import scala.language.implicitConversions
import scala.quoted.{autolift, _}

object TastyReflector:
  inline def reflect(inline expr: Any): List[Fun] = ${ reflectImpl('expr) }

  def reflectImpl(expr: Expr[Any])(using QuoteContext): Expr[List[Fun]] =
    import qctx.tasty._
    // PP.pprintln(expr.unseal)

    def convTY(typ: TypeTree)(using QuoteContext): Expr[TY] =
      typ match
        case Applied(TypeIdent(tid @ ("U" | "S")), List(Singleton(Literal(Constant(n: Int))))) =>
          if tid == "U"
            '{ TY.U($n) }
          else
            '{ TY.S($n) }
        case _ =>
          error(s"invalid type ${typ.show} ($typ)", typ.pos)
          '{ TY.S(0) }

    def convEX(expr: Term)(using QuoteContext): Expr[EX] =
      expr match
        case Literal(Constant(n: Int)) => '{ EX.Num(${n.toLong}) }
        case Literal(Constant(n: Long)) => '{ EX.Num(${n.toLong}) }
        case Ident(lab) =>
          '{ EX.Ref(Label($lab)) }
        case Apply(Select(left, op), List(right)) =>
          '{ EX.Bin($op, ${convEX(left)}, ${convEX(right)}) }
        case Apply(Ident(fn), args) =>
          '{ EX.Call($fn, ${listToExpr(args.map(convEX))}) }
        case Block(Nil, kont) =>
          convEX(kont)
        case Block(df :: restDefs, kont) =>
          val kontEX = convEX(Block(restDefs, kont))
          df match
            case ValDef(label, typ, Some(bound)) =>
              '{ EX.Let(Entry(Label($label), ${convTY(typ)}), ${convEX(bound)}, $kontEX) }
            case _ =>
              error(s"invalid defiintion\n$df", df.pos)
              '{ ??? }
        case If(cond, tru, fls) =>
          '{ EX.If(${convEX(cond)}, ${convEX(tru)}, ${convEX(fls)}) }
        case _ =>
          error(s"invalid expr\n$expr", expr.pos)
          '{ ??? }

    expr.unseal match
      case Inlined(None, Nil, Block(defs, Literal(Constant(())))) =>
        val exprs = defs.map:
          case defDef @ DefDef(fnName, Nil, params, retTyp, Some(body)) =>
            val paramEntries = params match
              case List(params) => params.map:
                case paramDef @ ValDef(param, paramTyp, thicket) =>
                  if thicket.isDefined
                    error(s"unknown thicket\n$thicket", paramDef.pos)
                    '{ ??? }
                  else
                    '{ Entry(Label($param), ${convTY(paramTyp)}) }
              case _ =>
                error(s"multiple param lists\n$params", defDef.pos)
                Nil

            '{ Fun($fnName, ${convTY(retTyp)}, ${listToExpr(paramEntries)}, ${convEX(body)}) }

          case df =>
            error(s"invalid defiintion\n$df", df.pos)
            return '{ Nil }

        listToExpr(exprs)
      case prog =>
        error(s"invalid program\n$prog", prog.pos)
        '{ Nil }
    end match
  end reflectImpl

  private def listToExpr[T : Type](list: List[Expr[T]])(using QuoteContext): Expr[List[T]] =
    list.foldRight[Expr[List[T]]]('{ Nil })((a, b) => '{ $a :: $b })

end TastyReflector
