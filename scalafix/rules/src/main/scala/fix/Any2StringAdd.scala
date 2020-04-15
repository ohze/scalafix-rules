package fix

import scalafix.v1._
import scala.meta._

object Any2StringAdd {
  private val stringadd = SymbolMatcher.exact(
    "scala/Predef.any2stringadd#`+`().",
    "scala/Byte#`+`().",
    "scala/Short#`+`().",
    "scala/Char#`+`().",
    "scala/Int#`+`().",
    "scala/Long#`+`().",
    "scala/Float#`+`().",
    "scala/Double#`+`().",
  )

  private def isInParens(term: Term) = {
    val s = term.toString()
    s.startsWith("(") && s.endsWith(")")
  }

  private def blankStringPlus(term: Term) = term match {
    case _: Term.Name | _: Term.Select | _: Term.Block |
         _: Lit | _: Term.New | _: Term.Ascribe =>
      Patch.addLeft(term, """"" + """)
    case _ if isInParens(term) =>
      Patch.addLeft(term, """"" + """)
    case _ =>
      Patch.addLeft(term, """"" + (""") + Patch.addRight(term, ")")
  }
}

// fix https://github.com/scala/scala-rewrites/issues/18
// plus some improvements
final class Any2StringAdd extends SemanticRule("Any2StringAdd") {
  import Any2StringAdd._

  override def fix(implicit doc: SemanticDocument): Patch = {
    doc.tree.collect {
      case stringadd(Term.ApplyInfix(lhs, _, _, _)) => blankStringPlus(lhs)
    }.asPatch
  }
}
