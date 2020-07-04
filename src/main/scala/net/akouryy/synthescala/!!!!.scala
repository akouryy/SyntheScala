package net.akouryy

import scala.quoted._

package object synthescala:
  inline def !!!!(inline cause: Any): Nothing = ${ bang4Impl('cause) }

  def bang4Impl(expr: Expr[Any])(using QuoteContext): Expr[Nothing] =
    '{
      throw IllegalArgumentException(
        s"illegal: ${${ Expr(expr.show) }} = ${$expr}"
      )
    }
