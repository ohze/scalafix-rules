package fix

import metaconfig.Configured
import scalafix.internal.rule.{CompilerException, TypePrinter}
import scalafix.internal.v1.LazyValue
import scalafix.patch.Patch
import scalafix.util.TokenOps
import scalafix.v1._

import scala.meta._
import scala.meta.contrib._
import scala.meta.internal.pc.ScalafixGlobal
import scala.util.control.Exception.nonFatalCatch

import fix.impl.CompilerTypePrinter
import fix.impl.PatchEmptyBody

final class ExplicitImplicitTypesImpl(global: LazyValue[ScalafixGlobal]) extends SemanticRule("ExplicitImplicitTypes") {

  def this() = this(LazyValue.later(() => ScalafixGlobal.newCompiler(Nil, Nil, Map.empty)))

  override def withConfiguration(config: Configuration) =
    Configured.ok(new ExplicitImplicitTypesImpl(LazyValue.later { () =>
      ScalafixGlobal.newCompiler(config.scalacClasspath, config.scalacOptions, Map.empty)
    }))

  override def isRewrite: Boolean = true

  override def afterComplete() = shutdownCompiler()
  def shutdownCompiler() = for (g <- global) nonFatalCatch { g.askShutdown(); g.close() }

  override def fix(implicit doc: SemanticDocument): Patch = {
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

  def unsafeFix()(implicit doc: SemanticDocument): Patch = {
    lazy val types = new CompilerTypePrinter(global.value)
    doc.tree.collect {
      case t @ Defn.Val(mods, Pat.Var(name) :: Nil, None, body)
        if mods.exists(_.isInstanceOf[Mod.Implicit]) => // t.hasMod(mod"implicit")
        fixDefinition(t, body, name, types)

      case t @ Defn.Var(mods, Pat.Var(name) :: Nil, None, Some(body))
        if mods.exists(_.isInstanceOf[Mod.Implicit]) =>
        fixDefinition(t, body, name, types)

      case t @ Defn.Def(mods, name, _, _, None, body)
        if mods.exists(_.isInstanceOf[Mod.Implicit]) =>
        fixDefinition(t, body, name, types)
    }.asPatch
  }

  def fixDefinition(defn: Defn, body: Term, name: Term.Name, types: TypePrinter)(
      implicit ctx: SemanticDocument
  ): Patch = {
    val lst = ctx.tokenList
    for {
      start <- defn.tokens.headOption
      end <- body.tokens.headOption
      // Left-hand side tokens in definition.
      // Example: `val x = ` from `val x = rhs.banana`
      lhsTokens = lst.slice(start, end)
      replace <- lhsTokens.reverseIterator.find(x =>
        !x.is[Token.Equals] && !x.is[Trivia]
      )
      space = {
        if (TokenOps.needsLeadingSpaceBeforeColon(replace)) " "
        else ""
      }
      defnSymbol <- name.symbol.asNonEmpty
      typePatch <- types.toPatch(name.pos, defnSymbol, replace, defn, space)
    } yield typePatch + PatchEmptyBody(body)
  }.asPatch.atomic
}

