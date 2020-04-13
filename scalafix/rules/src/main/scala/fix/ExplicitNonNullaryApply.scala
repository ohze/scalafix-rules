package fix

import scalafix.util.Trivia
import scala.meta.tokens.Token.Underscore
import scala.PartialFunction.{cond, condOpt}
import scala.collection.mutable
import scala.meta._
import scalafix.v1._

// https://github.com/scala/scala-rewrites/blob/b2df038/rewrites/src/main/scala/fix/scala213/ExplicitNonNullaryApply.scala
class ExplicitNonNullaryApply extends SemanticRule("ExplicitNonNullaryApply") {
  val specialNames = Set(
    "asInstanceOf", "isInstanceOf",
    "getClass", "hashCode", "toString",
  )

  override def fix(implicit doc: SemanticDocument) = {
    val handled = mutable.Set.empty[Name]

    def fix(tree: Tree, meth: Term, noTypeArgs: Boolean, noArgs: Boolean) = {
      for {
        name <- termName(meth)
        if handled.add(name)
        if noArgs
        if name.isReference
        if !name.parent.exists(_.is[Term.ApplyInfix])
        info <- name.symbol.info
        if !info.isJava
        if !specialNames.contains(name.value)
        // `meth _` must not be patched as `meth() _`
        if doc
          .tokenList
          .trailing(tree.tokens.last)
          .find(!_.is[Trivia])
          .forall(!_.is[Underscore])
        if cond(info.signature) { case MethodSignature(_, List(Nil), _) => true }
      } yield Patch.addRight(if (noTypeArgs) name else tree, "()")
    }.asPatch

    doc.tree.collect {
      case t @ q"$meth[..$targs](...$args)" => fix(t, meth, targs.isEmpty, args.isEmpty)
      case t @ q"$meth(...$args)"           => fix(t, meth, true,          args.isEmpty)
    }.asPatch
  }

  private def termName(term: Term): Option[Name] = condOpt(term) {
    case name: Term.Name                 => name
    case Term.Select(_, name: Term.Name) => name
    case Type.Select(_, name: Type.Name) => name
  }
}
