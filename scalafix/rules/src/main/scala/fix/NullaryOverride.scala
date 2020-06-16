package fix

import metaconfig.Configured
import scalafix.internal.rule.CompilerException
import scalafix.internal.v1.LazyValue
import scalafix.v1._

import scala.meta._
import scala.meta.internal.pc.ScalafixGlobal
import scala.util.control.Exception.nonFatalCatch

class NullaryOverride(global: LazyValue[ScalafixGlobal]) extends SemanticRule("NullaryOverride") {
  def this() = this(LazyValue.later(() => ScalafixGlobal.newCompiler(Nil, Nil, Map.empty)))
  override def withConfiguration(config: Configuration) =
    Configured.ok(new NullaryOverride(LazyValue.later { () =>
      ScalafixGlobal.newCompiler(config.scalacClasspath, config.scalacOptions, Map.empty)
    }))

  override def fix(implicit doc: SemanticDocument) = {
    try unsafeFix() catch {
      case e: CompilerException =>
        val input = doc.input match {
          case f: scala.meta.inputs.Input.File => f.path
          case i => i.getClass
        }

        println(s"Retrying fix because $e when fix $input")
        shutdownCompiler()
        global.restart()
        try unsafeFix() catch {
          case _: CompilerException =>
            Patch.empty /* ignore compiler crashes */
        }
    }
  }

  private def unsafeFix()(implicit doc: SemanticDocument) = {
    lazy val power = new Power(global.value)
    doc.tree.collect {
      case Defn.Def(_, name, _, Nil, _, _) if power.nullaryMethod(name).contains(false) =>
        Patch.addRight(name.tokens.last, "()")
      case t @ Defn.Def(_, name, _, List(Nil), _, _) if power.nullaryMethod(name).contains(true) =>
        val nameTok = name.tokens.last
        val parens = t.tokens.dropWhile(_ != nameTok).slice(1, 3) // '(' and ')'
        Patch.removeTokens(parens)
    }.asPatch
  }

  override def afterComplete() = shutdownCompiler()
  def shutdownCompiler() = for (g <- global) nonFatalCatch { g.askShutdown(); g.close() }
}

import scala.meta.internal.proxy.GlobalProxy
import scala.PartialFunction.cond

private class Power(val g: ScalafixGlobal)(implicit doc: SemanticDocument) {
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
