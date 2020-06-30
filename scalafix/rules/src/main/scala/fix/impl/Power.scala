package fix.impl

import scalafix.internal.rule.CompilerException
import scalafix.v1._

import scala.PartialFunction.cond
import scala.meta._
import scala.meta.internal.pc.ScalafixGlobal
import scala.meta.internal.proxy.GlobalProxy

class Power(val g: ScalafixGlobal)(implicit doc: SemanticDocument) {
  private lazy val unit = g.newCompilationUnit(doc.input.text, doc.input.syntax)

  def nullaryMethod(t: Tree): Option[Boolean] =
    try {
      val meth = gsymbol(t)
      if (isJavaDefinedUnsafe(meth)) None
      else meth.nextOverriddenSymbol match {
        case m: g.MethodSymbol => Some(cond(m.info) {
          case g.NullaryMethodType(_) | g.PolyType(_, _: g.NullaryMethodType)=> true
        })
        case _ => None
      }
    } catch {
      case e: Throwable => throw CompilerException(e)
    }

  private def isJavaDefinedUnsafe(meth: g.Symbol) = {
    def test(sym: g.Symbol) = sym.isJavaDefined || sym.owner == g.definitions.AnyClass // sym.nextOverriddenSymbol
    test(meth) || meth.overrides.exists(test)
  }

  private def gsymbol(t: Tree): g.Symbol = {
    GlobalProxy.typedTreeAt(g, unit.position(t.pos.start))

    val sym = g
      .inverseSemanticdbSymbols(t.symbol.value)
      .find(t.symbol.value == g.semanticdbSymbol(_))
      .getOrElse(g.NoSymbol)

    if (sym.info.exists(g.definitions.NothingTpe == _))
      sym.overrides.lastOption.getOrElse(sym)
    else sym
  }
}
