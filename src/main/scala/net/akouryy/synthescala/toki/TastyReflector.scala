package net.akouryy.synthescala
package toki

import toki.{Expr => EX, Type => TY}
import scala.collection.mutable
import scala.language.implicitConversions
import scala.quoted.{autolift, _}

object TastyReflector:
  inline def reflect(inline expr: Any): Program = ${ reflectImpl('expr) }

  def reflectImpl(expr: Expr[Any])(using QuoteContext): Expr[Program] =
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
        // case Literal(Constant(n: Int)) => '{ EX.Num(${n.toLong}) }
        // case Literal(Constant(n: Long)) => '{ EX.Num(${n.toLong}) }
        case Apply(
          TypeApply(
            Select(Ident("ConversionFromLong"), "U" | "S"),
            List(Singleton(Literal(Constant(width)))),
          ),
          List(Literal(Constant(num: Long))),
        ) =>
          '{ EX.Num($num) }
        case Ident(lab) =>
          '{ EX.Ref(Label($lab)) }
        case Apply(select @ Select(left, op), List(right)) =>
          op match
            case BinOp.StringificationOf(_) =>
              '{ EX.Bin(BinOp.from($op).get, ${convEX(left)}, ${convEX(right)}) }
            case _ =>
              error(s"invalid operator\n${expr.showExtractors}", select.pos)
              '{ ??? }
        case Apply(TypeApply(select @ Select(left, op), List(_)), List(right)) =>
          BinOp.from(op) match
            case Some(_) =>
              '{ EX.Bin(BinOp.from($op).get, ${convEX(left)}, ${convEX(right)}) }
            case None =>
              error(s"invalid operator\n${expr.showExtractors}", select.pos)
              '{ ??? }
        case Apply(
          Apply(
            TypeApply(Select(Ident("ArrayAccess"), "apply"), List(Inferred(), Inferred())),
            List(Ident(arr)),
          ),
          List(index),
        ) =>
          '{ EX.Get(Label($arr), ${convEX(index)}) }
        case TypeApply(
          Select(base, "cast"),
          List(Singleton(Literal(Constant(width))))
        ) =>
          println(width)
          convEX(base) // TODO
        case Apply(Ident(fn), args) =>
          '{ EX.Call($fn, ${listToExpr(args.map(convEX))}) }
        case Block(Nil, kont) =>
          convEX(kont)
        case Block(st :: restStatements, kont) =>
          val kontEX = convEX(Block(restStatements, kont))
          st match
            case ValDef(label, typ, Some(bound)) =>
              '{ EX.Let(Entry(Label($label), ${convTY(typ)}), ${convEX(bound)}, $kontEX) }
            case Apply(
              Apply(
                TypeApply(Select(Ident("ArrayAccess"), "update"), List(Inferred(), Inferred())),
                List(Ident(arr)),
              ),
              List(index, value),
            ) =>
              '{ EX.Put(Label($arr), ${convEX(index)}, ${convEX(value)}, $kontEX) }
            case _ =>
              error(s"invalid statement\n$st", st.pos)
              '{ ??? }
        case If(cond, tru, fls) =>
          '{ EX.If(${convEX(cond)}, ${convEX(tru)}, ${convEX(fls)}) }
        case _ =>
          error(s"invalid expr\n$expr", expr.pos)
          '{ ??? }

    expr.unseal match
      case prog @ Inlined(None, Nil, Block(defs, Literal(Constant(())))) =>
        var main = Option.empty[Expr[Fun]]
        val arrayDefs = mutable.ListBuffer.empty[Expr[(Label, ArrayDef)]]

        defs.foreach:
          case defDef @ DefDef(fnName, Nil, params, retTyp, Some(body)) =>
            main match
              case Some(_) =>
                error(s"multiple functions", defDef.pos)
              case None =>
                val paramEntries = params match
                  case List(params) => params.map:
                    case paramDef @ ValDef(param, paramTyp, thicket) =>
                      thicket match
                        case Some(thicket) =>
                          error(s"unknown thicket\n${thicket.showExtractors}", paramDef.pos)
                          '{ ??? }
                        case None =>
                          '{ Entry(Label($param), ${convTY(paramTyp)}) }
                  case _ =>
                    error(s"multiple param lists\n$params", defDef.pos)
                    Nil
                main = Some(
                  '{ Fun($fnName, ${convTY(retTyp)}, ${listToExpr(paramEntries)}, ${convEX(body)}) }
                )

          case ValDef(arrName, Inferred(),
            Some(Apply(
              TypeApply(
                Select(New(Applied(TypeIdent("Array"), List(elemType: TypeTree))), "<init>"),
                List(Inferred()),
              ),
              List(Literal(Constant(len: Int))),
            ))
          ) =>
            arrayDefs += '{
              Label($arrName) -> ArrayDef(Label($arrName), ${convTY(elemType)}, $len)
            }

          case df =>
            error(s"invalid defiintion\n${df.showExtractors}", df.pos)

        main match
          case Some(main) => '{Program(${listToExpr(arrayDefs.toList)}.toMap, $main)}
          case None =>
            error(s"no function definition\n${prog.showExtractors}", prog.pos)
            '{ ??? }
      case prog =>
        error(s"invalid program\n${prog.showExtractors}", prog.pos)
        '{ ??? }
    end match
  end reflectImpl

  private def listToExpr[T : Type](list: List[Expr[T]])(using QuoteContext): Expr[List[T]] =
    list.foldRight[Expr[List[T]]]('{ Nil })((a, b) => '{ $a :: $b })

end TastyReflector
