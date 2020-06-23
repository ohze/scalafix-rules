package fix

import scalafix.v1.{Patch, SyntacticDocument, SyntacticRule}

import scala.meta._
import scala.meta.tokens.Token.LeftParen

class ParensAroundLambda extends SyntacticRule("ParensAroundLambda") {
  override def fix(implicit doc: SyntacticDocument): Patch = {
    doc.tree.collect {
      case Term.Function(List(p @ Term.Param(_, _, Some(_), _)), _) =>
        val tokens = p.tokens
        if (tokens.head.is[LeftParen]) Patch.empty
        else {
          Patch.addLeft(tokens.head, "(") +
            Patch.addRight(tokens.last, ")")
        }
    }.asPatch
  }
}
