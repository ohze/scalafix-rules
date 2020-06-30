package fix

import metaconfig.Configured
import scalafix.internal.v1.LazyValue
import scalafix.v1._

import scala.meta._
import scala.meta.internal.pc.ScalafixGlobal

import fix.impl.Power
import fix.impl.CompilerDependentRule

class NullaryOverrideImpl(global: LazyValue[ScalafixGlobal]) extends CompilerDependentRule(global, "NullaryOverride") {
  def this() = this(LazyValue.later(() => ScalafixGlobal.newCompiler(Nil, Nil, Map.empty)))

  override def withConfiguration(config: Configuration) =
    Configured.ok(new NullaryOverrideImpl(LazyValue.later { () =>
      ScalafixGlobal.newCompiler(config.scalacClasspath, config.scalacOptions, Map.empty)
    }))

  protected def unsafeFix()(implicit doc: SemanticDocument): Patch = {
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
}
