package fix

import scalafix.util.Trivia
import scala.meta.tokens.Token.Underscore
import scala.PartialFunction.{cond, condOpt}
import scala.collection.mutable
import scala.meta._
import scalafix.v1._

// https://github.com/scala/scala-rewrites/blob/b2df038/rewrites/src/main/scala/fix/scala213/ExplicitNonNullaryApply.scala
class ExplicitNonNullaryApply extends SemanticRule("ExplicitNonNullaryApply") {
  private val specialNames = Set(
    // methods from AnyRef (Object)
    "getClass", "hashCode", "toString", "##", "clone"
  )
  private val ignore = SymbolMatcher.exact(
    "scala/Any#asInstanceOf().",
    "scala/Any#isInstanceOf().",
    // nullary in scala 2.13 but non-nullary in 2.12
    "scala/concurrent/duration/Duration#isFinite().",
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
        if !ignore.matches(name)
        // `meth _` must not be patched as `meth() _`
        if doc
          .tokenList
          .trailing(tree.tokens.last)
          .find(!_.is[Trivia])
          .forall(!_.is[Underscore])
        if cond(info.signature) {
          case MethodSignature(_, List(Nil, _*), _) => true
          case ClassSignature(_, _, _, decls) if tree.isInstanceOf[Term.ApplyType] =>
            decls.exists { info =>
              info.displayName == "apply" &&
              cond(info.signature) {
                case MethodSignature(_, List(Nil, _*), _) => true
              }
            }
        }
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
