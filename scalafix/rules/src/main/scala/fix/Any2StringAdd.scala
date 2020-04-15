package fix

import scalafix.v1._
import scala.meta._

object Any2StringAdd {
  val any2stringaddPlusString = SymbolMatcher.exact("scala/Predef.any2stringadd#`+`().")
  val primitivePlusString = SymbolMatcher.exact(
    "scala/Byte#`+`().",
    "scala/Short#`+`().",
    "scala/Char#`+`().",
    "scala/Int#`+`().",
    "scala/Long#`+`().",
    "scala/Float#`+`().",
    "scala/Double#`+`().",
  )
}

// fix https://github.com/scala/scala-rewrites/issues/18
// plus some improvements
final class Any2StringAdd extends SemanticRule("Any2StringAdd") {
  import Any2StringAdd._

  override def fix(implicit doc: SemanticDocument): Patch = {
    doc.tree.collect {
      case any2stringaddPlusString(Term.ApplyInfix(lhs, _, _, _)) => stringValueOf(lhs)
      case primitivePlusString(Term.ApplyInfix(lhs, _, _, _)) => blankStringPlus(lhs)
    }.asPatch
  }

  private def isInParentheses(term: Term) = {
    val s = term.toString()
    s.startsWith("(") && s.endsWith(")")
  }

  private def stringValueOf(term: Term) =
    if (isInParentheses(term)) {
      Patch.addLeft(term, "String.valueOf")
    } else {
      Patch.addLeft(term, "String.valueOf(") + Patch.addRight(term, ")")
    }

  private def blankStringPlus(term: Term) = term match {
    case _: Term.Name | _: Term.Select | _: Term.Block | _: Lit =>
      Patch.addLeft(term, """"" + """)
    case _ if isInParentheses(term) =>
      Patch.addLeft(term, """"" + """)
    case _ =>
      Patch.addLeft(term, """"" + (""") + Patch.addRight(term, ")")
  }
}
