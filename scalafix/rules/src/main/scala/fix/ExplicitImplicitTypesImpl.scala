package fix

import metaconfig.Configured
import scalafix.internal.rule.TypePrinter
import scalafix.internal.v1.LazyValue
import scalafix.patch.Patch
import scalafix.util.TokenOps
import scalafix.v1._

import scala.meta._
import scala.meta.contrib._
import scala.meta.internal.pc.ScalafixGlobal

import fix.impl.CompilerTypePrinter
import fix.impl.PatchEmptyBody
import fix.impl.CompilerDependentRule

final class ExplicitImplicitTypesImpl(global: LazyValue[ScalafixGlobal]) extends CompilerDependentRule(global, "ExplicitImplicitTypes") {
  def this() = this(LazyValue.later(() => ScalafixGlobal.newCompiler(Nil, Nil, Map.empty)))

  override def withConfiguration(config: Configuration) = {
    val symbolReplacements =
      config.conf.dynamic.ExplicitImplicitTypes.symbolReplacements
        .as[Map[String, String]]
        .getOrElse(Map.empty)

    Configured.ok(new ExplicitImplicitTypesImpl(LazyValue.later { () =>
      ScalafixGlobal.newCompiler(config.scalacClasspath, config.scalacOptions, symbolReplacements)
    }))
  }

  override def isRewrite: Boolean = true

  def unsafeFix()(implicit doc: SemanticDocument): Patch = {
    lazy val types = new CompilerTypePrinter(global.value)
    doc.tree.collect {
      case t @ Defn.Val(mods, Pat.Var(name) :: Nil, None, body)
        if isRuleCandidate(t, name, mods, body) =>
        fixDefinition(t, body, name, types)

      case t @ Defn.Var(mods, Pat.Var(name) :: Nil, None, Some(body))
        if isRuleCandidate(t, name, mods, body) =>
        fixDefinition(t, body, name, types)

      case t @ Defn.Def(mods, name, _, _, None, body)
        if isRuleCandidate(t, name, mods, body) =>
        fixDefinition(t, body, name, types)
    }.asPatch
  }

  // Don't explicitly annotate vals when the right-hand body is a single call
  // to `implicitly`. Prevents ambiguous implicit. Not annotating in such cases,
  // this a common trick employed implicit-heavy code to workaround SI-2712.
  // Context: https://gitter.im/typelevel/cats?at=584573151eb3d648695b4a50
  private def isImplicitly(term: Term): Boolean = term match {
    case Term.ApplyType(Term.Name("implicitly"), _) => true
    case _ => false
  }

  def isRuleCandidate(
     defn: Defn,
     nm: Name,
     mods: Iterable[Mod],
     body: Term
   )(implicit ctx: SemanticDocument): Boolean = {

    def isFinalLiteralVal: Boolean =
      defn.is[Defn.Val] &&
        mods.exists(_.is[Mod.Final]) &&
        body.is[Lit]

    def isImplicit: Boolean =
      mods.exists(_.is[Mod.Implicit]) && !isImplicitly(body)

    def hasParentWihTemplate: Boolean =
      defn.parent.exists(_.is[Template])

    isImplicit &&
      !isFinalLiteralVal &&
      nm.symbol.isLocal && // use ExplicitResultTypes for non-local implicits
      hasParentWihTemplate // use this Rule for local implicits in Template only
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
