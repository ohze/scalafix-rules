package fix

import scala.PartialFunction.{cond, condOpt}
import scala.collection.mutable
import scala.meta._
import scalafix.v1._

/** Similar to fix.scala213.ExplicitNonNullaryApply from scala-rewrites project
 * but don't need scala.meta.internal.pc.ScalafixGlobal to hook into scala-compiler
 * So this rule can be run with scala 2.13.x
 * Don't like fix.scala213.ExplicitNonNullaryApply, this rule also add `()` methods
 * that is defined in java and be overridden in scala.
 * @see ExplicitNonNullaryApplyJavaPending test
 * @see [[https://github.com/scala/scala-rewrites/blob/1cea92d/rewrites/src/main/scala/fix/scala213/ExplicitNonNullaryApply.scala fix.scala213.ExplicitNonNullaryApply]]
 */
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
        if !specialNames.contains(name.value) && !ignore.matches(name)
        if noArgs
        if name.isReference
        if !cond(name.parent) {
          case Some(Term.ApplyInfix(_, `name`, _, _)) => true
        }
        if !tree.parent.exists(_.is[Term.Eta])
        info <- Workaround1104.symbol(name).info
        if !info.isJava
        if cond(info.signature) {
          case MethodSignature(_, List(Nil, _*), _) => true
          case ClassSignature(_, _, _, decls) if tree.isInstanceOf[Term.ApplyType] =>
            decls.exists { decl =>
              decl.displayName == "apply" &&
              cond(decl.signature) {
                case MethodSignature(_, List(Nil, _*), _) => true
              }
            }
        }
      } yield {
        val tok =
          if (noTypeArgs) Workaround1104.lastToken(name)
          else tree.tokens.last
        val right = Patch.addRight(tok, "()")
        name.parent match {
          // scalameta:trees don't have PostfixSelect like
          // scala.tools.nsc.ast.Trees.PostfixSelect
          // so we have to check if Token.Dot is existed
          case Some(Term.Select(qual, `name`)) =>
            val qualLast = qual.tokens.last
            val nameHead = name.tokens.head
            val tokens = doc.tokenList
            val sliced = tokens.slice(tokens.next(qualLast), nameHead)
            if (sliced.exists(_.is[Token.Dot])) {
              right
            } else {
              Patch.removeTokens(tokens.trailingSpaces(qualLast)) +
                Patch.addLeft(nameHead, ".") +
                right
            }
          case _ => right
        }
      }
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
